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

package com.baidu.hugegraph.computer.core.util;

import org.junit.Test;

import com.baidu.hugegraph.computer.core.graph.id.Id;
import com.baidu.hugegraph.computer.core.graph.id.LongId;
import com.baidu.hugegraph.computer.core.graph.value.IdValue;
import com.baidu.hugegraph.testutil.Assert;

public class IdValueUtilTest {

    @Test
    public void testConvertIdAndIdValue() {
        Id id1 = new LongId(1L);
        Id id2 = new LongId(2L);
        IdValue value1 = IdValueUtil.toIdValue(id1, 11);
        IdValue value2 = IdValueUtil.toIdValue(id2, 11);
        Assert.assertEquals(id1, IdValueUtil.toId(value1));
        Assert.assertEquals(id2, IdValueUtil.toId(value2));
        Assert.assertNotEquals(id1, IdValueUtil.toId(value2));
    }
}
