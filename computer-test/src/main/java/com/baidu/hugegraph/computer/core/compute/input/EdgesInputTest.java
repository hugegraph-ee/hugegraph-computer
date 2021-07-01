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

package com.baidu.hugegraph.computer.core.compute.input;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.function.Consumer;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.baidu.hugegraph.computer.core.common.Constants;
import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.computer.core.compute.FileGraphPartition;
import com.baidu.hugegraph.computer.core.compute.MockMessageSender;
import com.baidu.hugegraph.computer.core.config.ComputerOptions;
import com.baidu.hugegraph.computer.core.config.Config;
import com.baidu.hugegraph.computer.core.config.EdgeFrequency;
import com.baidu.hugegraph.computer.core.config.Null;
import com.baidu.hugegraph.computer.core.graph.edge.Edge;
import com.baidu.hugegraph.computer.core.graph.edge.Edges;
import com.baidu.hugegraph.computer.core.graph.id.Id;
import com.baidu.hugegraph.computer.core.graph.id.LongId;
import com.baidu.hugegraph.computer.core.graph.properties.Properties;
import com.baidu.hugegraph.computer.core.graph.value.IdValueList;
import com.baidu.hugegraph.computer.core.graph.value.IdValueListList;
import com.baidu.hugegraph.computer.core.graph.value.LongValue;
import com.baidu.hugegraph.computer.core.graph.vertex.Vertex;
import com.baidu.hugegraph.computer.core.io.BytesOutput;
import com.baidu.hugegraph.computer.core.io.IOFactory;
import com.baidu.hugegraph.computer.core.manager.Managers;
import com.baidu.hugegraph.computer.core.network.ConnectionId;
import com.baidu.hugegraph.computer.core.network.buffer.ManagedBuffer;
import com.baidu.hugegraph.computer.core.network.message.MessageType;
import com.baidu.hugegraph.computer.core.receiver.MessageRecvManager;
import com.baidu.hugegraph.computer.core.receiver.ReceiverUtil;
import com.baidu.hugegraph.computer.core.sender.MessageSendManager;
import com.baidu.hugegraph.computer.core.sort.sorting.SortManager;
import com.baidu.hugegraph.computer.core.store.FileManager;
import com.baidu.hugegraph.computer.core.store.hgkvfile.entry.EntryOutput;
import com.baidu.hugegraph.computer.core.store.hgkvfile.entry.EntryOutputImpl;
import com.baidu.hugegraph.computer.core.store.hgkvfile.entry.KvEntryWriter;
import com.baidu.hugegraph.computer.suite.unit.UnitTestBase;
import com.baidu.hugegraph.testutil.Whitebox;

public class EdgesInputTest extends UnitTestBase {

    private Config config;
    private Managers managers;

    @After
    public void teardown() {
        this.managers.closeAll(this.config);
    }

    @Test
    public void testSingle() throws IOException {
        this.testEdgeFreq(EdgeFrequency.SINGLE);
    }

    @Test
    public void testSinglePerLabel() throws IOException {
        this.testEdgeFreq(EdgeFrequency.SINGLE_PER_LABEL);
    }

    @Test
    public void testMultiple() throws IOException {
        this.testEdgeFreq(EdgeFrequency.MULTIPLE);
    }

    private void testEdgeFreq(EdgeFrequency freq)
                              throws IOException {
        this.config = UnitTestBase.updateWithRequiredOptions(
            ComputerOptions.JOB_ID, "local_001",
            ComputerOptions.JOB_WORKERS_COUNT, "1",
            ComputerOptions.JOB_PARTITIONS_COUNT, "1",
            ComputerOptions.WORKER_COMBINER_CLASS,
            Null.class.getName(), // Can't combine
            ComputerOptions.ALGORITHM_RESULT_CLASS,
            IdValueListList.class.getName(),
            ComputerOptions.ALGORITHM_MESSAGE_CLASS,
            IdValueList.class.getName(),
            ComputerOptions.WORKER_DATA_DIRS, "[data_dir1, data_dir2]",
            ComputerOptions.WORKER_RECEIVED_BUFFERS_BYTES_LIMIT, "10000",
            ComputerOptions.WORKER_WAIT_FINISH_MESSAGES_TIMEOUT, "1000",
            ComputerOptions.INPUT_MAX_EDGES_IN_ONE_VERTEX, "10",
            ComputerOptions.INPUT_EDGE_FREQ, freq.name()
        );
        this.managers = new Managers();
        FileManager fileManager = new FileManager();
        this.managers.add(fileManager);
        SortManager sortManager = new SortManager(context());
        this.managers.add(sortManager);

        MessageSendManager sendManager = new MessageSendManager(
                                         context(), sortManager,
                                         new MockMessageSender());
        this.managers.add(sendManager);
        MessageRecvManager receiveManager = new MessageRecvManager(context(),
                                                                   fileManager,
                                                                   sortManager);
        this.managers.add(receiveManager);
        this.managers.initAll(this.config);
        ConnectionId connectionId = new ConnectionId(new InetSocketAddress(
                                                     "localhost", 8081),
                                                     0);
        FileGraphPartition<?> partition = new FileGraphPartition(context(),
                                                                 this.managers,
                                                                 0);
        receiveManager.onStarted(connectionId);
        add200VertexBuffer((ManagedBuffer buffer) -> {
            receiveManager.handle(MessageType.VERTEX, 0, buffer);
        });
        receiveManager.onFinished(connectionId);
        receiveManager.onStarted(connectionId);
        addEdgeBuffer((ManagedBuffer buffer) -> {
            receiveManager.handle(MessageType.EDGE, 0, buffer);
        }, freq);

        receiveManager.onFinished(connectionId);
        partition.init(receiveManager.vertexPartitions().get(0),
                       receiveManager.edgePartitions().get(0));
        File edgeFile = Whitebox.getInternalState(partition, "edgeFile");
        EdgesInput edgesInput = new EdgesInput(context(), edgeFile);
        edgesInput.init();
        this.checkEdgesInput(edgesInput, freq);
        edgesInput.close();
    }

