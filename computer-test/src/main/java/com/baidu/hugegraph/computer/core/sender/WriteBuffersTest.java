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

package com.baidu.hugegraph.computer.core.sender;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.baidu.hugegraph.computer.core.common.ComputerContext;
import com.baidu.hugegraph.computer.core.graph.GraphFactory;
import com.baidu.hugegraph.computer.core.graph.id.LongId;
import com.baidu.hugegraph.computer.core.graph.id.Utf8Id;
import com.baidu.hugegraph.computer.core.graph.properties.Properties;
import com.baidu.hugegraph.computer.core.graph.value.DoubleValue;
import com.baidu.hugegraph.computer.core.graph.value.IntValue;
import com.baidu.hugegraph.computer.core.graph.value.ListValue;
import com.baidu.hugegraph.computer.core.graph.value.ValueType;
import com.baidu.hugegraph.computer.core.graph.vertex.Vertex;
import com.baidu.hugegraph.computer.core.io.RandomAccessInput;
import com.baidu.hugegraph.computer.core.io.StreamGraphInput;
import com.baidu.hugegraph.computer.suite.unit.UnitTestBase;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.testutil.Whitebox;
import com.google.common.collect.ImmutableList;

public class WriteBuffersTest extends UnitTestBase {

    private final ComputerContext context = ComputerContext.instance();

