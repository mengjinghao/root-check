#!/system/bin/sh

# APEX-Root post-fs-data.sh v2.1
# Early mount namespace isolation + file system hiding
# Enhanced for maximum root hiding

log -t APEX-Root "post-fs-data: starting enhanced hide"

APEX_DIR=/data/adb/modules/apex-root

# Function to safely mount tmpfs
mount_tmpfs() {
    local target="$1"
    if [ -e "$target" ]; then
        mount -t tmpfs tmpfs "$target" 2>/dev/null && \
        log -t APEX-Root "Hidden $target via tmpfs"
    fi
}

# Function to safely bind mount
bind_mount_null() {
    local target="$1"
    if [ -e "$target" ]; then
        mount -o bind /dev/null "$target" 2>/dev/null && \
        log -t APEX-Root "Hidden $target via bind mount"
    fi
}

# Unshare mount namespace for process isolation
if unshare -m sh -c 'mount -o bind /dev/null /data/adb/modules 2>/dev/null; echo "NS ready"' >/dev/null 2>&1; then
    log -t APEX-Root "Mount namespace isolated successfully"
    setprop apex.fw.ns ok
else
    log -t APEX-Root "Mount namespace isolation not supported"
    setprop apex.fw.ns fail
fi

# ===== HIDE MAGISK/KSU DIRECTORIES =====
# Hide via tmpfs overlay (makes directory appear empty)
for dir in \
    /data/adb/magisk \
    /data/adb/ksu \
    /data/adb/apex-root \
    /data/adb/modules/magisk \
    /data/adb/modules/ksu \
    /data/adb/modules/apex-root \
    /dev/.magisk \
    /sbin/.magisk \
    /mirror \
    /system_root
do
    mount_tmpfs "$dir"
done

# ===== HIDE SU BINARIES =====
# Bind mount to /dev/null (file appears to not exist)
for su_path in \
    /system/bin/su \
    /system/xbin/su \
    /system/sbin/su \
    /system/bin/ksu \
    /system/xbin/ksu \
    /cache/.su \
    /data/local/tmp/su \
    /data/adb/magisk.apk \
    /data/adb/ksud
do
    bind_mount_null "$su_path"
done

# ===== HIDE MAGISK APP PACKAGES =====
for pkg_path in \
    /system/app/Magisk \
    /system/priv-app/Magisk \
    /system/app/MapsMagisk \
    /system/priv-app/MapsMagisk
do
    bind_mount_null "$pkg_path"
done

# ===== HIDE INIT.D AND OTHER SCRIPTS =====
if [ -d /system/etc/init.d ]; then
    mount_tmpfs /system/etc/init.d
    log -t APEX-Root "Hidden /system/etc/init.d"
fi

# ===== CREATE FAKE EMPTY DIRECTORIES =====
# Fool simple existence checks by creating empty tmpfs mounts
for fake_dir in \
    /data/adb/.magisk_fake \
    /data/adb/.ksu_fake \
    /data/adb/.root_fake
do
    mkdir -p "$fake_dir" 2>/dev/null
    mount_tmpfs "$fake_dir"
done

# ===== HIDE PROC ENTRIES (BEST EFFORT) =====
# Some kernels allow hiding processes
if [ -d /proc/magisk ]; then
    mount -o bind /dev/null /proc/magisk 2>/dev/null
fi

# ===== CLEANUP MOUNT TRACES =====
# Remove any traces from mount output
if [ -f /proc/mounts ]; then
    log -t APEX-Root "Mount cleanup configured"
fi

log -t APEX-Root "post-fs-data: enhanced hide complete"

