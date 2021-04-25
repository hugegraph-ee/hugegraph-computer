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

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import com.baidu.hugegraph.computer.core.config.ComputerOptions;
import com.baidu.hugegraph.computer.core.network.TransportClient;
import com.baidu.hugegraph.computer.core.network.session.ServerSession;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;

public class HeartBeatHandlerTest extends AbstractNetworkTest {

    private static final int HEARTBEAT_INTERVAL = 2;
    private static final int HEARTBEAT_TIME_OUT = 6;
    private static final int MAX_HEARTBEAT_TIMES = 3;

    @Override
    protected void initOption() {
        super.updateOption(ComputerOptions.TRANSPORT_HEARTBEAT_INTERVAL,
                           HEARTBEAT_INTERVAL);
        super.updateOption(ComputerOptions.TRANSPORT_HEARTBEAT_TIMEOUT,
                           HEARTBEAT_TIME_OUT);
        super.updateOption(ComputerOptions.TRANSPORT_MAX_HEARTBEAT_TIMES,
                           MAX_HEARTBEAT_TIMES);
    }

    @Test
    public void testHeartBeatHandler() throws Exception {
        HeartBeatHandler beatHandler = new HeartBeatHandler();
        HeartBeatHandler mockHeartBeatHandler = Mockito.spy(beatHandler);
        Mockito.doAnswer(invocationOnMock -> {
            invocationOnMock.callRealMethod();
            Channel channel = invocationOnMock.getArgument(0);
            channel.pipeline().replace("heartBeatHandler", "heartBeatHandler",
                                       mockHeartBeatHandler);
            return null;
        }).when(clientProtocol).initializeClientPipeline(Mockito.any());

        // Skip processPingMessage()
        ServerSession serverSession = new ServerSession(conf);
        NettyServerHandler handler2 = new NettyServerHandler(serverSession,
                                                             serverHandler);
        NettyServerHandler spyNettyServerHandler = Mockito.spy(handler2);
        Mockito.doAnswer(invocationOnMock -> {
            invocationOnMock.callRealMethod();
            Channel channel = invocationOnMock.getArgument(0);
            channel.pipeline().replace(NettyProtocol.SERVER_HANDLER_NAME,
                                       NettyProtocol.SERVER_HANDLER_NAME,
                                       spyNettyServerHandler);
            return null;
        }).when(serverProtocol).initializeServerPipeline(Mockito.any(),
                                                         Mockito.any());
        Mockito.doAnswer(invocationOnMock -> null)
               .when(spyNettyServerHandler).processPingMessage(Mockito.any(),
                                                               Mockito.any(),
                                                               Mockito.any());

        NettyTransportClient client = (NettyTransportClient) this.oneClient();
        NettyClientHandler handler = new NettyClientHandler(client);
        NettyClientHandler spyHandler = Mockito.spy(handler);
        client.channel().pipeline().replace(NettyProtocol.CLIENT_HANDLER_NAME,
                                            NettyProtocol.CLIENT_HANDLER_NAME,
                                            spyHandler);

        int heartbeatTimesClose = MAX_HEARTBEAT_TIMES + 1;
        long timout = ((HEARTBEAT_INTERVAL * heartbeatTimesClose)) * 1000L;
        Mockito.verify(mockHeartBeatHandler,
                       Mockito.timeout(timout).times(heartbeatTimesClose))
               .userEventTriggered(Mockito.any(),
                                   Mockito.any(IdleStateEvent.class));

        Assert.assertFalse(client.active());
    }

    @Test
    public void testServerIdleHandler() throws Exception {
        ServerIdleHandler serverIdleHandler = new ServerIdleHandler();
        ServerIdleHandler spyServerIdleHandler = Mockito.spy(serverIdleHandler);
        Mockito.doAnswer(invocationOnMock -> {
            invocationOnMock.callRealMethod();
            Channel channel = invocationOnMock.getArgument(0);
            channel.pipeline().replace("serverIdleHandler", "serverIdleHandler",
                                       spyServerIdleHandler);
            return null;
        }).when(serverProtocol).initializeServerPipeline(Mockito.any(),
                                                         Mockito.any());

        HeartBeatHandler beatHandler = new HeartBeatHandler();
        HeartBeatHandler mockHeartBeatHandler = Mockito.spy(beatHandler);
        Mockito.doAnswer(invocationOnMock -> {
            invocationOnMock.callRealMethod();
            Channel channel = invocationOnMock.getArgument(0);
            channel.pipeline().replace("heartBeatHandler", "heartBeatHandler",
                                       mockHeartBeatHandler);
            return null;
        }).when(clientProtocol).initializeClientPipeline(Mockito.any());

        Mockito.doAnswer(invocationOnMock -> {
            ChannelHandlerContext ctx = invocationOnMock.getArgument(0);
            Object event = invocationOnMock.getArgument(1);
            ctx.fireUserEventTriggered(event);
            return null;
        }).when(mockHeartBeatHandler)
          .userEventTriggered(Mockito.any(), Mockito.any(IdleStateEvent.class));

        TransportClient client = this.oneClient();

        long delay = (HEARTBEAT_TIME_OUT + 1) * 1000L;
        Mockito.verify(spyServerIdleHandler, Mockito.timeout(delay).times(1))
               .userEventTriggered(Mockito.any(),
                                   Mockito.any(IdleStateEvent.class));
    }
}
