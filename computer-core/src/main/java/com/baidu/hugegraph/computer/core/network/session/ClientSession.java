/*
 * Copyright 2017 HugeGraph Authors
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with this
 * work for additional information regarding copyright ownership. The ASF
 * licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.baidu.hugegraph.computer.core.network.session;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import com.baidu.hugegraph.computer.core.common.exception.ComputeException;
import com.baidu.hugegraph.computer.core.common.exception.TransportException;
import com.baidu.hugegraph.computer.core.network.TransportConf;
import com.baidu.hugegraph.computer.core.network.TransportState;
import com.baidu.hugegraph.computer.core.network.buffer.ManagedBuffer;
import com.baidu.hugegraph.computer.core.network.buffer.NioManagedBuffer;
import com.baidu.hugegraph.computer.core.network.message.AbstractMessage;
import com.baidu.hugegraph.computer.core.network.message.DataMessage;
import com.baidu.hugegraph.computer.core.network.message.FinishMessage;
import com.baidu.hugegraph.computer.core.network.message.Message;
import com.baidu.hugegraph.computer.core.network.message.MessageType;
import com.baidu.hugegraph.computer.core.network.message.StartMessage;
import com.baidu.hugegraph.concurrent.BarrierEvent;
import com.baidu.hugegraph.util.E;

public class ClientSession extends TransportSession {

    private final int maxPendingRequests;
    private final int minPendingRequests;
    private final long syncRequestTimeout;
    private final long finishSessionTimeout;

    private final Lock flowControlStatusLock;
    private volatile boolean flowControlStatus;
    private final BarrierEvent startBarrierEvent;
    private final BarrierEvent finishBarrierEvent;
    private final Function<Message, ?> sendFunction;

    public ClientSession(TransportConf conf,
                         Function<Message, ?> sendFunction) {
        super(conf);
        this.maxPendingRequests = this.conf.maxPendingRequests();
        this.minPendingRequests = this.conf.minPendingRequests();
        this.syncRequestTimeout = this.conf.syncRequestTimeout();
        this.finishSessionTimeout = this.conf.finishSessionTimeout();
        this.flowControlStatusLock = new ReentrantLock();
        this.flowControlStatus = false;
        this.startBarrierEvent = new BarrierEvent();
        this.finishBarrierEvent = new BarrierEvent();
        this.sendFunction = sendFunction;
    }

    @Override
    protected void ready() {
        this.flowControlStatus = false;
        super.ready();
    }

    private void startSend() {
        this.maxRequestId = AbstractMessage.START_SEQ;
        this.state = TransportState.START_SEND;
    }

    private void finishSend(int finishId) {
        this.finishId = finishId;
        this.state = TransportState.FINISH_SEND;
    }

    @Override
    public void startComplete() {
        E.checkArgument(this.state == TransportState.START_SEND,
                        "The state must be START_SEND instead of %s " +
                        "on startComplete", this.state);
        this.establish();
        this.maxAckId = AbstractMessage.START_SEQ;
        this.startBarrierEvent.signalAll();
    }

    @Override
    public void finishComplete() {
        E.checkArgument(this.state == TransportState.FINISH_SEND,
                        "The state must be FINISH_SEND instead of %s " +
                        "on finishComplete", this.state);
        this.ready();
        this.finishBarrierEvent.signalAll();
    }

    public synchronized void syncStart() throws TransportException,
                                                InterruptedException {
        E.checkArgument(this.state == TransportState.READY,
                        "The state must be READY instead of %s " +
                        "on syncStart", this.state);

        this.startSend();

        this.sendFunction.apply(StartMessage.INSTANCE);

        if (!this.startBarrierEvent.await(this.syncRequestTimeout)) {
            throw new TransportException("Timeout(%sms) to wait start " +
                                         "response", this.syncRequestTimeout);
        }
        this.startBarrierEvent.reset();
    }

    public synchronized void syncFinish() throws TransportException,
                                                 InterruptedException {
        E.checkArgument(this.state == TransportState.ESTABLISH,
                        "The state must be ESTABLISH instead of %s " +
                        "on syncFinish", this.state);

        int finishId = this.genFinishId();

        this.finishSend(finishId);

        FinishMessage finishMessage = new FinishMessage(finishId);
        this.sendFunction.apply(finishMessage);

        if (!this.finishBarrierEvent.await(this.finishSessionTimeout)) {
            throw new TransportException("Timeout(%sms) to wait finish " +
                                         "response", this.finishSessionTimeout);
        }
        this.finishBarrierEvent.reset();
    }

    public synchronized void asyncSend(MessageType messageType, int partition,
                                       ByteBuffer buffer) {
        E.checkArgument(this.state == TransportState.ESTABLISH,
                        "The state must be ESTABLISH instead of %s " +
                        "on asyncSend", this.state);
        int requestId = this.nextRequestId();

        ManagedBuffer managedBuffer = new NioManagedBuffer(buffer);
        DataMessage dataMessage = new DataMessage(messageType, requestId,
                                                  partition, managedBuffer);

        this.sendFunction.apply(dataMessage);

        this.updateFlowControlStatus();
    }

    public void ackRecv(int ackId) {
        if (ackId == AbstractMessage.START_SEQ &&
            this.state == TransportState.START_SEND) {
            this.startComplete();
        } else if (ackId == this.finishId &&
                   this.state == TransportState.FINISH_SEND) {
            this.finishComplete();
        } else if (this.state == TransportState.ESTABLISH ||
                   this.state == TransportState.FINISH_SEND) {
            if (this.maxAckId < ackId) {
                this.maxAckId = ackId;
            }
            this.updateFlowControlStatus();
        } else {
            throw new ComputeException("Receive an ack message, but the " +
                                       "state not match, state: %s, ackId: " +
                                       "%s", this.state, ackId);
        }
    }

    public boolean flowControllerStatus() {
        return this.flowControlStatus;
    }

    private void updateFlowControlStatus() {
        this.flowControlStatusLock.lock();
        try {
            int pendingRequests = this.maxRequestId - this.maxAckId;

            if (pendingRequests >= this.maxPendingRequests) {
                this.flowControlStatus = true;
            } else if (pendingRequests < this.minPendingRequests) {
                this.flowControlStatus = false;
            }
        } finally {
            this.flowControlStatusLock.unlock();
        }
    }
}
