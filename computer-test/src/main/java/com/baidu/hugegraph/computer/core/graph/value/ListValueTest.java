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

package com.baidu.hugegraph.computer.core.graph.value;

import java.io.IOException;

import org.apache.commons.collections.ListUtils;
import org.junit.Test;

import com.baidu.hugegraph.computer.core.BaseCoreTest;
import com.baidu.hugegraph.testutil.Assert;
import com.google.common.collect.Lists;

public class ListValueTest extends BaseCoreTest {

    @Test
    public void test() {
        ListValue<IntValue> listValue1 = new ListValue<>(ValueType.INT);
        ListValue<IntValue> listValue2 = new ListValue<>(ValueType.INT);

        listValue1.add(new IntValue(100));
        listValue2.add(new IntValue(100));

        Assert.assertEquals(ValueType.LIST_VALUE, listValue1.type());
        Assert.assertEquals(ValueType.INT, listValue1.elemType());
        Assert.assertTrue(ListUtils.isEqualList(
                          Lists.newArrayList(new IntValue(100)),
                          listValue1.values()));
        Assert.assertEquals(listValue1, listValue2);

        listValue2.add(new IntValue(200));
        Assert.assertTrue(ListUtils.isEqualList(
                          Lists.newArrayList(new IntValue(100),
                                             new IntValue(200)),
                          listValue2.values()));
        Assert.assertNotEquals(listValue1, listValue2);
        Assert.assertEquals(ListUtils.hashCodeForList(
                            Lists.newArrayList(new IntValue(100))),
                            listValue1.hashCode());
    }

    @Test
    public void testReadWrite() throws IOException {
        ListValue<IntValue> oldValue = new ListValue<>(ValueType.INT);
        oldValue.add(new IntValue(100));
        oldValue.add(new IntValue(200));
        assertValueEqualAfterWriteAndRead(oldValue);
    }
}
