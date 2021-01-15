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

package com.baidu.hugegraph.computer.core.io;

import java.io.DataOutputStream;
import java.io.IOException;

public class JsonStructGraphOutput extends StructGraphOutput {

    public JsonStructGraphOutput(DataOutputStream out) {
        super(out);
    }

    @Override
    public void writeObjectStart() throws IOException {
        this.writeRawString("{");
    }

    @Override
    public void writeObjectEnd() throws IOException {
        this.writeRawString("}");
    }

    public void writeKey(String key) throws IOException {
        this.writeString(key);
    }

    @Override
    public void writeJoiner() throws IOException {
        this.writeRawString(":");
    }

    public void writeSplitter() throws IOException {
        this.writeRawString(",");
    }
}