    private static void add200VertexBuffer(Consumer<ManagedBuffer> consumer)
                                           throws IOException {
        for (long i = 0L; i < 200L; i += 2) {
            Vertex vertex = graphFactory().createVertex();
            vertex.id(new LongId(i));
            vertex.properties(graphFactory().createProperties());
            ReceiverUtil.comsumeBuffer(writeVertex(vertex), consumer);
        }
    }

    private static byte[] writeVertex(Vertex vertex) throws IOException {
        BytesOutput bytesOutput = IOFactory.createBytesOutput(
                                  Constants.SMALL_BUF_SIZE);
        EntryOutput entryOutput = new EntryOutputImpl(bytesOutput);

        entryOutput.writeEntry(out -> {
            out.writeByte(vertex.id().type().code());
            vertex.id().write(out);
        }, out -> {
            vertex.properties().write(out);
        });

        return bytesOutput.toByteArray();
    }

    private static void addEdgeBuffer(Consumer<ManagedBuffer> consumer,
                                      EdgeFrequency freq) throws IOException {
        for (long i = 0L; i < 200L; i++) {
            Vertex vertex = graphFactory().createVertex();
            vertex.id(new LongId(i));
            int count = (int) i;
            if (count == 0) {
                continue;
            }
            Edges edges = graphFactory().createEdges(count);

            for (long j = 0; j < count; j++) {
                Edge edge = graphFactory().createEdge();
                switch (freq) {
                    case SINGLE:
                        edge.targetId(new LongId(j));
                        break;
                    case SINGLE_PER_LABEL:
                        edge.label(String.valueOf(j));
                        edge.targetId(new LongId(j));
                        break;
                    case MULTIPLE:
                        edge.name(String.valueOf(j));
                        edge.label(String.valueOf(j));
                        edge.targetId(new LongId(j));
                        break;
                    default:
                        throw new ComputerException(
                                  "Illegal edge frequency %s", freq);
                }

                Properties properties = graphFactory().createProperties();
                properties.put("p1", new LongValue(i));
                edge.properties(properties);
                edges.add(edge);
            }
            vertex.edges(edges);
            ReceiverUtil.comsumeBuffer(writeEdges(vertex, freq), consumer);
        }
    }

    private static byte[] writeEdges(Vertex vertex, EdgeFrequency freq)
                                     throws IOException {
        BytesOutput bytesOutput = IOFactory.createBytesOutput(
                                  Constants.SMALL_BUF_SIZE);
        EntryOutput entryOutput = new EntryOutputImpl(bytesOutput);

        Id id = vertex.id();
        KvEntryWriter subKvWriter = entryOutput.writeEntry(out -> {
            out.writeByte(id.type().code());
            id.write(out);
        });
        for (Edge edge : vertex.edges()) {
            Id targetId = edge.targetId();
            subKvWriter.writeSubKv(out -> {
                switch (freq) {
                    case SINGLE:
                        out.writeByte(targetId.type().code());
                        targetId.write(out);
                        break;
                    case SINGLE_PER_LABEL:
                        out.writeUTF(edge.label());
                        out.writeByte(targetId.type().code());
                        targetId.write(out);
                        break;
                    case MULTIPLE:
                        out.writeUTF(edge.label());
                        out.writeUTF(edge.name());
                        out.writeByte(targetId.type().code());
                        targetId.write(out);
                        break;
                    default:
                        throw new ComputerException(
                                  "Illegal edge frequency %s", freq);
                }
            }, out -> {
                edge.properties().write(out);
            });
        }
        subKvWriter.writeFinish();
        return bytesOutput.toByteArray();
    }

    private void checkEdgesInput(EdgesInput edgesInput, EdgeFrequency freq)
                                 throws IOException {

        for (long i = 0L; i < 200L; i+= 2) {
            LongId id = new LongId(i);
            ReusablePointer idPointer = idToReusablePointer(id);
            Edges edges = edgesInput.edges(idPointer);
            Iterator<Edge> edgesIt = edges.iterator();
            Assert.assertEquals(i, edges.size());
            for (int j = 0; j < edges.size(); j++) {
                Assert.assertTrue(edgesIt.hasNext());
                Edge edge = edgesIt.next();
                switch (freq) {
                    case SINGLE:
                        Assert.assertEquals(new LongId(j), edge.targetId());
                        break;
                    case SINGLE_PER_LABEL:
                        Assert.assertEquals(new LongId(j), edge.targetId());
                        Assert.assertEquals(String.valueOf(j), edge.label());
                        break;
                    case MULTIPLE:
                        Assert.assertEquals(new LongId(j), edge.targetId());
                        Assert.assertEquals(String.valueOf(j), edge.label());
                        Assert.assertEquals(String.valueOf(j), edge.name());
                        break;
                    default:
                        throw new ComputerException(
                                  "Illegal edge frequency %s", freq);
                }
            }
            Assert.assertFalse(edgesIt.hasNext());
        }
    }

    public static ReusablePointer idToReusablePointer(Id id)
                                                      throws IOException {
        BytesOutput output = IOFactory.createBytesOutput(9);
        output.writeByte(id.type().code());
        id.write(output);
        return new ReusablePointer(output.buffer(), (int) output.position());
    }
}