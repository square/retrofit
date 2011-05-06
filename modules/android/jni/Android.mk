LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE    := retrofit
LOCAL_SRC_FILES := retrofit.c

include $(BUILD_SHARED_LIBRARY)