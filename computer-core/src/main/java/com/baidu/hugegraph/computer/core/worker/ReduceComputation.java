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

package com.baidu.hugegraph.computer.core.worker;

import java.util.Arrays;
import java.util.Iterator;

import com.baidu.hugegraph.computer.core.combiner.Combiner;
import com.baidu.hugegraph.computer.core.graph.value.Value;
import com.baidu.hugegraph.computer.core.graph.vertex.Vertex;

/**
 * For algorithm with combiner that combine all the messages, and the
 * algorithm can only receive a message for a vertex. If no message received,
 * null will be passed. Compute message will return a result that send to
 * all adjacent vertices by default.
 */
public interface ReduceComputation<M extends Value> extends Computation<M> {

    /**
     * Set vertex's value and return initial message. The message will be
     * used to compute the vertex as parameter Iterator<M> messages.
     */
    M initialValue(WorkerContext context, Vertex vertex);

    @Override
    default void compute0(WorkerContext context, Vertex vertex) {
        M result = this.initialValue(context, vertex);
        this.compute(context, vertex, Arrays.asList(result).iterator());
    }

    /**
     * Called at all supersteps(except superstep0) with messages,
     * or at superstep0 with user defined message.
     */
    @Override
    default void compute(WorkerContext context,
                         Vertex vertex,
                         Iterator<M> messages) {
        M message = Combiner.combineAll(context.combiner(), messages);
        M result = this.computeMessage(context, vertex, message);
        if (result != null) {
            this.sendMessage(context, vertex, result);
        }
        this.updateState(vertex);
    }

    /**
     * Compute the vertex with combined message, or null if no message received.
     * The returned message will be sent to adjacent vertices.
     * For a vertex, this method can be called only one time in a superstep.
     * @param message Combined message, or null if no message received
     */
    M computeMessage(WorkerContext context, Vertex vertex, M message);

    /**
     * By default, computed result will be sent to all adjacent vertices.
     * The algorithm should override this method if the algorithm doesn't wants
     * to send the result along all edges.
     */
    default void sendMessage(WorkerContext context, Vertex vertex, M result) {
        context.sendMessageToAllEdges(vertex, result);
    }

    /**
     * Set vertex's state after computation, set inactive by default.
     */
    default void updateState(Vertex vertex) {
        vertex.inactivate();
    }
}