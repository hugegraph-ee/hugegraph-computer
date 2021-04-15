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

package com.baidu.hugegraph.computer.core.store.base;

import java.io.IOException;

import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.computer.core.io.RandomAccessInput;

public class DefaultPointer implements Pointer {

    private final RandomAccessInput input;
    private final long offset;
    private final long length;

    public DefaultPointer(RandomAccessInput input, long offset, long length) {
        this.input = input;
        this.offset = offset;
        this.length = length;
    }

    @Override
    public RandomAccessInput input() {
        return this.input;
    }

    @Override
    public long offset() {
        return this.offset;
    }

    @Override
    public long length() {
        return this.length;
    }

    @Override
    public int compareTo(Pointer other) {
        int result;
        try {
            result = this.input.compare(this.offset, this.length, other.input(),
                                        other.offset(), other.length());
        } catch (IOException e) {
            throw new ComputerException(e.getMessage(), e);
        }
        return result;
    }
}
