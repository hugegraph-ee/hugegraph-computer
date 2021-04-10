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

package com.baidu.hugegraph.computer.core.network;

import org.slf4j.Logger;

import com.baidu.hugegraph.computer.core.common.exception.TransportException;
import com.baidu.hugegraph.computer.core.network.buffer.ManagedBuffer;
import com.baidu.hugegraph.computer.core.network.message.MessageType;
import com.baidu.hugegraph.util.Log;

public class MockMessageHandler implements MessageHandler {

    private static final Logger LOG = Log.logger(MockMessageHandler.class);

    @Override
    public void handle(MessageType messageType, int partition,
                       ManagedBuffer buffer) {
        LOG.info("messageType: {}, partition: {}, buffer readable size: {}",
                 messageType.name(), partition,
                 buffer != null ? buffer.size() : null);

        if (buffer != null) {
            // Must be release it
            buffer.release();
        }
    }

    @Override
    public void channelActive(ConnectionID connectionID) {
        LOG.info("Server channel active, connectionID: {}", connectionID);
    }

    @Override
    public void channelInactive(ConnectionID connectionID) {
        LOG.info("Server channel inActive, connectionID: {}", connectionID);
    }

    @Override
    public void exceptionCaught(TransportException cause,
                                ConnectionID connectionID) {
        LOG.error("Server channel exception, connectionID: {}, cause: ",
                  connectionID, cause);
    }
}
