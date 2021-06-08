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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.slf4j.Logger;

import com.baidu.hugegraph.computer.core.common.ComputerContext;
import com.baidu.hugegraph.computer.core.common.Constants;
import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.computer.core.config.ComputerOptions;
import com.baidu.hugegraph.computer.core.config.Config;
import com.baidu.hugegraph.computer.core.graph.id.Id;
import com.baidu.hugegraph.computer.core.graph.partition.Partitioner;
import com.baidu.hugegraph.computer.core.graph.value.Value;
import com.baidu.hugegraph.computer.core.graph.vertex.Vertex;
import com.baidu.hugegraph.computer.core.manager.Manager;
import com.baidu.hugegraph.computer.core.network.message.MessageType;
import com.baidu.hugegraph.computer.core.sort.sorting.SortManager;
import com.baidu.hugegraph.util.Log;

public class MessageSendManager implements Manager {

    public static final Logger LOG = Log.logger(MessageSendManager.class);

    public static final String NAME = "message_send";

    private final MessageSendBuffers buffers;
    private final Partitioner partitioner;
    private final SortManager sortManager;
    private final MessageSender sender;
    private final AtomicReference<Throwable> exception;

    public MessageSendManager(ComputerContext context, SortManager sortManager,
                              MessageSender sender) {
        this.buffers = new MessageSendBuffers(context);
        this.partitioner = context.config().createObject(
                           ComputerOptions.WORKER_PARTITIONER);
        // The sort and client manager is inited at WorkerService init()
        this.sortManager = sortManager;
        this.sender = sender;
        this.exception = new AtomicReference<>();
    }

    @Override
    public String name() {
        return NAME;
    }

    @Override
    public void init(Config config) {
        this.partitioner.init(config);
    }

    @Override
    public void close(Config config) {
        // pass
    }

    /**
     * There will multiple threads calling the method
     */
    public void sendVertex(Vertex vertex) {
        this.checkException();

        WriteBuffers buffer = this.sortIfTargetBufferIsFull(vertex.id(),
                                                            MessageType.VERTEX);
        try {
            // Write vertex to buffer
            buffer.writeVertex(vertex);
        } catch (IOException e) {
            throw new ComputerException("Failed to write vertex", e);
        }
    }

    public void sendEdge(Vertex vertex) {
        this.checkException();

        WriteBuffers buffer = this.sortIfTargetBufferIsFull(vertex.id(),
                                                            MessageType.EDGE);
        try {
            // Write edge to buffer
            buffer.writeEdges(vertex);
        } catch (IOException e) {
            throw new ComputerException("Failed to write vertex", e);
        }
    }

    public void sendMessage(Id targetId, Value<?> value) {
        this.checkException();

        WriteBuffers buffer = this.sortIfTargetBufferIsFull(targetId,
                                                            MessageType.MSG);
        try {
            // Write vertex to buffer
            buffer.writeMessage(targetId, value);
        } catch (IOException e) {
            throw new ComputerException("Failed to write message", e);
        }
    }

    /**
     * Start send message, put an START signal into queue
     * @param type the message type
     */
    public void startSend(MessageType type) {
        Map<Integer, WriteBuffers> all = this.buffers.all();
        Set<Integer> workerIds = all.keySet().stream()
                                    .map(this.partitioner::workerId)
                                    .collect(Collectors.toSet());
        this.sendControlMessageToWorkers(workerIds, MessageType.START);
        LOG.info("Start send message(type={})", type);
    }

    /**
     * Finsih send message, send the last buffer and put an END signal
     * into queue
     * @param type the message type
     */
    public void finishSend(MessageType type) {
        Map<Integer, WriteBuffers> all = this.buffers.all();
        this.sortLastBatchBuffer(all, type);

        Set<Integer> workerIds = all.keySet().stream()
                                    .map(this.partitioner::workerId)
                                    .collect(Collectors.toSet());
        this.sendControlMessageToWorkers(workerIds, MessageType.FINISH);
        LOG.info("Finish send message(type={})", type);
    }

