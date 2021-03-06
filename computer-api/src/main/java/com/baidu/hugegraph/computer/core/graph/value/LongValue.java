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

package com.baidu.hugegraph.computer.core.graph.value;

import java.io.IOException;

import com.baidu.hugegraph.computer.core.io.RandomAccessInput;
import com.baidu.hugegraph.computer.core.io.RandomAccessOutput;
import com.baidu.hugegraph.util.E;

public class LongValue implements Value<LongValue> {

    private long value;

    public LongValue() {
        this.value = 0L;
    }

    public LongValue(long value) {
        this.value = value;
    }

    public long value() {
        return this.value;
    }

    /*
     * This method is reserved for performance, otherwise it will create a new
     * LongValue object when change it's value.
     */
    public void value(long value) {
        this.value = value;
    }

    @Override
    public ValueType type() {
        return ValueType.LONG;
    }

    @Override
    public void assign(Value<LongValue> other) {
        this.checkAssign(other);
        this.value = ((LongValue) other).value;
    }

    @Override
    public LongValue copy() {
        return new LongValue(this.value);
    }

    @Override
    public void read(RandomAccessInput in) throws IOException {
        this.value = in.readLong();
    }

    @Override
    public void write(RandomAccessOutput out) throws IOException {
        out.writeLong(this.value);
    }

    @Override
    public int compareTo(LongValue obj) {
        E.checkArgumentNotNull(obj, "The compare argument can't be null");
        return Long.compare(this.value, obj.value);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof LongValue)) {
            return false;
        }
        return ((LongValue) obj).value == this.value;
    }

    @Override
    public int hashCode() {
        return Long.hashCode(this.value);
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }
}
