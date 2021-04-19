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

package com.baidu.hugegraph.computer.core.store.file;

import java.io.File;
import java.io.IOException;

import com.baidu.hugegraph.computer.core.io.BufferedFileInput;
import com.baidu.hugegraph.computer.core.io.RandomAccessInput;
import com.baidu.hugegraph.computer.core.store.entry.DefaultPointer;
import com.baidu.hugegraph.computer.core.store.entry.Pointer;
import com.baidu.hugegraph.util.E;

public class HgkvFileImpl extends AbstractHgkvFile {

    public static final String VERSION;
    public static final String MAGIC;

    static {
        VERSION = "1.0";
        MAGIC = "hgkv";
    }

    public HgkvFileImpl(String path) {
        super(path);
    }

    public static HgkvFile create(String path) throws IOException {
        File file = new File(path);
        E.checkArgument(!file.exists(), "File already exists, path: '%s'",
                        file.getPath());
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        file.createNewFile();

        return new HgkvFileImpl(path);
    }

    public static HgkvFile open(String path) throws IOException {
        E.checkArgumentNotNull(path, "Path must not be null");
        return open(new File(path));
    }

    public static HgkvFile open(File file) throws IOException {
        E.checkArgumentNotNull(file, "File must not be null");
        E.checkArgument(file.exists(), "File not exists, path: '%s'",
                        file.getPath());
        E.checkArgument(file.isFile(), "Path is not file, path: '%s'",
                        file.getPath());

        HgkvFileImpl hgkvFile = new HgkvFileImpl(file.getPath());
        hgkvFile.readFooter();
        return hgkvFile;
    }

    private void readFooter() throws IOException {
        File file = new File(this.path);
        try (BufferedFileInput input = new BufferedFileInput(file)) {
            long fileSize = file.length();

            // Read magic
            long magicOffset = fileSize - HgkvFileImpl.MAGIC.length();
            this.magic = this.readMagic(input, magicOffset, file.getPath());

            // Read footer length
            long footerLengthOffset = magicOffset - Integer.BYTES;
            int footerLength = this.readFooterLength(input, footerLengthOffset);

            // Read numEntries
            long numEntriesOffset = fileSize - footerLength;
            this.numEntries = this.readNumEntries(input, numEntriesOffset);

            // Read max key and min key
            long maxOffset = numEntriesOffset + Long.BYTES;
            long minOffset = maxOffset + Long.BYTES;
            if (this.numEntries > 0) {
                this.max = this.readMax(input, maxOffset);
                this.min = this.readMin(input, minOffset);
            }

            // Read Version
            long dataBlockLengthOffset = minOffset + Long.BYTES;
            long indexBlockLengthOffset = dataBlockLengthOffset + Long.BYTES;
            long versionOffset = indexBlockLengthOffset + Long.BYTES;
            this.version = this.readVersion(input, versionOffset);
        }
    }

    private String readMagic(RandomAccessInput input, long magicOffset,
                             String path) throws IOException {
        input.seek(magicOffset);
        byte[] magicBytes = input.readBytes(HgkvFileImpl.MAGIC.length());
        String fileMagic = new String(magicBytes);
        E.checkArgument(HgkvFileImpl.MAGIC.equals(fileMagic),
                        "Illegal file '%s'", path);
        return fileMagic;
    }

    private int readFooterLength(RandomAccessInput input,
                                 long footerLengthOffset) throws IOException {
        input.seek(footerLengthOffset);
        return input.readInt();
    }

    private long readNumEntries(RandomAccessInput input,
                                long numEntriesOffset) throws IOException {
        input.seek(numEntriesOffset);
        return input.readLong();
    }

    private Pointer readMax(RandomAccessInput input, long maxOffset)
                            throws IOException {
        input.seek(maxOffset);
        long maxKeyOffset = input.readLong();
        input.seek(maxKeyOffset);
        int maxKeyLength = input.readInt();
        return new DefaultPointer(input, input.position(), maxKeyLength);
    }

    private Pointer readMin(RandomAccessInput input, long minOffset)
                            throws IOException {
        input.seek(minOffset);
        long maxKeyOffset = input.readLong();
        input.seek(maxKeyOffset);
        int minKeyLength = input.readInt();
        return new DefaultPointer(input, input.position(), minKeyLength);
    }

    private String readVersion(RandomAccessInput input, long versionOffset)
                               throws IOException {
        input.seek(versionOffset);
        int versionLength = input.readInt();
        byte[] version = input.readBytes(versionLength);
        return new String(version);
    }
}
