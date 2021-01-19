/*
 *  Copyright 2017 HugeGraph Authors
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with this
 *  work for additional information regarding copyright ownership. The ASF
 *  licenses this file to You under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */

package com.baidu.hugegraph.computer.core.bsp;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.baidu.hugegraph.computer.core.common.exception.ComputerException;
import com.baidu.hugegraph.testutil.Assert;

public class EtcdClientTest {

    private static String ENDPOINTS = "http://localhost:2379";
    private static String NAMESPACE = "test_job_0001";
    private static String KEY_PREFIX = "/key";
    private static String KEY1 = "/key1";
    private static String KEY2 = "/key2";
    private static String NO_SUCH_KEY = "/no-such-key";
    private static byte[] VALUE1 = "value1".getBytes(UTF_8);
    private static byte[] VALUE2 = "value2".getBytes(UTF_8);

    private EtcdClient client;

    @Before
    public void setup() {
        this.client = new EtcdClient(ENDPOINTS, NAMESPACE);
    }

    @After
    public void tearDown() {
        this.client.close();
    }
    
    @Test
    public void testPut() {
        this.client.put(KEY1, VALUE1);
        byte[] bytes = this.client.get(KEY1);
        Assert.assertArrayEquals(VALUE1, bytes);
        this.client.delete(KEY1);
    }

    @Test
    public void testGet() {
        this.client.put(KEY1, VALUE1);
        byte[] bytes1 = this.client.get(KEY1);
        Assert.assertArrayEquals(VALUE1, bytes1);
        byte[] bytes2 = this.client.get(NO_SUCH_KEY);
        Assert.assertNull(bytes2);
        this.client.delete(KEY1);
    }

    @Test
    public void testGetByNotExistKey() {
        this.client.put(KEY1, VALUE1);
        byte[] bytes1 = this.client.get(KEY1, true);
        Assert.assertArrayEquals(VALUE1, bytes1);
        byte[] bytes2 = this.client.get(NO_SUCH_KEY, false);
        Assert.assertNull(bytes2);
        Assert.assertThrows(ComputerException.class, () -> {
            client.get(NO_SUCH_KEY, true);
        });
        this.client.delete(KEY1);
    }

    @Test
    public void testGetWithTimeout() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Runnable putThread = () -> {
            try {
                TimeUnit.MILLISECONDS.sleep(100L);
            } catch (InterruptedException e) {
                // Do nothing.
            }
            this.client.put(KEY2, VALUE2);
            this.client.put(KEY1, VALUE1);
        };
        executorService.submit(putThread);
        byte[] bytes1 = this.client.get(KEY1, 1000L, true);
        executorService.shutdown();
        executorService.awaitTermination(1L, TimeUnit.SECONDS);
        Assert.assertArrayEquals(VALUE1, bytes1);
        Assert.assertThrows(ComputerException.class, () -> {
            this.client.get(NO_SUCH_KEY, 1000L, true);
        });
        byte[] bytes2 = this.client.get(NO_SUCH_KEY, 1000L, false);
        Assert.assertTrue(bytes2 == null);
        this.client.delete(KEY1);
        this.client.delete(KEY2);
    }

    @Test
    public void testGetWithPrefix() {
        this.client.put(KEY1, VALUE1);
        this.client.put(KEY2, VALUE2);
        List<byte[]> valueList1  = this.client.getWithPrefix(KEY_PREFIX);
        Assert.assertEquals(2, valueList1.size());
        List<byte[]> valueList2 = this.client.getWithPrefix(NO_SUCH_KEY);
        Assert.assertEquals(0, valueList2.size());
        this.client.delete(KEY1);
        this.client.delete(KEY2);
    }

    @Test
    public void testGetWithPrefixAndCount() throws InterruptedException {
        this.client.put(KEY2, VALUE2);
        this.client.put(KEY1, VALUE1);
        List<byte[]> valueList1 = this.client.getWithPrefix(KEY_PREFIX, 2,
                                                            true);
        Assert.assertEquals(2, valueList1.size());
        Assert.assertArrayEquals(VALUE1, valueList1.get(0));
        Assert.assertArrayEquals(VALUE2, valueList1.get(1));

        Assert.assertThrows(ComputerException.class, () -> {
            this.client.getWithPrefix(NO_SUCH_KEY, 1, true);
        });

        List<byte[]> values = this.client.getWithPrefix(NO_SUCH_KEY, 1, false);
        Assert.assertEquals(0, values.size());
        this.client.delete(KEY1);
        this.client.delete(KEY2);
    }

    @Test
    public void testGetWithPrefixAndTimeout() throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        Runnable putThread = () -> {
            try {
                TimeUnit.MILLISECONDS.sleep(100L);
            } catch (InterruptedException e) {
                // Do nothing.
            }
            this.client.put(KEY1, VALUE1);
            this.client.put(KEY1, VALUE1);
            this.client.put(KEY2, VALUE2);
        };
        executorService.submit(putThread);
        List<byte[]> valueList1 = this.client.getWithPrefix(KEY_PREFIX, 2,
                                                            1000L, true);
        executorService.shutdown();
        executorService.awaitTermination(1L, TimeUnit.SECONDS);
        Assert.assertEquals(2, valueList1.size());
        Assert.assertArrayEquals(VALUE1, valueList1.get(0));
        Assert.assertArrayEquals(VALUE2, valueList1.get(1));

        List<byte[]> values2 = this.client.getWithPrefix(KEY_PREFIX, 3,
                                                         1000L, false);
        Assert.assertEquals(2, values2.size());
        Assert.assertThrows(ComputerException.class, () -> {
            this.client.getWithPrefix(NO_SUCH_KEY, 1, 1000L, true);
        });

        List<byte[]> values3 = this.client.getWithPrefix(NO_SUCH_KEY, 1, 1000L,
                                                         false);
        Assert.assertEquals(0, values3.size());
        this.client.delete(KEY1);
        this.client.delete(KEY2);
    }

    @Test
    public void testDelete() {
        this.client.put(KEY1, VALUE1);
        long deleteCount1 = this.client.delete(KEY1);
        Assert.assertEquals(1L, deleteCount1);
        long deleteCount2 = this.client.delete(NO_SUCH_KEY);
        Assert.assertEquals(0L, deleteCount2);
    }

    @Test
    public void testDeleteWithPrefix() {
        this.client.put(KEY1, VALUE1);
        this.client.put(KEY2, VALUE2);
        this.client.put(NO_SUCH_KEY, VALUE2);
        long deleteCount1 = this.client.deleteWithPrefix(KEY_PREFIX);
        Assert.assertEquals(2L, deleteCount1);
        long deleteCount2 = this.client.delete(NO_SUCH_KEY);
        Assert.assertEquals(1L, deleteCount2);
    }

    @Test
    public void testDeleteAllKvInNamespace() {
        this.client.put(KEY1, VALUE1);
        this.client.put(KEY2, VALUE2);
        long deleteCount1 = this.client.deleteAllKvsInNamespace();
        Assert.assertEquals(2L, deleteCount1);
        long deleteCount2 = this.client.delete(KEY1);
        Assert.assertEquals(0L, deleteCount2);
    }
}
