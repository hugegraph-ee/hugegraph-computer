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

package com.baidu.hugegraph.computer.k8s;

public class Constants {

    public static final int MASTER_INSTANCES = 1;
    public static final int TOTAL_COMPONENTS = 2;
    public static final int ALLOW_FAILED_JOBS = 0;
    public static final int ALLOW_FAILED_COMPONENTS = 0;

    // NO BACKOFF
    public static final int JOB_BACKOFF_LIMIT = 0;

    public static final String POD_REASON_UNSCHEDULABLE = "Unschedulable";

    public static final String WORKING_DIR = "/app/hugegraph-computer";
    public static final String COMPUTER_CONF_FILE = "computer-conf.yaml";

    public static final String POD_IP = "POD_IP";
    public static final String POD_NAMESPACE = "POD_NAMESPACE";
    public static final String POD_NAME = "POD_NAME";

    public static final String POD_IP_PATH = "status.podIP";
    public static final String POD_NAMESPACE_PATH = "metadata.namespace";
    public static final String POD_NAME_PATH = "metadata.name";

    public static final String CONFIG_MAP_VOLUME = "config-map-volume";
    public static final String CONFIG_FILE_PATH = WORKING_DIR + "/conf";

    public static final int TRANSPORT_PORT = 9000;
    public static final int RPC_PORT = 8090;

    public static final String TRANSPORT_PORT_KEY = "transport.server_port";
    public static final String RPC_PORT_KEY = "rpc.server_port";
    public static final String TRANSPORT_IP_KEY = "transport.server_host";
    public static final String RPC_IP_KEY = "rpc.server_host";
    public static final String WORKER_COUNT_KEY = "job.workers_count";
}
