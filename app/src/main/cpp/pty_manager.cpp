// Aashu Tarminal — original PTY handling implementation.
// Opens /dev/ptmx, forks, execve's the target shell in the child with the
// slave PTY wired to stdin/stdout/stderr, and returns the master fd + pid
// to the JVM side.

#include "native-lib.h"
#include <android/log.h>
#include <errno.h>
#include <fcntl.h>
#include <pty.h>
#include <stdio.h>
#include <string>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/syscall.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>
#include <vector>

#define LOG_TAG "AashuPty"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// The memfd_create() libc wrapper only exists from API 30 onward on
// Android, but the underlying kernel syscall has existed for far longer.
// Call it directly via syscall() so this works on our minSdk (24) too.
#ifndef __NR_memfd_create
  #if defined(__aarch64__)
    #define __NR_memfd_create 279
  #elif defined(__arm__)
    #define __NR_memfd_create 385
  #elif defined(__x86_64__)
    #define __NR_memfd_create 319
  #elif defined(__i386__)
    #define __NR_memfd_create 356
  #endif
#endif

static int aashu_memfd_create(const char *name) {
#ifdef __NR_memfd_create
    return (int) syscall(__NR_memfd_create, name, 0);
#else
    (void) name;
    return -1;
#endif
}

static std::vector<std::string> jarrayToVec(JNIEnv *env, jobjectArray arr) {
    std::vector<std::string> out;
    if (!arr) return out;
    jsize len = env->GetArrayLength(arr);
    for (jsize i = 0; i < len; i++) {
        auto jstr = (jstring) env->GetObjectArrayElement(arr, i);
        const char *chars = env->GetStringUTFChars(jstr, nullptr);
        out.emplace_back(chars);
        env->ReleaseStringUTFChars(jstr, chars);
        env->DeleteLocalRef(jstr);
    }
    return out;
}

extern "C" JNIEXPORT jintArray JNICALL
Java_com_aashutarminal_terminal_TerminalNative_createSubprocess(
    JNIEnv *env, jobject /*thiz*/,
    jstring shellPath, jstring cwd, jobjectArray argsArr, jobjectArray envArr) {

    const char *shell = env->GetStringUTFChars(shellPath, nullptr);
    const char *dir = env->GetStringUTFChars(cwd, nullptr);
    auto args = jarrayToVec(env, argsArr);
    auto envVars = jarrayToVec(env, envArr);

    int masterFd;
    struct winsize ws{};
    ws.ws_col = 80;
    ws.ws_row = 24;

    pid_t pid = forkpty(&masterFd, nullptr, nullptr, &ws);
    if (pid == 0) {
        // Child process
        chdir(dir);

        std::vector<char *> cArgs;
        cArgs.push_back(const_cast<char *>(shell));
        for (auto &a : args) cArgs.push_back(const_cast<char *>(a.c_str()));
        cArgs.push_back(nullptr);

        std::vector<char *> cEnv;
        for (auto &e : envVars) cEnv.push_back(const_cast<char *>(e.c_str()));
        cEnv.push_back(nullptr);

        // Android 10+ blocks executing binaries directly from app-private
        // storage (W^X hardening) -- the same problem Termux's own
        // "termux-exec" solves. Work around it by copying the binary into
        // an anonymous, kernel-backed memfd and exec'ing that via its
        // /proc/self/fd/N path, which isn't subject to the noexec mount
        // restriction. Falls back to a plain execve() for binaries that
        // live somewhere already executable (e.g. /system/bin/sh).
        int srcFd = open(shell, O_RDONLY);
        if (srcFd >= 0) {
            int mfd = aashu_memfd_create("aashu-exe");
            if (mfd >= 0) {
                char buf[65536];
                ssize_t n;
                while ((n = read(srcFd, buf, sizeof(buf))) > 0) {
                    ssize_t off = 0;
                    while (off < n) {
                        ssize_t w = write(mfd, buf + off, n - off);
                        if (w <= 0) break;
                        off += w;
                    }
                }
                close(srcFd);
                char procPath[64];
                snprintf(procPath, sizeof(procPath), "/proc/self/fd/%d", mfd);
                execve(procPath, cArgs.data(), cEnv.data());
                // If we reach here, the memfd exec failed -- report why,
                // directly to the pty (stderr is already wired to it by
                // forkpty), then fall through to the plain execve attempt.
                dprintf(STDERR_FILENO, "[memfd exec failed: %s]\r\n", strerror(errno));
                close(mfd);
            } else {
                dprintf(STDERR_FILENO, "[memfd_create failed: %s]\r\n", strerror(errno));
                close(srcFd);
            }
        } else {
            dprintf(STDERR_FILENO, "[open('%s') failed: %s]\r\n", shell, strerror(errno));
        }

        execve(shell, cArgs.data(), cEnv.data());
        dprintf(STDERR_FILENO, "[plain execve failed: %s]\r\n", strerror(errno));
        _exit(127); // execve failed
    }

    env->ReleaseStringUTFChars(shellPath, shell);
    env->ReleaseStringUTFChars(cwd, dir);

    if (pid < 0) {
        LOGE("forkpty failed: %s", strerror(errno));
    }

    jintArray result = env->NewIntArray(2);
    jint vals[2] = {masterFd, (jint) pid};
    env->SetIntArrayRegion(result, 0, 2, vals);
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_aashutarminal_terminal_TerminalNative_read(
    JNIEnv *env, jobject /*thiz*/, jint fd, jbyteArray buffer) {
    jsize len = env->GetArrayLength(buffer);
    std::vector<char> tmp(len);
    ssize_t n = ::read(fd, tmp.data(), len);
    if (n > 0) env->SetByteArrayRegion(buffer, 0, n, reinterpret_cast<jbyte *>(tmp.data()));
    return (jint) n;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_aashutarminal_terminal_TerminalNative_write(
    JNIEnv *env, jobject /*thiz*/, jint fd, jbyteArray data) {
    jsize len = env->GetArrayLength(data);
    std::vector<char> tmp(len);
    env->GetByteArrayRegion(data, 0, len, reinterpret_cast<jbyte *>(tmp.data()));
    return (jint) ::write(fd, tmp.data(), len);
}

extern "C" JNIEXPORT void JNICALL
Java_com_aashutarminal_terminal_TerminalNative_resize(
    JNIEnv * /*env*/, jobject /*thiz*/, jint fd, jint cols, jint rows) {
    struct winsize ws{};
    ws.ws_col = (unsigned short) cols;
    ws.ws_row = (unsigned short) rows;
    ioctl(fd, TIOCSWINSZ, &ws);
}

extern "C" JNIEXPORT void JNICALL
Java_com_aashutarminal_terminal_TerminalNative_closeFd(
    JNIEnv * /*env*/, jobject /*thiz*/, jint fd) {
    ::close(fd);
}

extern "C" JNIEXPORT void JNICALL
Java_com_aashutarminal_terminal_TerminalNative_killProcess(
    JNIEnv * /*env*/, jobject /*thiz*/, jint pid) {
    kill(pid, SIGKILL);
    int status;
    waitpid(pid, &status, WNOHANG);
}
