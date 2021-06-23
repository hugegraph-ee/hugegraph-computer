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

package com.baidu.hugegraph.computer.k8s.operator.controller;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;

import com.baidu.hugegraph.computer.k8s.Constants;
import com.baidu.hugegraph.computer.k8s.crd.model.ComputerJobSpec;
import com.baidu.hugegraph.computer.k8s.crd.model.HugeGraphComputerJob;
import com.baidu.hugegraph.computer.k8s.crd.model.Protocol;
import com.baidu.hugegraph.computer.k8s.crd.model.ResourceName;
import com.baidu.hugegraph.computer.k8s.util.KubeUtil;
import com.baidu.hugegraph.util.Log;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ConfigMapBuilder;
import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ContainerBuilder;
import io.fabric8.kubernetes.api.model.ContainerPort;
import io.fabric8.kubernetes.api.model.ContainerPortBuilder;
import io.fabric8.kubernetes.api.model.EnvVar;
import io.fabric8.kubernetes.api.model.EnvVarBuilder;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.PodSpec;
import io.fabric8.kubernetes.api.model.PodSpecBuilder;
import io.fabric8.kubernetes.api.model.Quantity;
import io.fabric8.kubernetes.api.model.Volume;
import io.fabric8.kubernetes.api.model.VolumeBuilder;
import io.fabric8.kubernetes.api.model.VolumeMount;
import io.fabric8.kubernetes.api.model.VolumeMountBuilder;
import io.fabric8.kubernetes.api.model.batch.v1.Job;
import io.fabric8.kubernetes.api.model.batch.v1.JobBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;

public class ComputerJobDeployer {

    private static final Logger LOG = Log.logger(ComputerJobDeployer.class);

    private final KubernetesClient kubeClient;

    public ComputerJobDeployer(KubernetesClient kubeClient) {
        this.kubeClient = kubeClient;
    }

    public void deploy(ComputerJobComponent observed) {
        HugeGraphComputerJob computerJob = observed.computerJob();

        Set<ContainerPort> ports = this.handleConfig(computerJob.getSpec());

        ComputerJobComponent desired = new ComputerJobComponent();
        desired.configMap(this.desiredConfigMap(computerJob));
        desired.masterJob(this.desiredMasterJob(computerJob, ports));
        desired.workerJob(this.desiredWorkerJob(computerJob, ports));

        this.reconcileComponent(computerJob.getMetadata().getNamespace(),
                                desired, observed);
    }

    private void reconcileComponent(String namespace,
                                    ComputerJobComponent desired,
                                    ComputerJobComponent observed) {
        ConfigMap desiredConfigMap = desired.configMap();
        ConfigMap observedConfigMap = observed.configMap();
        if (desiredConfigMap == null && observedConfigMap != null) {
            this.kubeClient.configMaps()
                           .inNamespace(namespace)
                           .delete(observedConfigMap);
        } else if (desiredConfigMap != null && observedConfigMap == null) {
            KubeUtil.ignoreExists(() -> {
                return this.kubeClient.configMaps()
                                      .inNamespace(namespace)
                                      .create(desiredConfigMap);
            });
        }
        if (desiredConfigMap != null && observedConfigMap != null) {
            LOG.info("ConfigMap already exists, no action");
        }

        Job desiredMasterJob = desired.masterJob();
        Job observedMasterJob = observed.masterJob();
        if (desiredMasterJob == null && observedMasterJob != null) {
            this.kubeClient.batch().v1().jobs().inNamespace(namespace)
                           .delete(observedMasterJob);
        } else if (desiredMasterJob != null && observedMasterJob == null) {
            KubeUtil.ignoreExists(() -> {
                return this.kubeClient.batch().v1().jobs()
                                      .inNamespace(namespace)
                                      .create(desiredMasterJob);
            });
        }
        if (desiredMasterJob != null && observedMasterJob != null) {
            LOG.info("MasterJob already exists, no action");
        }

        Job desiredWorkerJob = desired.workerJob();
        Job observedWorkerJob = observed.workerJob();
        if (desiredWorkerJob == null && observedWorkerJob != null) {
            this.kubeClient.batch().v1().jobs().inNamespace(namespace)
                           .delete(observedWorkerJob);
        } else if (desiredWorkerJob != null && observedWorkerJob == null) {
            KubeUtil.ignoreExists(() -> {
                return this.kubeClient.batch().v1().jobs()
                                      .inNamespace(namespace)
                                      .create(desiredWorkerJob);
            });
        }
        if (desiredWorkerJob != null && observedWorkerJob != null) {
            LOG.info("WorkerJob already exists, no action");
        }
    }

