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

package com.baidu.hugegraph.computer.core.network.message;

import java.nio.ByteBuffer;

import com.baidu.hugegraph.computer.core.network.TransportUtil;
import com.baidu.hugegraph.computer.core.network.buffer.ManagedBuffer;
import com.baidu.hugegraph.computer.core.network.buffer.NioManagedBuffer;

import io.netty.buffer.ByteBuf;

public class FailMessage extends AbstractMessage implements ResponseMessage {

    private final int failAckId;
    private final int errorCode;
    private final String message;

    public FailMessage(int failAckId, int errorCode, String message) {
        this.failAckId = failAckId;
        this.errorCode = errorCode;
        this.message = message;
    }

    @Override
    public MessageType type() {
        return MessageType.FAIL;
    }

    @Override
    public int ackId() {
        return this.failAckId;
    }

    @Override
    protected ManagedBuffer encodeBody(ByteBuf buf) {
        byte[] bytes = TransportUtil.encodeString(this.message);
        int bodyLength = Integer.BYTES + bytes.length;
        // Copy to direct memory
        ByteBuffer buffer = ByteBuffer.allocateDirect(bodyLength)
                                      .putInt(this.errorCode)
                                      .put(bytes);
        // Flip to make it readable
        buffer.flip();
        return new NioManagedBuffer(buffer);
    }

    public static FailMessage parseFrom(ByteBuf buf) {
        int failAckId = buf.readInt();
        int failCode = 0;
        String failMsg = null;
        // Skip partition
        buf.skipBytes(Integer.BYTES);

        // Decode body buffer
        int bodyLength = buf.readInt();
        if (bodyLength >= Integer.BYTES) {
            failCode = buf.readInt();
            failMsg = TransportUtil.readString(buf, bodyLength - Integer.BYTES);
        }

        skipExtraBuffer(buf);
        return new FailMessage(failAckId, failCode, failMsg);
    }

    public String message() {
        return this.message;
    }

    public int errorCode() {
        return this.errorCode;
    }
}
