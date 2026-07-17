// Aashu Tarminal — shared native init (JNI_OnLoad, version reporting).
#include <jni.h>
#include <android/log.h>

#define LOG_TAG "AashuTerminal"

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void * /*reserved*/) {
    __android_log_print(ANDROID_LOG_INFO, LOG_TAG, "aashuterminal native lib loaded");
    return JNI_VERSION_1_6;
}
