// Aashu Tarminal — exec redirect shim (original implementation).
//
// Android 10+ blocks executing any file from an app's own writable data
// directory. The only place the OS grants execute permission is the
// app's native-library directory (jniLibs), which is where
// repackageBootstrapExecs (see app/build.gradle) copies every bootstrap
// binary under a sanitized libtx_*.so name, alongside a JSON manifest
// mapping "bin/bash" -> "libtx_bin_bash.so" etc.
//
// This shared library is loaded via LD_PRELOAD into the shell process (and
// therefore every process it forks, since LD_PRELOAD is inherited through
// the environment). It intercepts execve() -- the primitive bash and
// busybox ultimately call for external commands -- and rewrites the
// requested path to its jniLibs equivalent before handing off to the real
// execve(), so every command typed in the terminal gets the same
// noexec workaround as the initial shell launch.
//
// Configuration is passed via two environment variables set by
// BootstrapManager.kt when the top-level shell is spawned:
//   AASHU_NATIVE_LIB_DIR - the app's real native library directory
//   AASHU_EXEC_MAP       - filesystem path to the JSON manifest for this ABI

#define _GNU_SOURCE
#include <android/log.h>
#include <dlfcn.h>
#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>

#define LOG_TAG "AashuExecShim"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

typedef int (*execve_t)(const char *, char *const[], char *const[]);

static execve_t real_execve = NULL;
static char native_lib_dir[512] = {0};
static int map_loaded = 0;

// Tiny hand-rolled "key\tvalue\n" lookup table, parsed once from the JSON
// manifest at first use (avoids pulling in a JSON library for ~a few
// hundred short lines). See exec_map_to_tsv() on the Kotlin/build side --
// the manifest is pre-flattened to TSV by BootstrapManager before this
// shim ever runs, so this stays a dumb linear scan over a small file.
#define MAX_MAP_ENTRIES 4096
static char *map_keys[MAX_MAP_ENTRIES];
static char *map_vals[MAX_MAP_ENTRIES];
static int map_count = 0;

static void load_map_once() {
    if (map_loaded) return;
    map_loaded = 1;

    const char *dir = getenv("AASHU_NATIVE_LIB_DIR");
    if (dir) strncpy(native_lib_dir, dir, sizeof(native_lib_dir) - 1);

    const char *mapPath = getenv("AASHU_EXEC_MAP_TSV");
    if (!mapPath) return;

    FILE *f = fopen(mapPath, "r");
    if (!f) return;

    char line[1024];
    while (fgets(line, sizeof(line), f) && map_count < MAX_MAP_ENTRIES) {
        char *tab = strchr(line, '\t');
        if (!tab) continue;
        *tab = '\0';
        char *value = tab + 1;
        char *nl = strchr(value, '\n');
        if (nl) *nl = '\0';

        map_keys[map_count] = strdup(line);
        map_vals[map_count] = strdup(value);
        map_count++;
    }
    fclose(f);
}

// Given an absolute path like ".../usr/bin/ls", find "bin/ls" (the part
// the manifest keys are relative to, matching the bootstrap zip layout)
// and look it up.
static const char *find_redirect(const char *path) {
    if (map_count == 0 || native_lib_dir[0] == '\0') return NULL;

    const char *rel = strstr(path, "/bin/");
    if (!rel) rel = strstr(path, "/lib/");
    if (!rel) return NULL;
    rel++; // skip leading '/'

    for (int i = 0; i < map_count; i++) {
        if (strcmp(map_keys[i], rel) == 0) {
            return map_vals[i];
        }
    }
    return NULL;
}

int execve(const char *pathname, char *const argv[], char *const envp[]) {
    if (!real_execve) {
        real_execve = (execve_t) dlsym(RTLD_NEXT, "execve");
    }
    load_map_once();

    const char *soName = find_redirect(pathname);
    if (soName) {
        char realPath[768];
        snprintf(realPath, sizeof(realPath), "%s/%s", native_lib_dir, soName);
        LOGD("redirect exec: %s -> %s", pathname, realPath);
        return real_execve(realPath, argv, envp);
    }

    // Not one of our bootstrap binaries (e.g. a path outside $PREFIX) --
    // let it go through unchanged; it'll fail on its own if it can't run.
    return real_execve(pathname, argv, envp);
}
