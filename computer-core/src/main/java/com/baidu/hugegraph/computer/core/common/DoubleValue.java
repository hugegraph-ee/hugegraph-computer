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

package com.baidu.hugegraph.computer.core.common;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public class DoubleValue implements Value {

    private double value;

    public DoubleValue() {
        this.value = 0.0D;
    }

    public DoubleValue(double value) {
        this.value = value;
    }

    @Override
    public void write(DataOutput out) throws IOException {
        out.writeDouble(this.value);
    }

    @Override
    public void read(DataInput in) throws IOException {
        this.value = in.readDouble();
    }

    public double value() {
        return this.value;
    }

    /*
     * This method is reserved for performance, otherwise it will create a new
     * DoubleValue object when change it's value.
     */
    public void value(double value) {
        this.value = value;
    }

    @Override
    public ValueType type() {
        return ValueType.DOUBLE;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof DoubleValue)) {
            return false;
        }
        return ((DoubleValue) obj).value == this.value;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(this.value);
    }

    @Override
    public String toString() {
        return String.valueOf(this.value);
    }
}
