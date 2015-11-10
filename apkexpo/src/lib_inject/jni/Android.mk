LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := inject2
LOCAL_SRC_FILES := inject2.cpp ../../main/jni/expo.cpp 

ifeq ($(TARGET_ARCH), arm)
LOCAL_SRC_FILES += shellcode.s
endif

LOCAL_C_INCLUDES := $(LOCAL_PATH)
LOCAL_CPPFLAGS := -fpermissive -w -g -fexceptions -std=gnu++11 -O3
LOCAL_LDLIBS := -ldl -llog

LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_MODULE_TAGS := debug

include $(BUILD_EXECUTABLE)
