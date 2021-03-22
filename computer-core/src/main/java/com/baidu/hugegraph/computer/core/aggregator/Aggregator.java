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

package com.baidu.hugegraph.computer.core.aggregator;

import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.computer.core.graph.value.Value;

public interface Aggregator<V extends Value> {

    /**
     * Workers used to add a new value, needs to be commutative and associative.
     * @param value The value to be aggregated
     */
    default void aggregateValue(V value) {
                 throw new ComputerException("Not implemented");
    }

    /**
     * Aggregate an int value. For performance reasons, it can aggregate without
     * create an IntValue object.
     */
    default void aggregateValue(int value) {
                 throw new ComputerException("Not implemented");
    }

    /**
     * Aggregate a long value. For performance reasons, it can aggregate without
     * create a LongValue object.
     */
    default void aggregateValue(long value) {
                 throw new ComputerException("Not implemented");
    }

    /**
     * Aggregate a float value. For performance reasons, it can aggregate
     * without create a FloatValue object.
     */
    default void aggregateValue(float value) {
                 throw new ComputerException("Not implemented");
    }

    /**
     * Aggregate a double value. For performance reasons, it can aggregate
     * without create a DoubleValue object.
     */
    default void aggregateValue(double value) {
                 throw new ComputerException("Not implemented");
    }

    /**
     * Workers or Master can get get aggregated value.
     */
    V aggregatedValue();

    /**
     * Master can set the aggregated value.
     */
    void aggregatedValue(V value);
}