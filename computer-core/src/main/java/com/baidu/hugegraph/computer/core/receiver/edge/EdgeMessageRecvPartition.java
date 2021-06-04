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

package com.baidu.hugegraph.computer.core.receiver.edge;

import com.baidu.hugegraph.computer.core.combiner.Combiner;
import com.baidu.hugegraph.computer.core.combiner.OverwriteCombiner;
import com.baidu.hugegraph.computer.core.combiner.PointerCombiner;
import com.baidu.hugegraph.computer.core.common.ComputerContext;
import com.baidu.hugegraph.computer.core.common.Constants;
import com.baidu.hugegraph.computer.core.config.ComputerOptions;
import com.baidu.hugegraph.computer.core.config.Config;
import com.baidu.hugegraph.computer.core.graph.GraphFactory;
import com.baidu.hugegraph.computer.core.graph.properties.Properties;
import com.baidu.hugegraph.computer.core.network.message.MessageType;
import com.baidu.hugegraph.computer.core.receiver.MessageRecvPartition;
import com.baidu.hugegraph.computer.core.sort.Sorter;
import com.baidu.hugegraph.computer.core.sort.flusher.CombineSubKvOuterSortFlusher;
import com.baidu.hugegraph.computer.core.sort.flusher.OuterSortFlusher;
import com.baidu.hugegraph.computer.core.store.FileGenerator;
import com.baidu.hugegraph.computer.core.store.hgkvfile.entry.Pointer;

public class EdgeMessageRecvPartition extends MessageRecvPartition {

    private static final String TYPE = MessageType.EDGE.name();

    private final OuterSortFlusher flusher;

    public EdgeMessageRecvPartition(ComputerContext context,
                                    FileGenerator fileGenerator,
                                    Sorter sorter) {
        super(context.config(), fileGenerator, sorter,
              true, Constants.INPUT_SUPERSTEP);
        Config config = context.config();
        int flushThreshold = config.get(
                             ComputerOptions.INPUT_MAX_EDGES_IN_ONE_VERTEX);
        Combiner<Properties> propertiesCombiner = config.createObject(
        ComputerOptions.WORKER_EDGE_PROPERTIES_COMBINER_CLASS);

        /*
         * If propertiesCombiner is OverwriteCombiner, just remain the
         * second, no need to deserialize the properties and then serialize
         * the second properties.
         */
        if (propertiesCombiner instanceof OverwriteCombiner) {
            this.flusher = new CombineSubKvOuterSortFlusher(
                           new OverwriteCombiner<>(), flushThreshold);
        } else {
            GraphFactory graphFactory = context.graphFactory();
            Properties v1 = graphFactory.createProperties();
            Properties v2 = graphFactory.createProperties();

            Combiner<Pointer> pointerCombiner = new PointerCombiner<>(
                                                v1, v2, propertiesCombiner);
            this.flusher = new CombineSubKvOuterSortFlusher(pointerCombiner,
                                                            flushThreshold);
        }
    }

    @Override
    protected OuterSortFlusher outerSortFlusher() {
        return this.flusher;
    }

    @Override
    protected String type() {
        return TYPE;
    }
}
