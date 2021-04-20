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

package com.baidu.hugegraph.computer.core.network.netty.codec;

import org.slf4j.Logger;

import com.baidu.hugegraph.computer.core.network.TransportUtil;
import com.baidu.hugegraph.computer.core.network.message.AbstractMessage;
import com.baidu.hugegraph.util.Log;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

/**
 * Frame decoder based on netty's {@link LengthFieldBasedFrameDecoder}
 */
public class FrameDecoder extends LengthFieldBasedFrameDecoder {

    private static final Logger LOG = Log.logger(FrameDecoder.class);

    private static final int LENGTH_ADJUSTMENT = 0;
    private static final int INITIAL_BYTES_TO_STRIP = 0;

    public FrameDecoder() {
        super(AbstractMessage.MAX_MESSAGE_LENGTH,
              AbstractMessage.OFFSET_BODY_LENGTH,
              AbstractMessage.LENGTH_BODY_LENGTH,
              LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in)
                            throws Exception {
        ByteBuf msg = (ByteBuf) super.decode(ctx, in);
        if (msg == null) {
            return null;
        }

        int magicNumber = msg.readShort();
        if (magicNumber != AbstractMessage.MAGIC_NUMBER) {
            LOG.warn("Network stream corrupted: received incorrect " +
                     "magic number: {}, remote address: {}",
                     magicNumber, TransportUtil.remoteAddress(ctx.channel()));
            msg.release();
            return null;
        }
        int version = msg.readByte();
        if (version != AbstractMessage.PROTOCOL_VERSION) {
            LOG.warn("Network stream corrupted: received incorrect " +
                     "protocol version: {}, remote address: {}",
                     version, TransportUtil.remoteAddress(ctx.channel()));
            msg.release();
            return null;
        }
        // TODO: improve it use shard memory
        return msg;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("The FrameDecoder active from {}",
                      TransportUtil.remoteAddress(ctx.channel()));
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (LOG.isDebugEnabled()) {
            LOG.debug("The FrameDecoder inActive from {}",
                      TransportUtil.remoteAddress(ctx.channel()));
        }
        super.channelInactive(ctx);
    }
}
