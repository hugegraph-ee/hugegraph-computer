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

import java.net.InetSocketAddress;

import org.junit.Test;

import com.baidu.hugegraph.computer.core.util.StringEncoding;
import com.baidu.hugegraph.testutil.Assert;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.util.ReferenceCountUtil;

public class TransportUtilTest {

    @Test
    @SuppressWarnings("all")
    public void testRemoteAddressWithNull() {
        Channel channel = null;
        String address = TransportUtil.remoteAddress(channel);
        Assert.assertNull(address);
    }

    @Test
    @SuppressWarnings("all")
    public void testRemoteConnectionIDWithNull() {
        Channel channel = null;
        ConnectionId connectionId = TransportUtil.remoteConnectionId(channel);
        Assert.assertNull(connectionId);
    }

    @Test
    public void testResolvedSocketAddress() {
        InetSocketAddress address = TransportUtil.resolvedSocketAddress(
                                    "www.baidu.com", 80);
        Assert.assertFalse(address.isUnresolved());

        InetSocketAddress address2 = TransportUtil.resolvedSocketAddress(
                                     "www.baidu.com", 9797);
        Assert.assertFalse(address2.isUnresolved());

        InetSocketAddress address3 = TransportUtil.resolvedSocketAddress(
                                     "xxxxx", 80);
        Assert.assertTrue(address3.isUnresolved());

        InetSocketAddress address4 = TransportUtil.resolvedSocketAddress(
                                     "127.0.0.1", 80);
        Assert.assertFalse(address4.isUnresolved());
    }

    @Test
    public void testFormatAddress() {
        InetSocketAddress address = TransportUtil.resolvedSocketAddress(
                                    "xxxxx", 80);
        String formatAddress = TransportUtil.formatAddress(address);
        Assert.assertEquals("xxxxx:80", formatAddress);

        InetSocketAddress address2 = TransportUtil.resolvedSocketAddress(
                                     "127.0.0.1", 8089);
        String formatAddress2 = TransportUtil.formatAddress(address2);
        Assert.assertContains("127.0.0.1:8089", formatAddress2);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testReadString() {
        byte[] testData = StringEncoding.encode("test data");
        ByteBuf buffer = Unpooled.directBuffer(testData.length);
        buffer = ReferenceCountUtil.releaseLater(buffer);
        buffer.writeInt(testData.length);
        buffer.writeBytes(testData);
        String readString = TransportUtil.readString(buffer);
        Assert.assertEquals("test data", readString);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testWriteString() {
        ByteBuf buffer = Unpooled.buffer();
        buffer = ReferenceCountUtil.releaseLater(buffer);
        TransportUtil.writeString(buffer, "test data");
        String readString = TransportUtil.readString(buffer);
        Assert.assertEquals("test data", readString);
    }
}
