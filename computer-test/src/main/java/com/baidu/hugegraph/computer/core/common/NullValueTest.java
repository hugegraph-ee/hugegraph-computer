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

package com.baidu.hugegraph.computer.core.common;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.Test;

import com.baidu.hugegraph.computer.core.graph.value.NullValue;
import com.baidu.hugegraph.computer.core.graph.value.ValueType;
import com.baidu.hugegraph.testutil.Assert;

public class NullValueTest {

    @Test
    public void test() {
        NullValue nullValue1 = NullValue.get();
        NullValue nullValue2 = NullValue.get();
        Assert.assertEquals(ValueType.NULL, nullValue1.type());
        Assert.assertEquals(NullValue.get(), nullValue1);
        Assert.assertEquals(0, nullValue1.hashCode());
        Assert.assertEquals("<null>", nullValue1.toString());
        Assert.assertEquals(nullValue1, nullValue2);
    }

    @Test
    public void testReadWrite() throws IOException {
        NullValue nullValue = NullValue.get();
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutput dataOutput = new DataOutputStream(bos);
        nullValue.write(dataOutput);
        bos.close();
        ByteArrayInputStream bais = new ByteArrayInputStream(bos.toByteArray());
        DataInputStream dis = new DataInputStream(bais);
        NullValue newValue = NullValue.get();
        newValue.read(dis);
        Assert.assertEquals(nullValue, newValue);
        bais.close();
    }
}
