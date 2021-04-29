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

package com.baidu.hugegraph.computer.core.store.util;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.computer.core.io.RandomAccessInput;
import com.baidu.hugegraph.computer.core.store.entry.DefaultKvEntry;
import com.baidu.hugegraph.computer.core.store.entry.KvEntryWithFirstSubKv;
import com.baidu.hugegraph.computer.core.store.iter.EntriesInput;
import com.baidu.hugegraph.computer.core.store.entry.DefaultPointer;
import com.baidu.hugegraph.computer.core.store.entry.KvEntry;
import com.baidu.hugegraph.computer.core.store.entry.Pointer;

public final class EntriesUtil {

    private static final int DEFAULT_CAPACITY = 100000;

    public static List<KvEntry> readInput(RandomAccessInput input) {
        List<KvEntry> pointers = new ArrayList<>(DEFAULT_CAPACITY);
        EntriesInput entriesInput = new EntriesInput(input);
        while (entriesInput.hasNext()) {
            pointers.add(entriesInput.next());
        }
        return pointers;
    }

    public static Pointer valuePointerByKeyPointer(Pointer keyPointer)
                                                   throws IOException {
        RandomAccessInput input = keyPointer.input();
        long position = input.position();
        input.seek(keyPointer.offset());
        input.skip(keyPointer.length());
        // Read value
        int valueLength = input.readInt();
        DefaultPointer value = new DefaultPointer(input, input.position(),
                                                  valueLength);
        input.seek(position);
        return value;
    }

    public static List<KvEntry> subKvsFromEntry(KvEntry kvEntry)
                                                throws IOException {
        Pointer value = kvEntry.value();
        RandomAccessInput input = value.input();
        input.seek(value.offset());
        int subKvSize = input.readInt();
        List<KvEntry> subKvs = new ArrayList<>(subKvSize);
        for (int i = 0; i < subKvSize; i++) {
            subKvs.add(EntriesUtil.entryFromInput(input));
        }
        return subKvs;
    }

    public static Iterator<KvEntry> subKvIterFromEntry(KvEntry entry)
                                                       throws IOException {
        return new SubKvIterator(entry);
    }

    private static class SubKvIterator implements Iterator<KvEntry> {

        private final RandomAccessInput input;
        private long position;
        private final long size;

        public SubKvIterator(KvEntry kvEntry) throws IOException {
            Pointer value = kvEntry.value();
            this.input = value.input();
            this.position = 0L;
            this.input.seek(value.offset());
            this.size = this.input.readInt();
            this.position = this.input.position();
        }

        @Override
        public boolean hasNext() {
            return this.size > 0;
        }

        @Override
        public KvEntry next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }

            try {
                this.input.seek(this.position);
                KvEntry entry = EntriesUtil.entryFromInput(this.input);
                this.position = this.input.position();

                return entry;
            } catch (IOException e) {
                throw new ComputerException(e.getMessage(), e);
            }
        }
    }

    public static KvEntry entryFromInput(RandomAccessInput input)
                                         throws IOException {
        return entryFromInput(input, input);
    }

    public static KvEntry entryFromInput(RandomAccessInput input,
                                         RandomAccessInput externalInput)
                                         throws IOException {
        // Read key
        int keyLength = input.readInt();
        long keyOffset = input.position();
        input.skip(keyLength);

        // Read value
        int valueLength = input.readInt();
        long valueOffset = input.position();
        input.skip(valueLength);

        Pointer key = new DefaultPointer(externalInput, keyOffset, keyLength);
        Pointer value = new DefaultPointer(externalInput, valueOffset,
                                           valueLength);
        return new DefaultKvEntry(key, value);
    }

    public static KvEntryWithFirstSubKv kvEntryWithFirstSubKv(KvEntry entry)
                                        throws IOException {
        RandomAccessInput input = entry.value().input();
        input.seek(entry.value().offset());
        // Skip sub-entry size
        input.skip(Integer.BYTES);
        KvEntry firstSubKv = EntriesUtil.entryFromInput(input);

        return new KvEntryWithFirstSubKv(entry.key(), entry.value(),
                                         firstSubKv);
    }
}
