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

package com.baidu.hugegraph.computer.core.combiner;

import org.junit.Test;

import com.baidu.hugegraph.computer.core.graph.id.Utf8Id;
import com.baidu.hugegraph.computer.core.graph.properties.DefaultProperties;
import com.baidu.hugegraph.computer.core.graph.properties.Properties;
import com.baidu.hugegraph.testutil.Assert;

public class DefaultPropertiesCombinerTest {

    @Test
    public void test() {
        Properties properties1 = new DefaultProperties();
        properties1.put("name", new Utf8Id("marko").idValue());
        properties1.put("city", new Utf8Id("Beijing").idValue());

        Properties properties2 = new DefaultProperties();
        properties1.put("name", new Utf8Id("josh").idValue());

        PropertiesCombiner combiner = new DefaultPropertiesCombiner();
        Properties properties = combiner.combine(properties1, properties2);
        Assert.assertEquals(properties2, properties);
    }
}
