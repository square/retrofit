#!/bin/sh

if [ ! -d android-ndk ]
then
  echo Downloading Android NDK...
  curl http://dl.google.com/android/ndk/android-ndk-r5b-darwin-x86.tar.bz2 > /tmp/android-ndk.tgz
  echo Extracting Android NDK...
  tar zxf /tmp/android-ndk.tgz -C /tmp
  mv /tmp/android-ndk-r5b ./android-ndk
fi

./android-ndk/ndk-build

