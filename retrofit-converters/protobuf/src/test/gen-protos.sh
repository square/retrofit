#!/bin/bash

set -ex
protoc --java_out=java/ protos/phone.proto
