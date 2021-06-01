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

package com.baidu.hugegraph.computer.core.sort.sorting;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.baidu.hugegraph.computer.core.combiner.Combiner;
import com.baidu.hugegraph.computer.core.combiner.OverwriteCombiner;
import com.baidu.hugegraph.computer.core.common.ComputerContext;
import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.computer.core.config.ComputerOptions;
import com.baidu.hugegraph.computer.core.config.Config;
import com.baidu.hugegraph.computer.core.io.RandomAccessInput;
import com.baidu.hugegraph.computer.core.io.RandomAccessOutput;
import com.baidu.hugegraph.computer.core.io.UnsafeBytesOutput;
import com.baidu.hugegraph.computer.core.manager.Manager;
import com.baidu.hugegraph.computer.core.network.message.MessageType;
import com.baidu.hugegraph.computer.core.sender.WriteBuffers;
import com.baidu.hugegraph.computer.core.sort.Sorter;
import com.baidu.hugegraph.computer.core.sort.SorterImpl;
import com.baidu.hugegraph.computer.core.sort.flusher.CombineKvInnerSortFlusher;
import com.baidu.hugegraph.computer.core.sort.flusher.CombineSubKvInnerSortFlusher;
import com.baidu.hugegraph.computer.core.sort.flusher.InnerSortFlusher;
import com.baidu.hugegraph.computer.core.store.hgkvfile.entry.Pointer;
import com.baidu.hugegraph.util.ExecutorUtil;
import com.baidu.hugegraph.util.Log;

public class SortManager implements Manager {

    public static final Logger LOG = Log.logger(SortManager.class);

    private static final String NAME = "sort";
    private static final String PREFIX = "sort-executor-";

    private final ExecutorService sortExecutor;
    private final Sorter sorter;
    private final Combiner<Pointer> combiner;
    private final int capacity;
    private final int flushThreshold;

    public SortManager(ComputerContext context) {
        Config config = context.config();
        int threadNum = config.get(ComputerOptions.SORT_THREAD_NUMS);
        this.sortExecutor = ExecutorUtil.newFixedThreadPool(threadNum, PREFIX);
        this.sorter = new SorterImpl(config);
        this.combiner = new OverwriteCombiner<>();
        this.capacity = config.get(
                        ComputerOptions.WORKER_WRITE_BUFFER_INIT_CAPACITY);
        this.flushThreshold = config.get(
                              ComputerOptions.INPUT_MAX_EDGES_IN_ONE_VERTEX);
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void init(Config config) {
        // pass
    }

    @Override
    public void close(Config config) {
        this.sortExecutor.shutdown();
        try {
            this.sortExecutor.awaitTermination(5000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOG.warn("Waiting sort executor terminated was interrupted");
        }
    }

    public CompletableFuture<ByteBuffer> sort(MessageType type,
                                              WriteBuffers buffer) {
        return CompletableFuture.supplyAsync(() -> {
            RandomAccessInput bufferForRead = buffer.wrapForRead();
            // TODO：This ByteBuffer should be allocated from the off-heap
            UnsafeBytesOutput output = new UnsafeBytesOutput(this.capacity);
            InnerSortFlusher flusher = this.createSortFlusher(
                                       type, output,
                                       this.flushThreshold);
            try {
                this.sorter.sortBuffer(bufferForRead, flusher);
            } catch (Exception e) {
                throw new ComputerException("Failed to sort buffer", e);
            }
            return ByteBuffer.wrap(output.buffer());
        }, this.sortExecutor);
    }

    private InnerSortFlusher createSortFlusher(MessageType type,
                                               RandomAccessOutput output,
                                               int flushThreshold) {
        if (type == MessageType.VERTEX || type == MessageType.MSG) {
            return new CombineKvInnerSortFlusher(output, this.combiner);
        } else {
            assert type == MessageType.EDGE;
            return new CombineSubKvInnerSortFlusher(output, this.combiner,
                                                    flushThreshold);
        }
    }
}