    private WriteBuffers sortIfTargetBufferIsFull(Id id, MessageType type) {
        int partitionId = this.partitioner.partitionId(id);
        WriteBuffers buffer = this.buffers.get(partitionId);
        if (buffer.reachThreshold()) {
            // After switch, the buffer can be continued to write
            buffer.switchForSorting();
            this.sortThenSend(partitionId, type, buffer);
        }
        return buffer;
    }

    private Future<?> sortThenSend(int partitionId, MessageType type,
                                   WriteBuffers buffer) {
        int workerId = this.partitioner.workerId(partitionId);
        return this.sortManager.sort(type, buffer).thenAccept(sortedBuffer -> {
            // The following code is also executed in sort thread
            buffer.finishSorting();
            // Each target worker has a buffer queue
            QueuedMessage message = new QueuedMessage(partitionId, workerId,
                                                      type, sortedBuffer);
            try {
                this.sender.send(workerId, message);
            } catch (InterruptedException e) {
                throw new ComputerException("Waiting to put buffer into " +
                                            "queue was interrupted");
            }
        }).whenComplete((r, e) -> {
            if (e != null) {
                LOG.error("Failed to sort buffer or put sorted buffer " +
                          "into queue", e);
                // Just record the first error
                this.exception.compareAndSet(null, e);
            }
        });
    }

    private void sortLastBatchBuffer(Map<Integer, WriteBuffers> all,
                                     MessageType type) {
        List<Future<?>> futures = new ArrayList<>(all.size());
        // Sort and send the last buffer
        for (Map.Entry<Integer, WriteBuffers> entry : all.entrySet()) {
            int partitionId = entry.getKey();
            WriteBuffers buffer = entry.getValue();
            /*
             * If the last buffer has already been sorted and sent (empty),
             * there is no need to send again here
             */
            if (!buffer.isEmpty()) {
                buffer.prepareSorting();
                Future<?> future = this.sortThenSend(partitionId, type, buffer);
                futures.add(partitionId, future);
            }
        }
        this.checkException();

        // Wait all future finished
        try {
            for (Future<?> future : futures) {
                future.get(Constants.FUTURE_TIMEOUT, TimeUnit.SECONDS);
            }
        } catch (TimeoutException e) {
            throw new ComputerException("Wait sort future finished timeout", e);
        } catch (InterruptedException | ExecutionException e) {
            throw new ComputerException("Failed to wait sort future finished",
                                        e);
        }
    }

    private void sendControlMessageToWorkers(Set<Integer> workerIds,
                                             MessageType type) {
        Map<Integer, CompletableFuture<Void>> futures = new HashMap<>();
        try {
            for (Integer workerId : workerIds) {
                QueuedMessage message = new QueuedMessage(-1, workerId,
                                                          type, null);
                futures.put(workerId, this.sender.send(workerId, message));
            }
        } catch (InterruptedException e) {
            throw new ComputerException("Waiting to send message async " +
                                        "was interrupted");
        }

        try {
            for (Map.Entry<Integer, CompletableFuture<Void>> entry :
                futures.entrySet()) {
                CompletableFuture<Void> future = entry.getValue();
                future.get(Constants.FUTURE_TIMEOUT, TimeUnit.SECONDS);

                this.sender.reset(entry.getKey(), future);
            }
        } catch (TimeoutException e) {
            throw new ComputerException("Wait control message(%s) finished " +
                                        "timeout", e, type);
        } catch (InterruptedException | ExecutionException e) {
            throw new ComputerException("Failed to wait control message(%s) " +
                                        "finished", e, type);
        }
    }

    private void checkException() {
        if (this.exception.get() != null) {
            throw new ComputerException("Failed to send message",
                                        this.exception.get());
        }
    }
}
