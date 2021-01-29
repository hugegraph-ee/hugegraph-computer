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

package com.baidu.hugegraph.computer.core.bsp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.configuration.MapConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

import com.baidu.hugegraph.computer.core.common.ComputerOptions;
import com.baidu.hugegraph.computer.core.common.ContainerInfo;
import com.baidu.hugegraph.computer.core.common.UnitTestBase;
import com.baidu.hugegraph.computer.core.graph.GraphStat;
import com.baidu.hugegraph.computer.core.graph.partition.PartitionStat;
import com.baidu.hugegraph.computer.core.worker.WorkerStat;
import com.baidu.hugegraph.config.HugeConfig;
import com.baidu.hugegraph.testutil.Assert;
import com.baidu.hugegraph.util.Log;

public class EtcdBspTest extends UnitTestBase {

    private static final Logger LOG = Log.logger(EtcdBspTest.class);

    private Bsp4Master bsp4Master;
    private Bsp4Worker bsp4Worker;
    private ContainerInfo masterInfo;
    private ContainerInfo workerInfo;
    private ExecutorService executorService = Executors.newFixedThreadPool(2);
    private int maxSuperStep;

    @Before
    public void init() {
        Map<String, Object> map = new HashMap<>();
        String job_id = "local_001";
        map.put(ComputerOptions.JOB_ID.name(), job_id);
        map.put(ComputerOptions.JOB_WORKERS_COUNT.name(), 1);
        map.put(ComputerOptions.BSP_LOG_INTERVAL.name(), 200L);
        map.put(ComputerOptions.BSP_MAX_SUPER_STEP.name(), 2);
        MapConfiguration configuration = new MapConfiguration(map);
        HugeConfig config = new HugeConfig(configuration);
        this.bsp4Master = new EtcdBsp4Master(config);
        this.bsp4Master.init();
        this.masterInfo = new ContainerInfo(-1, "localhost", 8001, 8002);
        this.workerInfo = new ContainerInfo(0, "localhost", 8003, 8004);
        this.bsp4Worker = new EtcdBsp4Worker(config, this.workerInfo);
        this.bsp4Worker.init();
        this.maxSuperStep = config.get(ComputerOptions.BSP_MAX_SUPER_STEP);
    }

    @After
    public void tearDown() {
        this.bsp4Worker.close();
        this.bsp4Master.clean();
        this.bsp4Master.close();
    }

    @Test
    public void testRegister() throws InterruptedException {
        // If both two threads reach countDown, it means no exception is thrown.
        CountDownLatch countDownLatch = new CountDownLatch(2);
        this.executorService.submit(() -> {
            this.bsp4Master.registerMaster(this.masterInfo);
            List<ContainerInfo> workers = this.bsp4Master
                                              .waitWorkersRegistered();
            Assert.assertEquals(1, workers.size());
            Assert.assertEquals(this.workerInfo, workers.get(0));
            countDownLatch.countDown();
        });
        this.executorService.submit(() -> {
            this.bsp4Worker.registerWorker();
            ContainerInfo master = this.bsp4Worker.waitMasterRegistered();
            Assert.assertEquals(this.masterInfo, master);
            List<ContainerInfo> workers = this.bsp4Worker
                                              .waitWorkersRegistered();
            Assert.assertEquals(1, workers.size());
            Assert.assertEquals(this.workerInfo, workers.get(0));
            countDownLatch.countDown();
        });
        countDownLatch.await();
    }

    @Test
    public void testInput() throws InterruptedException {
        // If both two threads reach countDown, it means no exception is thrown.
        WorkerStat workerStat = new WorkerStat();
        workerStat.add(new PartitionStat(0, 100L, 200L));
        workerStat.add(new PartitionStat(1, 200L, 300L));
        CountDownLatch countDownLatch = new CountDownLatch(2);
        this.executorService.submit(() -> {
            this.bsp4Master.masterSuperstepResume(-1);
            this.bsp4Master.waitWorkersInputDone();
            this.bsp4Master.masterInputDone();
            List<WorkerStat> workerStats = this.bsp4Master
                                               .waitWorkersSuperstepDone(-1);
            Assert.assertEquals(1, workerStats.size());
            Assert.assertEquals(workerStat, workerStats.get(0));
            countDownLatch.countDown();
        });
        this.executorService.submit(() -> {
            int firstSuperStep = this.bsp4Worker.waitMasterSuperstepResume();
            Assert.assertEquals(-1, firstSuperStep);
            this.bsp4Worker.workerInputDone();
            this.bsp4Worker.waitMasterInputDone();
            this.bsp4Worker.workerSuperstepDone(-1, workerStat);
            countDownLatch.countDown();
        });
        countDownLatch.await();
    }

    @Test
    public void testIterate() throws InterruptedException {
        // If both two threads reach countDown, it means no exception is thrown.
        WorkerStat workerStat = new WorkerStat();
        workerStat.add(new PartitionStat(0, 100L, 200L));
        workerStat.add(new PartitionStat(1, 200L, 300L));
        CountDownLatch countDownLatch = new CountDownLatch(2);
        this.executorService.submit(() -> {
            for (int i = 0; i < this.maxSuperStep; i++) {
                this.bsp4Master.waitWorkersSuperstepPrepared(i);
                this.bsp4Master.masterSuperstepPrepared(i);
                List<WorkerStat> list = this.bsp4Master
                                            .waitWorkersSuperstepDone(i);
                GraphStat graphStat = new GraphStat();
                for (WorkerStat workerStat1 : list) {
                    graphStat.increase(workerStat1);
                }
                if (i == this.maxSuperStep - 1) {
                    graphStat.halt(true);
                }
                this.bsp4Master.masterSuperstepDone(i, graphStat);
            }
            countDownLatch.countDown();

        });
        this.executorService.submit(() -> {
            int superstep = -1;
            GraphStat graphStat = null;
            while (graphStat == null || !graphStat.halt()) {
                superstep++;
                this.bsp4Worker.workerSuperstepPrepared(superstep);
                this.bsp4Worker.waitMasterSuperstepPrepared(superstep);
                PartitionStat stat1 = new PartitionStat(0, 100L, 200L,
                                                        50L, 60L, 70L);
                PartitionStat stat2 = new PartitionStat(1, 200L, 300L,
                                                        80L, 90L, 100);
                WorkerStat workerStatInSuperstep = new WorkerStat();
                workerStatInSuperstep.add(stat1);
                workerStatInSuperstep.add(stat2);
                // Sleep some time to simulate the worker do computation.
                sleep(1000L);
                this.bsp4Worker.workerSuperstepDone(superstep,
                                                    workerStatInSuperstep);
                graphStat = this.bsp4Worker.waitMasterSuperstepDone(superstep);
            }
            countDownLatch.countDown();
        });
        countDownLatch.await();
    }

    @Test
    public void testOutput() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(2);
        this.executorService.submit(() -> {
            this.bsp4Master.waitWorkersOutputDone();
            this.bsp4Master.clean();
            countDownLatch.countDown();

        });
        this.executorService.submit(() -> {
            this.bsp4Worker.workerOutputDone();
            countDownLatch.countDown();
        });
        countDownLatch.await();
    }
}
