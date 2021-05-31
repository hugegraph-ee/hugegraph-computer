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
	runtime "k8s.io/apimachinery/pkg/runtime"
)

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *HugeGraphComputerJob) DeepCopyInto(out *HugeGraphComputerJob) {
	*out = *in
	out.TypeMeta = in.TypeMeta
	in.ObjectMeta.DeepCopyInto(&out.ObjectMeta)
	out.Spec = in.Spec
	out.Status = in.Status
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

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *HugeGraphComputerJobSpec) DeepCopyInto(out *HugeGraphComputerJobSpec) {
	*out = *in
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new HugeGraphComputerJobSpec.
func (in *HugeGraphComputerJobSpec) DeepCopy() *HugeGraphComputerJobSpec {
	if in == nil {
		return nil
	}
	out := new(HugeGraphComputerJobSpec)
	in.DeepCopyInto(out)
	return out
}

// DeepCopyInto is an autogenerated deepcopy function, copying the receiver, writing into out. in must be non-nil.
func (in *HugeGraphComputerJobStatus) DeepCopyInto(out *HugeGraphComputerJobStatus) {
	*out = *in
}

// DeepCopy is an autogenerated deepcopy function, copying the receiver, creating a new HugeGraphComputerJobStatus.
func (in *HugeGraphComputerJobStatus) DeepCopy() *HugeGraphComputerJobStatus {
	if in == nil {
		return nil
	}
	out := new(HugeGraphComputerJobStatus)
	in.DeepCopyInto(out)
	return out
}
