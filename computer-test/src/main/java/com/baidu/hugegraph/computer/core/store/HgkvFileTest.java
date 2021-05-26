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

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import com.baidu.hugegraph.computer.core.UnitTestBase;
import com.baidu.hugegraph.computer.core.config.ComputerOptions;
import com.baidu.hugegraph.computer.core.config.Config;
import com.baidu.hugegraph.computer.core.store.hgkvfile.entry.KvEntry;
import com.baidu.hugegraph.computer.core.store.hgkvfile.file.HgkvFile;
import com.baidu.hugegraph.computer.core.store.hgkvfile.file.HgkvFileImpl;
import com.baidu.hugegraph.computer.core.store.hgkvfile.file.builder.HgkvFileBuilder;
import com.baidu.hugegraph.computer.core.store.hgkvfile.file.builder.HgkvFileBuilderImpl;
import com.baidu.hugegraph.computer.core.store.hgkvfile.file.reader.HgkvFileReader;
import com.baidu.hugegraph.computer.core.store.hgkvfile.file.reader.HgkvFileReaderImpl;
import com.baidu.hugegraph.testutil.Assert;
import com.google.common.collect.ImmutableList;

public class HgkvFileTest {

    private static Config CONFIG;

    @BeforeClass
    public static void init() {
        CONFIG = UnitTestBase.updateWithRequiredOptions(
                ComputerOptions.HGKV_MAX_FILE_SIZE, "32",
                ComputerOptions.HGKV_DATABLOCK_SIZE, "16"
        );
    }

    @After
    public void teardown() throws IOException {
        FileUtils.deleteDirectory(new File(StoreTestUtil.FILE_DIR));
    }

    @Test
    public void testHgkvFileBuilder() throws IOException {
        // The data must be ordered
        List<Integer> data = testData();
        String filePath = StoreTestUtil.availablePathById("1");
        File file = null;
        try {
            file = StoreTestUtil.hgkvFileFromMap(CONFIG, data, filePath);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    @Test
    public void testOpenFile() throws IOException {
        // The keys in the data must be ordered
        List<Integer> data = testData();
        String filePath = StoreTestUtil.availablePathById("1");
        File file = null;
        try {
            file = StoreTestUtil.hgkvFileFromMap(CONFIG, data, filePath);
            // Open file
            HgkvFile hgkvFile = HgkvFileImpl.open(file.getPath());
            Assert.assertEquals(HgkvFileImpl.MAGIC, hgkvFile.magic());
            String version = HgkvFileImpl.PRIMARY_VERSION + "." +
                             HgkvFileImpl.MINOR_VERSION;
            Assert.assertEquals(version, hgkvFile.version());
            Assert.assertEquals(5, hgkvFile.numEntries());
            // Read max key
            int maxKey = StoreTestUtil.byteArrayToInt(hgkvFile.max());
            Assert.assertEquals(6, maxKey);
            // Read min key
            int minKey = StoreTestUtil.byteArrayToInt(hgkvFile.min());
            Assert.assertEquals(2, minKey);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    @Test
    public void testExceptionCase() throws IOException {
        // Exception to add key/value
        final String filePath = StoreTestUtil.availablePathById("1");
        try (HgkvFileBuilder builder = new HgkvFileBuilderImpl(CONFIG,
                                                               filePath)) {
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                builder.add(null);
            },
            e -> Assert.assertTrue(e.getMessage().contains("can't be null")));
            builder.finish();
            Assert.assertThrows(IllegalStateException.class, () -> {
                builder.add(null);
            },
            e -> Assert.assertTrue(e.getMessage().contains("finished")));
        } finally {
            FileUtils.deleteQuietly(new File(filePath));
        }

        // Open not exists file.
        File tempFile = null;
        try {
            tempFile = new File(StoreTestUtil.availablePathById("1"));
            File finalTempFile = tempFile;
            Assert.assertThrows(IllegalArgumentException.class, () -> {
                HgkvFileImpl.open(finalTempFile);
            },
            e -> Assert.assertTrue(e.getMessage().contains("not exists")));
        } finally {
            FileUtils.deleteQuietly(tempFile);
        }
    }

    @Test
    public void testHgkvFileReader() throws IOException {
        // The keys in the data must be ordered
        List<Integer> data = testData();
        String filePath = StoreTestUtil.availablePathById("1");
        File file = StoreTestUtil.hgkvFileFromMap(CONFIG, data, filePath);
        try {
            HgkvFileReader reader = new HgkvFileReaderImpl(file.getPath(),
                                                           false);
            Iterator<KvEntry> iterator = reader.iterator();
            int index = 0;
            while (iterator.hasNext()) {
                KvEntry next = iterator.next();
                int key = StoreTestUtil.byteArrayToInt(next.key().bytes());
                Assert.assertEquals(data.get(index).intValue(), key);
                index += 2;
            }
            Assert.assertThrows(NoSuchElementException.class,
                                iterator::next);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

    private static List<Integer> testData() {
        return ImmutableList.of(2, 3,
                                2, 1,
                                5, 2,
                                5, 9,
                                6, 2);
    }
}