    private Set<ContainerPort> handleConfig(ComputerJobSpec spec) {
        Map<String, String> config = spec.getComputerConf();

        config.put(Constants.WORKER_COUNT,
                   String.valueOf(spec.getWorkerInstances()));

        config.put(Constants.TRANSPORT_IP,
                   String.format("${%s}", Constants.POD_IP));
        config.put(Constants.RPC_IP,
                   String.format("${%s}", Constants.POD_IP));

        String transportPort = config.computeIfAbsent(
                Constants.TRANSPORT_PORT,
                k -> String.valueOf(Constants.DEFAULT_TRANSPORT_PORT));
        String rpcPort = config.computeIfAbsent(
                Constants.RPC_PORT,
                k -> String.valueOf(Constants.DEFAULT_RPC_PORT));

        ContainerPort transportContainerPort = new ContainerPortBuilder()
                .withName(Constants.TRANSPORT_PORT)
                .withContainerPort(Integer.valueOf(transportPort))
                .withProtocol(Protocol.TCP.value())
                .build();
        ContainerPort rpcContainerPort = new ContainerPortBuilder()
                .withName(Constants.RPC_PORT)
                .withContainerPort(Integer.valueOf(rpcPort))
                .withProtocol(Protocol.TCP.value())
                .build();

        return Sets.newHashSet(transportContainerPort, rpcContainerPort);
    }

    private ConfigMap desiredConfigMap(HugeGraphComputerJob computerJob) {
        String crName = computerJob.getMetadata().getName();
        ComputerJobSpec spec = computerJob.getSpec();

        Map<String, String> computerConf = spec.getComputerConf();
        Map<String, String> data = new HashMap<>();
        data.put(Constants.COMPUTER_CONF_FILE,
                 KubeUtil.asProperties(computerConf));

        String name = KubeUtil.configMapName(crName);

        return new ConfigMapBuilder()
                .withMetadata(this.getMetadata(computerJob, name))
                .withData(data)
                .build();
    }

    public Job desiredMasterJob(HugeGraphComputerJob computerJob,
                                Set<ContainerPort> ports) {
        String crName = computerJob.getMetadata().getName();
        ComputerJobSpec spec = computerJob.getSpec();

        List<String> command = Lists.newArrayList("/bin/sh" , "-c");
        List<String> args = Lists.newArrayList("master.sh");

        String name = KubeUtil.masterJobName(crName);

        ObjectMeta metadata = this.getMetadata(computerJob, name);

        Container container = this.getContainer(name, spec, ports,
                                                command, args);
        List<Container> containers = Lists.newArrayList(container);

        int instances = Constants.MASTER_INSTANCES;
        return this.getJob(crName, metadata, spec, instances, containers);
    }

    public Job desiredWorkerJob(HugeGraphComputerJob computerJob,
                                Set<ContainerPort> ports) {
        String crName = computerJob.getMetadata().getName();
        ComputerJobSpec spec = computerJob.getSpec();

        List<String> command = Lists.newArrayList("/bin/sh" , "-c");
        List<String> args = Lists.newArrayList("worker.sh");

        String name = KubeUtil.workerJobName(crName);

        ObjectMeta metadata = this.getMetadata(computerJob, name);

        Container container = this.getContainer(name, spec, ports,
                                                command, args);
        List<Container> containers = Lists.newArrayList(container);

        int instances = spec.getWorkerInstances();
        return this.getJob(crName, metadata, spec, instances, containers);
    }

