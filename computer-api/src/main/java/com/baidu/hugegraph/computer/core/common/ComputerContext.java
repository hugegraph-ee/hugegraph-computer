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

package com.baidu.hugegraph.computer.core.common;

import com.baidu.hugegraph.computer.core.allocator.Allocator;
import com.baidu.hugegraph.computer.core.graph.GraphFactory;
import com.baidu.hugegraph.computer.core.graph.value.ValueFactory;
import com.baidu.hugegraph.util.E;
import com.baidu.hugegraph.computer.core.config.Config;
public final class ComputerContext {

    private static volatile ComputerContext INSTANCE;

    private final Config config;
    private final GraphFactory graphFactory;
    private final ValueFactory valueFactory;
    private final Allocator allocator;

    private ComputerContext(Config config,
                            GraphFactory graphFactory,
                            ValueFactory valueFactory,
                            Allocator allocator) {
        this.config = config;
        this.graphFactory = graphFactory;
        this.valueFactory = valueFactory;
        this.allocator = allocator;
    }

    public static synchronized void initContext(Config config,
                                                GraphFactory graphFactory,
                                                ValueFactory valueFactory,
                                                Allocator allocator) {
        INSTANCE = new ComputerContext(config, graphFactory,
                                       valueFactory, allocator);
    }

    public static ComputerContext instance() {
        E.checkNotNull(INSTANCE, "ComputerContext INSTANCE");
        return INSTANCE;
    }

    public Config config() {
        return this.config;
    }

    public GraphFactory graphFactory() {
        return this.graphFactory;
    }

    public ValueFactory valueFactory() {
        return this.valueFactory;
    }

    public Allocator allocator() {
        return this.allocator;
    }
}
