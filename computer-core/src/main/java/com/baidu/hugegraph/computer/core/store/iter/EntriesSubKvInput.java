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

package com.baidu.hugegraph.computer.core.store.iter;

import java.io.IOException;
import java.util.Iterator;

import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.computer.core.io.RandomAccessInput;
import com.baidu.hugegraph.computer.core.store.entry.KvEntry;
import com.baidu.hugegraph.computer.core.store.util.EntriesUtil;

public class EntriesSubKvInput implements InputIterator {

    private final Iterator<KvEntry> entries;

    public EntriesSubKvInput(RandomAccessInput input) {
        this.entries = new EntriesInput(input);
    }

    @Override
    public boolean hasNext() {
        return this.entries.hasNext();
    }

    @Override
    public KvEntry next() {
        try {
            return EntriesUtil.kvEntryWithFirstSubKv(this.entries.next());
        } catch (IOException e) {
            throw new ComputerException(e.getMessage(), e);
        }
    }

    @Override
    public void close() throws IOException {
        // pass
    }
}
