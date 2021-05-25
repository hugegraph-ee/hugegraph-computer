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

package com.baidu.hugegraph.computer.core.receiver.edge;

import com.baidu.hugegraph.computer.core.config.Config;
import com.baidu.hugegraph.computer.core.receiver.MessageRecvPartitions;
import com.baidu.hugegraph.computer.core.store.DataFileGenerator;

public class EdgeMessageRecvPartitions
       extends MessageRecvPartitions<EdgeMessageRecvPartition> {

    public EdgeMessageRecvPartitions(Config config,
                                     DataFileGenerator fileGenerator) {
        super(config, fileGenerator, -1);
    }

    @Override
    public EdgeMessageRecvPartition createPartition(int superstep) {
        return new EdgeMessageRecvPartition(this.config, this.fileGenerator);
    }
}