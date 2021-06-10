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

import com.baidu.hugegraph.computer.core.common.ComputerContext;
import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.computer.core.graph.id.Id;
import com.baidu.hugegraph.computer.core.graph.value.Value;
import com.baidu.hugegraph.computer.core.graph.vertex.Vertex;
import com.baidu.hugegraph.computer.core.io.BytesOutput;
import com.baidu.hugegraph.computer.core.io.IOFactory;
import com.baidu.hugegraph.computer.core.io.RandomAccessInput;
import com.baidu.hugegraph.util.E;

public class WriteBuffers {

    // For writing
    private WriteBuffer writingBuffer;
    // For sorting
    private WriteBuffer sortingBuffer;

    public WriteBuffers(ComputerContext context, int threshold, int capacity) {
        E.checkArgument(threshold > 0,
                        "The threshold of buffer must be > 0, actual got %s",
                        threshold);
        E.checkArgument(capacity > 0,
                        "The capacity of buffer must be > 0, actual got %s",
                        capacity);
        E.checkArgument(threshold <= capacity,
                        "The threshold must be <= capacity, actual got %s > %s",
                        threshold, capacity);
        this.writingBuffer = new WriteBuffer(context, threshold, capacity);
        this.sortingBuffer = new WriteBuffer(context, threshold, capacity);
    }

    public boolean reachThreshold() {
        return this.writingBuffer.reachThreshold();
    }

    public boolean isEmpty() {
        return this.writingBuffer.isEmpty();
    }

    public synchronized void writeVertex(Vertex vertex) throws IOException {
        this.writingBuffer.writeVertex(vertex);
    }

    public synchronized void writeEdges(Vertex vertex) throws IOException {
        this.writingBuffer.writeEdges(vertex);
    }

    public synchronized void writeMessage(Id targetId, Value<?> value)
                                          throws IOException {
        this.writingBuffer.writeMessage(targetId, value);
    }

    public synchronized void switchForSorting() {
        if (!this.reachThreshold()) {
            return;
        }
        this.prepareSorting();
    }

    /**
     * Can remove synchronized if MessageSendManager.finish() only called by
     * single thread
     */
    public synchronized void prepareSorting() {
        // Ensure last sorting task finished
        while (!this.sortingBuffer.isEmpty()) {
            try {
                this.wait();
            } catch (InterruptedException e) {
                throw new ComputerException("Interrupted when waiting " +
                                            "sorting buffer empty");
            }
        }
        // Swap the writing buffer and sorting buffer pointer
        WriteBuffer temp = this.writingBuffer;
        this.writingBuffer = this.sortingBuffer;
        this.sortingBuffer = temp;
    }

    public synchronized void finishSorting() {
        try {
            this.sortingBuffer.clear();
        } catch (IOException e) {
            throw new ComputerException("Failed to clear sorting buffer");
        }
        this.notify();
    }

    public synchronized RandomAccessInput wrapForRead() {
        BytesOutput output = this.sortingBuffer.output();
        return IOFactory.createBytesInput(output.buffer(),
                                          (int) output.position());
    }

    public long size() {
        return this.sortingBuffer.output().position();
    }
}
