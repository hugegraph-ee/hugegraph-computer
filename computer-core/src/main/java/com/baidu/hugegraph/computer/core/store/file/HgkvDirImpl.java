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
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.baidu.hugegraph.computer.core.util.BytesUtil;
import com.baidu.hugegraph.util.E;

public class HgkvDirImpl extends AbstractHgkvFile implements HgkvDir {

    public static final String NAME_PREFIX;
    public static final String EXTEND_NAME;
    private static final String NAME_REGEX;
    private static final Pattern FILE_NUM_PATTERN;

    static {
        NAME_PREFIX = "hgkv_";
        EXTEND_NAME = ".hgkv";
        NAME_REGEX = NAME_PREFIX + "[0-9]+" + EXTEND_NAME;
        FILE_NUM_PATTERN = Pattern.compile("[0-9]+");
    }

    private final List<HgkvFile> segments;

    private HgkvDirImpl(String path) {
        this(path, null);
    }

    private HgkvDirImpl(String path, List<HgkvFile> segments) {
        super(path);
        this.segments = segments;
    }

    public static HgkvDir create(String path) throws IOException {
        File file = new File(path);
        E.checkArgument(file.getName().matches(NAME_REGEX),
                        "Illegal hgkv file name '%s'", path);
        E.checkArgument(!file.exists(),
                        "Can't create HgkvDir, because the " +
                        "directory already exists '%s'", file.getPath());
        file.mkdirs();
        return new HgkvDirImpl(path);
    }

    public static HgkvDir open(String path) throws IOException {
        File file = new File(path);
        E.checkArgument(file.getName().matches(NAME_REGEX),
                        "Illegal hgkv file name '%s'", path);
        E.checkArgument(file.exists(), "Path not exists '%s'", file.getPath());
        E.checkArgument(file.isDirectory(), "Path is not directory '%s'",
                        file.getPath());
        return open(file);
    }

    private static File[] scanHgkvFiles(File dir) {
        return dir.listFiles((dirName, name) -> name.matches(NAME_REGEX));
    }

    private static HgkvDir open(File file) throws IOException {
        File[] files = scanHgkvFiles(file);
        assert files != null && files.length != 0;

        // Open segments
        List<HgkvFile> segments = segmentsFromFiles(files);

        // Set HgkvDir properties
        HgkvDirImpl hgkvDir = new HgkvDirImpl(file.getPath(), segments);
        hgkvDir.build();

        return hgkvDir;
    }

    private static List<HgkvFile> segmentsFromFiles(File[] files)
                                  throws IOException {
        List<HgkvFile> segments = new ArrayList<>();
        for (File file : files) {
            segments.add(HgkvFileImpl.open(file));
        }
        segments.sort((o1, o2) -> {
            int id1 = fileNameToSegmentId(o1.path());
            int id2 = fileNameToSegmentId(o2.path());
            return Integer.compare(id1, id2);
        });
        return segments;
    }

    private static int fileNameToSegmentId(String path) {
        String fileName = Paths.get(path).getFileName().toString();
        Matcher matcher = FILE_NUM_PATTERN.matcher(fileName);
        E.checkState(matcher.find(), "Illegal file name '%s'", fileName);
        return Integer.parseInt(matcher.group());
    }

    @Override
    public void close() throws IOException {
        // pass
    }

    @Override
    public byte[] max() {
        return this.max;
    }

    @Override
    public byte[] min() {
        return this.min;
    }

    @Override
    public List<HgkvFile> segments() {
        return this.segments;
    }

    private void build() throws IOException {
        this.magic = MAGIC;
        this.version = PRIMARY_VERSION + "." + MINOR_VERSION;
        this.entriesSize = this.segments.stream()
                                        .mapToLong(HgkvFile::numEntries)
                                        .sum();
        this.max = this.segments.stream()
                                .map(HgkvFile::max)
                                .max(BytesUtil::compare)
                                .orElse(null);
        this.min = this.segments.stream()
                                .map(HgkvFile::min)
                                .min(BytesUtil::compare)
                                .orElse(null);
        // Close segments
        for (HgkvFile segment : this.segments) {
            segment.close();
        }
    }
}
