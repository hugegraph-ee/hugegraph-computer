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

import java.util.List;

/**
 * One HgkvDir consists of segments, and each segment is a HgkvFile.
 * one HgkvFile is a complete file, one HgkvDir consists of multiple HgkvFile.
 */
public interface HgkvDir extends HgkvFile {

    /**
     * Return the segments in HgkvDir.
     * A HgkvDir is a complete file, HgkvDir consists of multiple HgkvFile.
     */
    List<HgkvFile> segments();

    /**
     * Return the path to the next segment.
     */
    String nextSegmentPath();
}
