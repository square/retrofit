#!/bin/sh

if [ ! -d android-ndk ]
then
  echo Extracting Android NDK...
  tar zxf android-ndk.tgz  
fi

./android-ndk/ndk-build

