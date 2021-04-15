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

package com.baidu.hugegraph.computer.core.store;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.computer.core.UnitTestBase;
import com.baidu.hugegraph.computer.core.config.ComputerOptions;
import com.baidu.hugegraph.computer.core.store.base.KvEntry;
import com.baidu.hugegraph.computer.core.store.base.Pointer;
import com.baidu.hugegraph.computer.core.store.file.HgkvDir;
import com.baidu.hugegraph.computer.core.store.file.HgkvDirImpl;
import com.baidu.hugegraph.computer.core.store.file.HgkvFileImpl;
import com.baidu.hugegraph.computer.core.store.file.builder.HgkvDirBuilder;
import com.baidu.hugegraph.computer.core.store.file.builder.HgkvDirBuilderImpl;
import com.baidu.hugegraph.computer.core.store.file.reader.HgkvDirReader;
import com.baidu.hugegraph.computer.core.store.file.reader.HgkvDirReaderImpl;
import com.baidu.hugegraph.testutil.Assert;
import com.google.common.collect.ImmutableList;

public class HgkvDirTest {

    @BeforeClass
    public static void setup() {
        UnitTestBase.updateWithRequiredOptions(
                ComputerOptions.HGKVFILE_SIZE, "32",
                ComputerOptions.DATABLOCK_SIZE, "16"
        );
    }

    @Test
    public void testHgkvDirBuilder() throws IOException {
        // The data must be ordered
        List<Integer> data = ImmutableList.of(2, 3,
                                              2, 1,
                                              5, 2,
                                              5, 9,
                                              6, 2);
        List<KvEntry> kvEntries = StoreTestData.kvEntriesFromMap(data);

        String userDir = System.getProperty("user.home");
        String basePath = userDir + File.separator + "hgkv";
        String dirPath = basePath + File.separator + "test_dir";
        try (HgkvDirBuilder builder = new HgkvDirBuilderImpl(dirPath)) {
            for (KvEntry kvEntry : kvEntries) {
                builder.write(kvEntry);
            }
            builder.finish();

            // Open the file and determine the footer is as expected
            HgkvDir dir = HgkvDirImpl.open(dirPath);
            Assert.assertEquals(HgkvFileImpl.MAGIC, dir.magic());
            Assert.assertEquals(HgkvFileImpl.VERSION, dir.version());
            Assert.assertEquals(5, dir.numEntries());

            Pointer max = dir.max();
            max.input().seek(max.offset());
            int maxKey = max.input().readInt();
            Assert.assertEquals(6, maxKey);

            Pointer min = dir.min();
            min.input().seek(min.offset());
            int minKey = min.input().readInt();
            Assert.assertEquals(2, minKey);
        } finally {
            FileUtils.deleteQuietly(new File(basePath));
        }
    }

    @Test
    public void testExceptionCase() throws IOException {
        // Path isn't directory
        File file = File.createTempFile(UUID.randomUUID().toString(), null);
        Assert.assertThrows(IllegalArgumentException.class,
                            () -> HgkvDirImpl.open(file.getPath()));
        FileUtils.deleteQuietly(file);

        // Open not exists file
        Assert.assertThrows(IllegalArgumentException.class,
                            () -> HgkvDirImpl.open(file.getPath()));
    }

    @Test
    public void testHgkvDirReader() throws IOException {
        // The keys in the data must be ordered
        List<Integer> data = ImmutableList.of(2, 3,
                                              2, 1,
                                              5, 2,
                                              5, 5,
                                              5, 9,
                                              6, 2);
        Iterator<Integer> result = ImmutableList.of(2, 5, 6).iterator();

        String userDir = System.getProperty("user.home");
        String basePath = userDir + File.separator + "hgkv";
        String dirPath = basePath + File.separator + "dir_1";
        File dir = StoreTestData.hgkvDirFromMap(data, dirPath);
        HgkvDirReader reader = new HgkvDirReaderImpl(dir.getPath());
        CloseableIterator<KvEntry> iterator;

        try {
            iterator = reader.iterator();
            while (iterator.hasNext()) {
                KvEntry entry = iterator.next();
                entry.key().input().seek(entry.key().offset());
                int key = entry.key().input().readInt();
                Assert.assertEquals(result.next().intValue(), key);
            }
            Assert.assertThrows(NoSuchElementException.class,
                                iterator::next);
            iterator.close();
        } finally {
            FileUtils.deleteQuietly(dir);
        }
    }
}
