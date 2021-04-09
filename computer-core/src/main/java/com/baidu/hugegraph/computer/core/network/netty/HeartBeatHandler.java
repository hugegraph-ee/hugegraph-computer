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

package com.baidu.hugegraph.computer.core.network.netty;

import org.slf4j.Logger;

import com.baidu.hugegraph.computer.core.network.TransportUtil;
import com.baidu.hugegraph.util.Log;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;

/**
 * Heart beat triggered.
 * send ping message
 */
@ChannelHandler.Sharable
public class HeartBeatHandler extends ChannelDuplexHandler {

    private static final Logger LOG = Log.logger(HeartBeatHandler.class);

    @Override
    public void userEventTriggered(final ChannelHandlerContext ctx,
                                   Object evt) throws Exception {
        LOG.debug("HeartBeatHandler userEventTriggered, evt class: {} from: {}",
                  evt.getClass().getName(),
                  TransportUtil.remoteAddress(ctx.channel()));
        // TODO: handle heartBeat
        super.userEventTriggered(ctx, evt);
    }
}
