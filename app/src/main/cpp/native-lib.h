#pragma once
#include <jni.h>

extern "C" {
    JNIEXPORT jintArray JNICALL
    Java_com_aashutarminal_terminal_TerminalNative_createSubprocess(
        JNIEnv *env, jobject thiz,
        jstring shellPath, jstring cwd, jobjectArray args, jobjectArray envVars);

    JNIEXPORT jint JNICALL
    Java_com_aashutarminal_terminal_TerminalNative_read(
        JNIEnv *env, jobject thiz, jint fd, jbyteArray buffer);

    JNIEXPORT jint JNICALL
    Java_com_aashutarminal_terminal_TerminalNative_write(
        JNIEnv *env, jobject thiz, jint fd, jbyteArray data);

    JNIEXPORT void JNICALL
    Java_com_aashutarminal_terminal_TerminalNative_resize(
        JNIEnv *env, jobject thiz, jint fd, jint cols, jint rows);

    JNIEXPORT void JNICALL
    Java_com_aashutarminal_terminal_TerminalNative_closeFd(
        JNIEnv *env, jobject thiz, jint fd);

    JNIEXPORT void JNICALL
    Java_com_aashutarminal_terminal_TerminalNative_killProcess(
        JNIEnv *env, jobject thiz, jint pid);
}
