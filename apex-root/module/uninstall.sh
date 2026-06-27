#!/system/bin/sh

# APEX-Root Uninstaller
# Complete cleanup of all module components

APEX_DIR=/data/adb/modules/apex-root

log -t APEX-Root "Uninstalling..."

# Unload eBPF program
if [ -f /sys/fs/bpf/apex_firewall ]; then
    rm -f /sys/fs/bpf/apex_firewall 2>/dev/null
    log -t APEX-Root "eBPF program removed"
fi

# Unmount tmpfs overlays
umount /data/adb/magisk 2>/dev/null
umount /data/adb/ksu 2>/dev/null
umount /system/bin/su 2>/dev/null
umount /system/xbin/su 2>/dev/null
umount /system/app/Magisk 2>/dev/null
umount /system/priv-app/Magisk 2>/dev/null

# Remove LD_PRELOAD init script
rm -f /data/local/tmp/ph_init.sh 2>/dev/null

# Clean up properties
setprop apex.fw.mode ""
setprop apex.fw.ns ""

# Clean up
rm -rf $APEX_DIR 2>/dev/null

log -t APEX-Root "Uninstall complete. Reboot recommended."
