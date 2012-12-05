LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)

LOCAL_MODULE := onirec
LOCAL_SRC_FILES := org_pointclouds_onirec_Application.cpp \
                   org_pointclouds_onirec_CaptureThreadManager.cpp \
                   org_pointclouds_onirec_grab_NativeBuffer.cpp \
                   org_pointclouds_onirec_PclzfWriter.cpp
LOCAL_LDLIBS := -ljnigraphics -llog
LOCAL_STATIC_LIBRARIES := lzf

include $(BUILD_SHARED_LIBRARY)
