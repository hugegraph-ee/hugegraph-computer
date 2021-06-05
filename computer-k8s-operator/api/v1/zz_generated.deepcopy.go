// +build !ignore_autogenerated

/*
Copyright 2017 HugeGraph Authors

Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements. See the NOTICE file distributed with this
work for additional information regarding copyright ownership. The ASF
licenses this file to You under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
License for the specific language governing permissions and limitations
under the License.
*/

// Code generated by controller-gen. DO NOT EDIT.

package v1

import (
	corev1 "k8s.io/api/core/v1"
	runtime "k8s.io/apimachinery/pkg/runtime"
)

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *ComputerJobSpec) DeepCopyInto(out *ComputerJobSpec) {
	*out = *in
	if in.AlgorithmName != nil {
		in, out := &in.AlgorithmName, &out.AlgorithmName
		*out = new(string)
		**out = **in
	}
	if in.JobId != nil {
		in, out := &in.JobId, &out.JobId
		*out = new(string)
		**out = **in
	}
	if in.Image != nil {
		in, out := &in.Image, &out.Image
		*out = new(string)
		**out = **in
	}
	if in.WorkerInstances != nil {
		in, out := &in.WorkerInstances, &out.WorkerInstances
		*out = new(int32)
		**out = **in
	}
	if in.MasterCpu != nil {
		in, out := &in.MasterCpu, &out.MasterCpu
		*out = new(string)
		**out = **in
	}
	if in.WorkerCpu != nil {
		in, out := &in.WorkerCpu, &out.WorkerCpu
		*out = new(int32)
		**out = **in
	}
	if in.MasterMemory != nil {
		in, out := &in.MasterMemory, &out.MasterMemory
		*out = new(string)
		**out = **in
	}
	if in.WorkerMemory != nil {
		in, out := &in.WorkerMemory, &out.WorkerMemory
		*out = new(string)
		**out = **in
	}
	if in.ComputerConf != nil {
		in, out := &in.ComputerConf, &out.ComputerConf
		*out = make(map[string]string, len(*in))
		for key, val := range *in {
			(*out)[key] = val
		}
	}
	if in.ConfigMap != nil {
		in, out := &in.ConfigMap, &out.ConfigMap
		*out = new(string)
		**out = **in
	}
	if in.EnvVars != nil {
		in, out := &in.EnvVars, &out.EnvVars
		*out = make([]corev1.EnvVar, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
	if in.EnvFrom != nil {
		in, out := &in.EnvFrom, &out.EnvFrom
		*out = make([]corev1.EnvFromSource, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new ComputerJobSpec.
func (in *ComputerJobSpec) DeepCopy() *ComputerJobSpec {
	if in == nil {
		return nil
	}
	out := new(ComputerJobSpec)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *ComputerJobState) DeepCopyInto(out *ComputerJobState) {
	*out = *in
	if in.Superstep != nil {
		in, out := &in.Superstep, &out.Superstep
		*out = new(int32)
		**out = **in
	}
	if in.MaxSuperstep != nil {
		in, out := &in.MaxSuperstep, &out.MaxSuperstep
		*out = new(int32)
		**out = **in
	}
	if in.LastSuperstepStat != nil {
		in, out := &in.LastSuperstepStat, &out.LastSuperstepStat
		*out = new(string)
		**out = **in
	}
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new ComputerJobState.
func (in *ComputerJobState) DeepCopy() *ComputerJobState {
	if in == nil {
		return nil
	}
	out := new(ComputerJobState)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *ComputerJobStatus) DeepCopyInto(out *ComputerJobStatus) {
	*out = *in
	if in.JobStatus != nil {
		in, out := &in.JobStatus, &out.JobStatus
		*out = new(string)
		**out = **in
	}
	if in.JobState != nil {
		in, out := &in.JobState, &out.JobState
		*out = new(ComputerJobState)
		(*in).DeepCopyInto(*out)
	}
	in.LastUpdateTime.DeepCopyInto(&out.LastUpdateTime)
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new ComputerJobStatus.
func (in *ComputerJobStatus) DeepCopy() *ComputerJobStatus {
	if in == nil {
		return nil
	}
	out := new(ComputerJobStatus)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *HugeGraphComputerJob) DeepCopyInto(out *HugeGraphComputerJob) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ObjectMeta.DeepCopyInto(&out.ObjectMeta)
	in.Spec.DeepCopyInto(&out.Spec)
	in.Status.DeepCopyInto(&out.Status)
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new HugeGraphComputerJob.
func (in *HugeGraphComputerJob) DeepCopy() *HugeGraphComputerJob {
	if in == nil {
		return nil
	}
	out := new(HugeGraphComputerJob)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *HugeGraphComputerJob) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *HugeGraphComputerJobList) DeepCopyInto(out *HugeGraphComputerJobList) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ListMeta.DeepCopyInto(&out.ListMeta)
	if in.Items != nil {
		in, out := &in.Items, &out.Items
		*out = make([]HugeGraphComputerJob, len(*in))
		for i := range *in {
			(*in)[i].DeepCopyInto(&(*out)[i])
		}
	}
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new HugeGraphComputerJobList.
func (in *HugeGraphComputerJobList) DeepCopy() *HugeGraphComputerJobList {
	if in == nil {
		return nil
	}
	out := new(HugeGraphComputerJobList)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyObject is an autogenerated deepcopy function, copying the receiver, creating a new runtime.Object.
func (in *HugeGraphComputerJobList) DeepCopyObject() runtime.Object {
	if c := in.DeepCopy(); c != nil {
		return c
	}
	return nil
}
