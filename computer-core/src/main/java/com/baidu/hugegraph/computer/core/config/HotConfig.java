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

package com.baidu.hugegraph.computer.core.config;

import com.baidu.hugegraph.computer.core.graph.value.ValueType;

public final class HotConfig {

    private String algorithmName;
    private String vertexValueName;
    private String edgeValueName;
    private ValueType valueType;

    private boolean outputVertexOutEdges;
    private boolean outputVertexProperties;
    private boolean outputEdgeProperties;

    public String algorithmName() {
        return this.algorithmName;
    }

    public void algorithmName(String algorithmName) {
        this.algorithmName = algorithmName;
    }

    public String vertexValueName() {
        return this.vertexValueName;
    }

    public void vertexValueName(String vertexValueName) {
        this.vertexValueName = vertexValueName;
    }

    public String edgeValueName() {
        return this.edgeValueName;
    }

    public void edgeValueName(String edgeValueName) {
        this.edgeValueName = edgeValueName;
    }

    public ValueType valueType() {
        return this.valueType;
    }

    public void valueType(ValueType valueType) {
        this.valueType = valueType;
    }

    public boolean outputVertexOutEdges() {
        return this.outputVertexOutEdges;
    }

    public void outputVertexOutEdges(boolean outputVertexOutEdges) {
        this.outputVertexOutEdges = outputVertexOutEdges;
    }

    public boolean outputVertexProperties() {
        return this.outputVertexProperties;
    }

    public void outputVertexProperties(boolean outputVertexProperties) {
        this.outputVertexProperties = outputVertexProperties;
    }

    public boolean outputEdgeProperties() {
        return this.outputEdgeProperties;
    }

    public void outputEdgeProperties(boolean outputEdgeProperties) {
        this.outputEdgeProperties = outputEdgeProperties;
    }
}