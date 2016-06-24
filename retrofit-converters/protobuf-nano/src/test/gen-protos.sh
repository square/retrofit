#!/bin/bash

set -ex
protoc --javanano_out=java/ protos/phone.proto