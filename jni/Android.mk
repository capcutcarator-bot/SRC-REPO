LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE    := dobby
LOCAL_SRC_FILES := prebuilt/arm64-v8a/libdobby.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE           := SHUVOxMODS
LOCAL_SRC_FILES        := mod_menu.cpp
LOCAL_C_INCLUDES       := $(LOCAL_PATH)
LOCAL_LDLIBS           := -llog -landroid
LOCAL_STATIC_LIBRARIES := dobby
LOCAL_CPPFLAGS         := -std=c++17 -O2 -fvisibility=hidden -DANDROID -fno-exceptions -fno-rtti
include $(BUILD_SHARED_LIBRARY)
