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
import java.util.function.Function;

import com.baidu.hugegraph.computer.core.common.exception.ComputeException;
import com.baidu.hugegraph.computer.core.network.TransportConf;
import com.baidu.hugegraph.computer.core.network.TransportStatus;
import com.baidu.hugegraph.computer.core.network.buffer.ManagedBuffer;
import com.baidu.hugegraph.computer.core.network.buffer.NioManagedBuffer;
import com.baidu.hugegraph.computer.core.network.message.DataMessage;
import com.baidu.hugegraph.computer.core.network.message.FinishMessage;
import com.baidu.hugegraph.computer.core.network.message.Message;
import com.baidu.hugegraph.computer.core.network.message.MessageType;
import com.baidu.hugegraph.computer.core.network.message.StartMessage;
import com.baidu.hugegraph.concurrent.BarrierEvent;
import com.baidu.hugegraph.util.E;

public class ClientSession extends TransportSession {

    private volatile boolean flowControlStatus;
    private final BarrierEvent startBarrierEvent;
    private final BarrierEvent finishBarrierEvent;
    private final Function<Message, ?> sendFunction;

    public ClientSession(TransportConf conf,
                         Function<Message, ?> sendFunction) {
        super(conf);
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

    private void startSent() {
        MAX_REQUEST_ID_UPDATER.compareAndSet(this, SEQ_INIT_VALUE,
                                             START_REQUEST_ID);
        this.status = TransportStatus.START_SEND;
    }

    private void finishSent(int finishId) {
        this.finishId = finishId;
        this.status = TransportStatus.FINISH_SEND;
    }

    @Override
    public void startComplete() {
        E.checkArgument(this.status == TransportStatus.START_SEND,
                        "The status must be START_SEND instead of %s " +
                        "on startComplete", this.status);
        this.establish();
        this.maxAckId = START_REQUEST_ID;
        this.startBarrierEvent.signalAll();
    }

    @Override
    public void finishComplete() {
        E.checkArgument(this.status == TransportStatus.FINISH_SEND,
                        "The status must be FINISH_SEND instead of %s " +
                        "on finishComplete", this.status);
        this.ready();
        this.finishBarrierEvent.signalAll();
    }

    public synchronized void syncStart() throws InterruptedException {
        E.checkArgument(this.status == TransportStatus.READY,
                        "The status must be READY instead of %s " +
                        "on syncStart", this.status);

        this.sendFunction.apply(StartMessage.INSTANCE);

        this.startSent();

        this.startBarrierEvent.await(this.conf.syncRequestTimeout());
    }

    public synchronized void syncFinish() throws InterruptedException {
        E.checkArgument(this.status == TransportStatus.ESTABLISH,
                        "The status must be ESTABLISH instead of %s " +
                        "on syncFinish", this.status);

        int finishId = this.maxRequestId + 1;
        FinishMessage finishMessage = new FinishMessage(finishId);

        this.sendFunction.apply(finishMessage);

        this.finishSent(finishId);

        this.finishBarrierEvent.await(this.conf.syncRequestTimeout());
    }

    public synchronized void asyncSend(MessageType messageType, int partition,
                                       ByteBuffer buffer) {
        E.checkArgument(this.status == TransportStatus.ESTABLISH,
                        "The status must be ESTABLISH instead of %s " +
                        "on asyncSend", this.status);
        int requestId = this.nextRequestId();

        ManagedBuffer managedBuffer = new NioManagedBuffer(buffer);
        DataMessage dataMessage = new DataMessage(messageType, requestId,
                                                  partition, managedBuffer);

        this.sendFunction.apply(dataMessage);

        this.changeFlowControlStatus();
    }

    public void receiveAck(int ackId) {
        // TODO: 进入 FINISH_SEND 后 收到 data 的 ackId
        if (ackId == START_REQUEST_ID &&
            this.status == TransportStatus.START_SEND) {
            this.startComplete();
        } else if (ackId == this.finishId &&
                   this.status == TransportStatus.FINISH_SEND) {
            this.finishComplete();
        } else if (this.status == TransportStatus.ESTABLISH ||
                   this.status == TransportStatus.FINISH_SEND) {
            if (this.maxAckId < ackId) {
                this.maxAckId = ackId;
            }
            this.changeFlowControlStatus();
        } else {
            throw new ComputeException("Receive an ack message, but the " +
                                       "status not match, status: %s, ackId: " +
                                       "%s", this.status, ackId);
        }
    }

    public boolean flowControllerStatus() {
        return this.flowControlStatus;
    }

    private synchronized void changeFlowControlStatus() {
        int maxPendingRequests = this.conf.maxPendingRequests();
        int minPendingRequests = this.conf.minPendingRequests();

        int pendingRequests = this.maxRequestId - this.maxAckId;

        if (pendingRequests > maxPendingRequests) {
            this.flowControlStatus = true;
        } else if (pendingRequests < minPendingRequests){
            this.flowControlStatus = false;
        }
    }
}
