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

import com.baidu.hugegraph.computer.core.io.RandomAccessInput;
import com.baidu.hugegraph.computer.core.io.RandomAccessOutput;
import com.baidu.hugegraph.computer.core.io.UnsafeBytesInput;
import com.baidu.hugegraph.computer.core.util.BytesUtil;

public class InlinePointer implements Pointer {

    private final long length;
    private final byte[] bytes;

    public InlinePointer(byte[] bytes) {
        this.length = bytes.length;
        this.bytes = bytes;
    }

    @Override
    public RandomAccessInput input() {
        return new UnsafeBytesInput(this.bytes);
    }

    @Override
    public byte[] bytes() {
        return this.bytes;
    }

    @Override
    public void write(RandomAccessOutput output) throws IOException {
        output.writeInt((int) this.length);
        output.write(this.bytes);
    }

    @Override
    public long offset() {
        return -1L;
    }

    @Override
    public long length() {
        return this.length;
    }

    @Override
    public int compareTo(Pointer other) {
        return BytesUtil.compare(this.bytes, other.bytes());
    }
}