    @Test
    public void testConstructor() {
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new WriteBuffers(0, 20);
        }, e -> {
            Assert.assertTrue(e.getMessage().contains(
                              "The threshold of buffer must be > 0"));
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new WriteBuffers(10, -1);
        }, e -> {
            Assert.assertTrue(e.getMessage().contains(
                              "The capacity of buffer must be > 0"));
        });
        Assert.assertThrows(IllegalArgumentException.class, () -> {
            new WriteBuffers(20, 10);
        }, e -> {
            Assert.assertTrue(e.getMessage().contains(
                              "The threshold must be <= capacity"));
        });
        @SuppressWarnings("unused")
        WriteBuffers buffers = new WriteBuffers(10, 20);
    }

    @Test
    public void testReachThreshold() throws IOException {
        WriteBuffers buffers = new WriteBuffers(10, 50);
        Assert.assertFalse(buffers.reachThreshold());

        Vertex vertex = context.graphFactory().createVertex(
                        new LongId(1L), new DoubleValue(0.5d));
        // After write, the position is 5
        buffers.writeVertex(vertex);
        Assert.assertFalse(buffers.reachThreshold());

        // After write, the position is 10
        buffers.writeVertex(vertex);
        Assert.assertTrue(buffers.reachThreshold());

        // After write, the position is 15
        buffers.writeVertex(vertex);
        Assert.assertTrue(buffers.reachThreshold());
    }

    @Test
    public void testIsEmpty() throws IOException {
        WriteBuffers buffers = new WriteBuffers(10, 20);
        Assert.assertTrue(buffers.isEmpty());

        Vertex vertex = context.graphFactory().createVertex(
                        new LongId(1L), new DoubleValue(0.5d));
        buffers.writeVertex(vertex);
        Assert.assertFalse(buffers.isEmpty());
    }

    @Test
    public void testWriteVertex() throws IOException {
        GraphFactory graphFactory = context.graphFactory();

        // NOTE: need ensure the buffer size can hold follow writed bytes
        WriteBuffers buffers = new WriteBuffers(100, 110);
        Vertex vertex = graphFactory.createVertex(new LongId(1L),
                                                  new DoubleValue(0.5d));
        buffers.writeVertex(vertex);
        WriteBuffer buffer = Whitebox.getInternalState(buffers,
                                                       "writingBuffer");
        int position1 = Whitebox.getInternalState(buffer.output(), "position");
        Assert.assertGt(0, position1);

        vertex = graphFactory.createVertex(new LongId(1L),
                                           new DoubleValue(0.5d));
        Properties properties = graphFactory.createProperties();
        properties.put("name", new Utf8Id("marko").idValue());
        properties.put("age", new IntValue(18));
        properties.put("city", new ListValue<>(ValueType.ID_VALUE,
                               ImmutableList.of(new Utf8Id("wuhan").idValue(),
                                                new Utf8Id("xian").idValue())));
        vertex.properties(properties);
        buffers.writeVertex(vertex);
        buffer = Whitebox.getInternalState(buffers, "writingBuffer");
        int position2 = Whitebox.getInternalState(buffer.output(), "position");
        Assert.assertGt(position1, position2);

        vertex = graphFactory.createVertex(new LongId(1L),
                                           new DoubleValue(0.5d));
        vertex.addEdge(graphFactory.createEdge(new LongId(2L)));
        vertex.addEdge(graphFactory.createEdge("knows", new LongId(3L)));
        vertex.addEdge(graphFactory.createEdge("watch", "1111",
                                               new LongId(4L)));
        buffers.writeEdges(vertex);
        buffer = Whitebox.getInternalState(buffers, "writingBuffer");
        int position3 = Whitebox.getInternalState(buffer.output(), "position");
        Assert.assertGt(position2, position3);
    }

    @Test
    public void testPrepareSorting() throws IOException {
        GraphFactory graphFactory = context.graphFactory();

        WriteBuffers buffers = new WriteBuffers(50, 100);
        Vertex vertex = graphFactory.createVertex(new LongId(1L),
                                                  new DoubleValue(0.5d));
        vertex.addEdge(graphFactory.createEdge(new LongId(2L)));
        vertex.addEdge(graphFactory.createEdge("knows", new LongId(3L)));
        vertex.addEdge(graphFactory.createEdge("watch", "1111",
                                               new LongId(4L)));
        buffers.writeEdges(vertex);
        // Reached threshold, the position is 76
        Assert.assertTrue(buffers.reachThreshold());
        Assert.assertFalse(buffers.isEmpty());
        // Exchange writing buffer and sorting buffer
        buffers.prepareSorting();
        Assert.assertFalse(buffers.reachThreshold());
        Assert.assertTrue(buffers.isEmpty());
    }

    @Test
    public void testSwitchAndFinishSorting() throws IOException,
                                                    InterruptedException {
        GraphFactory graphFactory = context.graphFactory();

        WriteBuffers buffers = new WriteBuffers(50, 100);
        Vertex vertex = graphFactory.createVertex(new LongId(1L),
                                                  new DoubleValue(0.5d));
        vertex.addEdge(graphFactory.createEdge(new LongId(2L)));
        vertex.addEdge(graphFactory.createEdge("knows", new LongId(3L)));
        vertex.addEdge(graphFactory.createEdge("watch", "1111",
                                               new LongId(4L)));
        buffers.writeEdges(vertex);
        // Reached threshold, the position is 76
        Assert.assertTrue(buffers.reachThreshold());
        /*
         * When reached threshold, switchForSorting will exchange writing buffer
         * and sorting buffer, so the writing buffer become clean
         */
        buffers.switchForSorting();
        Assert.assertFalse(buffers.reachThreshold());
        Assert.assertTrue(buffers.isEmpty());
        // Nothing changed
        buffers.switchForSorting();
        Assert.assertFalse(buffers.reachThreshold());
        Assert.assertTrue(buffers.isEmpty());
        // The writing buffer reached threshold again, position is 76
        buffers.writeEdges(vertex);

        AtomicInteger counter = new AtomicInteger(0);
        Thread thread1 = new Thread(() -> {
            // Await until finishSorting method called
            buffers.switchForSorting();
            Assert.assertEquals(2, counter.get());
        });
        Thread thread2 = new Thread(() -> {
            while (counter.get() < 2) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Assert.fail(e.getMessage());
                }
                counter.incrementAndGet();
            }
            // counter is 2
            buffers.finishSorting();
        });
        thread1.start();
        thread2.start();

        thread1.join();
        thread2.join();
    }

//    @Test
    public void wrapForRead() throws IOException {
        GraphFactory graphFactory = context.graphFactory();

        WriteBuffers buffers = new WriteBuffers(10, 20);
        Vertex vertex = graphFactory.createVertex(new LongId(1L),
                                                  new DoubleValue(0.5d));
        buffers.writeVertex(vertex);

        try (RandomAccessInput input = buffers.wrapForRead()) {
            StreamGraphInput graphInput = new StreamGraphInput(context,
                                                               input);
            Vertex readVertex = graphInput.readVertex();
            Assert.assertEquals(vertex, readVertex);
        }
    }
}