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

package com.baidu.hugegraph.computer.core.receiver.vertex;

import java.io.File;
import java.io.IOException;
import java.util.function.Consumer;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

import com.baidu.hugegraph.computer.core.UnitTestBase;
import com.baidu.hugegraph.computer.core.config.ComputerOptions;
import com.baidu.hugegraph.computer.core.config.Config;
import com.baidu.hugegraph.computer.core.graph.id.Id;
import com.baidu.hugegraph.computer.core.graph.id.LongId;
import com.baidu.hugegraph.computer.core.graph.vertex.Vertex;
import com.baidu.hugegraph.computer.core.io.UnsafeBytesOutput;
import com.baidu.hugegraph.computer.core.network.buffer.ManagedBuffer;
import com.baidu.hugegraph.computer.core.network.message.MessageType;
import com.baidu.hugegraph.computer.core.receiver.ReceiverUtil;
import com.baidu.hugegraph.computer.core.sort.Sorter;
import com.baidu.hugegraph.computer.core.sort.SorterImpl;
import com.baidu.hugegraph.computer.core.sort.flusher.PeekableIterator;
import com.baidu.hugegraph.computer.core.store.FileManager;
import com.baidu.hugegraph.computer.core.store.hgkvfile.entry.EntryOutput;
import com.baidu.hugegraph.computer.core.store.hgkvfile.entry.EntryOutputImpl;
import com.baidu.hugegraph.computer.core.store.hgkvfile.entry.KvEntry;
import com.baidu.hugegraph.testutil.Assert;

public class VertexMessageRecvPartitionTest extends UnitTestBase {

    @Test
    public void testVertexMessageRecvPartition() throws IOException {
        Config config = UnitTestBase.updateWithRequiredOptions(
            ComputerOptions.JOB_ID, "local_001",
            ComputerOptions.JOB_WORKERS_COUNT, "1",
            ComputerOptions.JOB_PARTITIONS_COUNT, "1",
            ComputerOptions.WORKER_DATA_DIRS, "[data_dir1, data_dir2]",
            ComputerOptions.WORKER_RECEIVED_BUFFERS_BYTES_LIMIT, "10"
        );
        FileUtils.deleteQuietly(new File("data_dir1"));
        FileUtils.deleteQuietly(new File("data_dir2"));
        FileManager fileManager = new FileManager();
        fileManager.init(config);
        Sorter sorter = new SorterImpl(config);
        VertexMessageRecvPartition partition = new VertexMessageRecvPartition(
                                               config, fileManager, sorter);
        Assert.assertEquals(MessageType.VERTEX.name(), partition.type());
        consumeMockedVertexBuffer((ManagedBuffer buffer) -> {
            partition.addBuffer(buffer);
        });

        PeekableIterator<KvEntry> it = partition.iterator();
        checkPartitionIterator(it);
        Assert.assertFalse(it.hasNext());
        fileManager.close(config);
    }

    public static void consumeMockedVertexBuffer(
                       Consumer<ManagedBuffer> consumer)
                       throws IOException {
        for (long i = 0L; i < 10L; i++) {
            Vertex vertex = graphFactory().createVertex();
            vertex.id(new LongId(i));
            vertex.properties(graphFactory().createProperties());
            ReceiverUtil.comsumeBuffer(writeVertex(vertex),
                                       (ManagedBuffer buffer) -> {
                consumer.accept(buffer);
            });
        }
    }

    private static byte[] writeVertex(Vertex vertex) throws IOException {
        UnsafeBytesOutput bytesOutput = new UnsafeBytesOutput();
        EntryOutput entryOutput = new EntryOutputImpl(bytesOutput);

        entryOutput.writeEntry(out -> {
            out.writeByte(vertex.id().type().code());
            vertex.id().write(out);
        }, out -> {
            vertex.properties().write(out);
        });

        return bytesOutput.toByteArray();
    }

    public static void checkPartitionIterator(PeekableIterator<KvEntry> it)
                                              throws IOException {
        for (long i = 0L; i < 10L; i++) {
            Assert.assertTrue(it.hasNext());
            KvEntry entry = it.next();
            Id id = ReceiverUtil.readId(context(), entry.key());
            Assert.assertEquals(new LongId(i), id);
        }
    }
}
