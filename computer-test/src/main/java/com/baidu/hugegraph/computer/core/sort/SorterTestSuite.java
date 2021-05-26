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

package com.baidu.hugegraph.computer.core.sort;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import com.baidu.hugegraph.computer.core.sort.sorter.EntriesUtilTest;
import com.baidu.hugegraph.computer.core.sort.sorter.LargeDataSizeTest;
import com.baidu.hugegraph.computer.core.sort.sorter.SorterTest;
import com.baidu.hugegraph.computer.core.sort.sorting.HeapInputSortingTest;
import com.baidu.hugegraph.computer.core.sort.sorting.LoserTreeInputSortingTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    LoserTreeInputSortingTest.class,
    HeapInputSortingTest.class,
    EntriesUtilTest.class,
    SorterTest.class,
    LargeDataSizeTest.class
})
public class SorterTestSuite {
}