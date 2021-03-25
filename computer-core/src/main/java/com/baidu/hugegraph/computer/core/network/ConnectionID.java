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

import static com.baidu.hugegraph.computer.core.network.TransportUtil.resolvedAddress;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.baidu.hugegraph.util.E;

/**
 * A {@link ConnectionID} identifies a connection to a remote task manager by
 * the socket address and a client index. This allows multiple connections to
 * the same worker to be distinguished by their client index.
 */
public class ConnectionID {

    private final InetSocketAddress address;
    private final int clientIndex;
    private static final ConcurrentHashMap<String, ConnectionID>
            CONNECTION_ID_CACHE = new ConcurrentHashMap<>();

    public static ConnectionID parseConnectionID(String host, int port) {
        return parseConnectionID(host, port, 0);
    }

    public static ConnectionID parseConnectionID(String host, int port,
                                               int clientIndex) {
        String cacheKey = buildCacheKey(host, port, clientIndex);
        Function<String, ConnectionID> kvRemovalListener = key ->
                new ConnectionID(resolvedAddress(host, port), clientIndex);
        return CONNECTION_ID_CACHE.computeIfAbsent(cacheKey, kvRemovalListener);
    }

    public ConnectionID(InetSocketAddress address) {
        this(address, 0);
    }

    public ConnectionID(InetSocketAddress address, int clientIndex) {
        E.checkArgument(clientIndex >= 0,
                        "The clientIndex must be >= 0");
        // Use resolved address here
        E.checkArgument(!address.isUnresolved(),
                        "The address must be resolved");
        this.address = address;
        this.clientIndex = clientIndex;
    }

    private static String buildCacheKey(String host, int port,
                                        int clientIndex) {
        return host + ":" + port + "[" + clientIndex + "]";
    }

    public InetSocketAddress socketAddress() {
        return this.address;
    }

    public int clientIndex() {
        return this.clientIndex;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode() + (31 * this.clientIndex);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj.getClass() != ConnectionID.class) {
            return false;
        }

        final ConnectionID other = (ConnectionID) obj;
        return other.socketAddress().equals(this.address) &&
               other.clientIndex() == this.clientIndex;
    }

    @Override
    public String toString() {
        return this.address + " [" + this.clientIndex + "]";
    }
}