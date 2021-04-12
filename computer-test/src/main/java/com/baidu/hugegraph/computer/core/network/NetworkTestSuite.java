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

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.baidu.hugegraph.computer.core.network.buffer.ManagedBufferTest;
import com.baidu.hugegraph.computer.core.network.connection.ConnectionManagerTest;
import com.baidu.hugegraph.computer.core.network.netty.HeartBeatHandlerTest;
import com.baidu.hugegraph.computer.core.network.netty.NettyClientFactoryTest;
import com.baidu.hugegraph.computer.core.network.netty.NettyEncodeDecodeHandlerTest;
import com.baidu.hugegraph.computer.core.network.netty.NettyTransportClientTest;
import com.baidu.hugegraph.computer.core.network.netty.NettyTransportServerTest;
import com.baidu.hugegraph.computer.core.network.session.TransportSessionTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    NettyTransportServerTest.class,
    ConnectionIDTest.class,
    NettyClientFactoryTest.class,
    ConnectionManagerTest.class,
    TransportUtilTest.class,
    TransportSessionTest.class,
    NettyTransportClientTest.class,
    NettyEncodeDecodeHandlerTest.class,
    HeartBeatHandlerTest.class,
    ManagedBufferTest.class
})
public class NetworkTestSuite {
}
