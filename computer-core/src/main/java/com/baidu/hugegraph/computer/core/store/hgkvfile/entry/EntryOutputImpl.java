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

package com.baidu.hugegraph.computer.core.store.hgkvfile.entry;

import java.io.IOException;

import com.baidu.hugegraph.computer.core.graph.id.Id;
import com.baidu.hugegraph.computer.core.io.RandomAccessOutput;
import com.baidu.hugegraph.computer.core.io.Writable;

public class EntryOutputImpl implements EntryOutput {

    private final RandomAccessOutput output;

    public EntryOutputImpl(RandomAccessOutput output) {
        this.output = output;
    }

    @Override
    public KvEntryWriter writeEntry(Id key) throws IOException {
        // Write key
        this.writeData(key);

        return new KvEntryWriterImpl(this.output);
    }

    @Override
    public void writeEntry(Id key, Writable value) throws IOException {
        // Write key
        this.writeData(key);
        // Write value
        this.writeData(value);
    }

    private void writeData(Writable data) throws IOException {
        // Write data length placeholder
        this.output.writeInt(0);
        long position = this.output.position();
        // Write data
        data.write(this.output);
        // Fill data length placeholder
        int dataLength = (int) (this.output.position() - position);
        this.output.writeInt(position - Integer.BYTES, dataLength);
    }
}