    private Job getJob(String crName, ObjectMeta meta, ComputerJobSpec spec,
                       int instances, List<Container> containers) {
        PodSpec podSpec = new PodSpecBuilder()
                .withContainers(containers)
                .withImagePullSecrets(spec.getPullSecrets())
                .withVolumes(this.getConfigVolume(crName))
                .build();

        return new JobBuilder()
                .withMetadata(meta)
                .withNewSpec()
                .withParallelism(instances)
                .withCompletions(instances)
                .withBackoffLimit(Constants.JOB_BACKOFF_LIMIT)
                .withNewTemplate()
                .withNewMetadata()
                .withLabels(meta.getLabels())
                .endMetadata()
                .withSpec(podSpec)
                .endTemplate()
                .endSpec()
                .build();
    }

    private Container getContainer(String name, ComputerJobSpec spec,
                                   Collection<ContainerPort> ports,
                                   Collection<String> command,
                                   Collection<String> args) {
        List<EnvVar> envVars = spec.getEnvVars();
        if (envVars == null) {
            envVars = new ArrayList<>();
        }

        // Add Pod info to env
        EnvVar podIP = new EnvVarBuilder()
                .withName(Constants.POD_IP)
                .withNewValueFrom()
                .withNewFieldRef()
                .withFieldPath(Constants.POD_IP_PATH)
                .endFieldRef()
                .endValueFrom()
                .build();
        envVars.add(podIP);
        EnvVar podName = new EnvVarBuilder()
                .withName(Constants.POD_NAME)
                .withNewValueFrom()
                .withNewFieldRef()
                .withFieldPath(Constants.POD_NAME_PATH)
                .endFieldRef()
                .endValueFrom()
                .build();
        envVars.add(podName);
        EnvVar podNamespace = new EnvVarBuilder()
                .withName(Constants.POD_NAMESPACE)
                .withNewValueFrom()
                .withNewFieldRef()
                .withFieldPath(Constants.POD_NAMESPACE_PATH)
                .endFieldRef()
                .endValueFrom()
                .build();
        envVars.add(podNamespace);

        Quantity masterCpu = spec.getMasterCpu();
        Quantity masterMemory = spec.getMasterMemory();
        VolumeMount configMount = this.getConfigMount();

        return new ContainerBuilder()
                .withName(name)
                .withImage(spec.getImage())
                .withImagePullPolicy(spec.getPullPolicy())
                .withEnv(spec.getEnvVars())
                .withEnvFrom(spec.getEnvFrom())
                .withWorkingDir(Constants.WORKING_DIR)
                .addToVolumeMounts(configMount)
                .addAllToCommand(command)
                .addAllToArgs(args)
                .addAllToPorts(ports)
                .withNewResources()
                    .addToLimits(ResourceName.CPU.value(), masterCpu)
                    .addToLimits(ResourceName.MEMORY.value(), masterMemory)
                .endResources()
                .build();
    }

    private VolumeMount getConfigMount() {
        return new VolumeMountBuilder()
                .withName(Constants.CONFIG_MAP_VOLUME)
                .withMountPath(Constants.CONFIG_FILE_PATH)
                .build();
    }

    private Volume getConfigVolume(String configMapName) {
        return new VolumeBuilder()
                .withName(Constants.CONFIG_MAP_VOLUME)
                .withNewConfigMap()
                    .withName(configMapName)
                .endConfigMap()
                .build();
    }

    public ObjectMeta getMetadata(HugeGraphComputerJob computerJob,
                                  String name) {
        String namespace = computerJob.getMetadata().getNamespace();
        String crName = computerJob.getMetadata().getName();

        OwnerReference ownerReference = new OwnerReferenceBuilder()
                .withName(crName)
                .withApiVersion(computerJob.getApiVersion())
                .withUid(computerJob.getMetadata().getUid())
                .withKind(computerJob.getKind())
                .withController(true)
                .withBlockOwnerDeletion(true)
                .build();

        return new ObjectMetaBuilder()
                .withNamespace(namespace)
                .withName(computerJob.getMetadata().getName())
                .addToLabels(this.geCommonLabels(crName, name))
                .withOwnerReferences(ownerReference)
                .build();
    }

    public Map<String, String> geCommonLabels(String crName,
                                              String componentName) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app", crName);
        labels.put("component", componentName);
        return labels;
    }
}
