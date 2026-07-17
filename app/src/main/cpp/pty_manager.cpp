// Aashu Tarminal — original PTY handling implementation.
// Opens /dev/ptmx, forks, execve's the target shell in the child with the
// slave PTY wired to stdin/stdout/stderr, and returns the master fd + pid
// to the JVM side.

#include "native-lib.h"
#include <android/log.h>
#include <fcntl.h>
#include <pty.h>
#include <string.h>
#include <sys/ioctl.h>
#include <sys/wait.h>
#include <termios.h>
#include <unistd.h>
#include <vector>

#define LOG_TAG "AashuPty"
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

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

        execve(shell, cArgs.data(), cEnv.data());
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
