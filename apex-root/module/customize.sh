#!/system/bin/sh

# APEX-Root Module Installer
# Supports: Magisk, KernelSU, APatch
# Enhanced for maximum root hiding

APEX_DIR=/data/adb/modules/apex-root
UID_FILE=$APEX_DIR/apex_uid.txt
BASELINE_DIR=$APEX_DIR/baseline
LIB_DIR=$APEX_DIR/system/lib64
BPF_DIR=$APEX_DIR/bpf

echo "[APEX-Root] Installing PhantomHide v2.0..."

# Create directories
mkdir -p $BASELINE_DIR
mkdir -p $LIB_DIR
mkdir -p $BPF_DIR
mkdir -p $APEX_DIR/zygisk

# Set APEX app UID (populated by app on first run)
echo "0" > $UID_FILE

# Determine API level and kernel support for optimal path
API=$(getprop ro.build.version.sdk)
KVER=$(uname -r | cut -d. -f1,2)
ARCH=$(getprop ro.product.cpu.abi)

echo "[APEX-Root] Android API: $API, Kernel: $KVER, Arch: $ARCH"

# Install eBPF program if kernel >= 5.10 (Android 12+)
BPF_VER_MAJ=$(echo $KVER | cut -d. -f1)
BPF_VER_MIN=$(echo $KVER | cut -d. -f2)
if [ $BPF_VER_MAJ -ge 5 ] && [ $BPF_VER_MIN -ge 10 ]; then
    echo "[APEX-Root] Kernel supports eBPF, installing BPF firewall"
    cp -f $MODPATH/bpf/*.o $BPF_DIR/ 2>/dev/null || true
    setprop apex.fw.mode ebpf
else
    echo "[APEX-Root] Kernel too old for eBPF, using legacy path"
    setprop apex.fw.mode legacy
fi

# Copy LD_PRELOAD spoof library
if [ -f $MODPATH/system/lib64/libph_spoof.so ]; then
    cp -f $MODPATH/system/lib64/libph_spoof.so $LIB_DIR/
    echo "[APEX-Root] LD_PRELOAD hook library installed"
fi

# Copy Zygisk module if exists
if [ -d $MODPATH/zygisk ]; then
    cp -rf $MODPATH/zygisk/* $APEX_DIR/zygisk/ 2>/dev/null || true
    echo "[APEX-Root] Zygisk module installed"
fi

# SELinux policy - load or copy
if [ -f $MODPATH/sepolicy.rule ]; then
    cp -f $MODPATH/sepolicy.rule $APEX_DIR/
    echo "[APEX-Root] SELinux policy installed"
fi

# Create hide list configuration
cat > $APEX_DIR/hide_list.conf << 'EOF'
# Files to hide from detection apps
/data/adb/magisk
/data/adb/ksu
/data/adb/apex-root
/su
/system/bin/su
/system/xbin/su
/cache/.su
/data/local/tmp/su
/system/app/Magisk
/system/priv-app/Magisk
/data/app/com.topjohnwu.magisk*
/system/etc/init.d
/system/xbin/ksu
/data/adb/ksud
/system/bin/ksu
EOF

# Create process hide list
cat > $APEX_DIR/process_hide.conf << 'EOF'
magiskd
magisk
ksud
ksu
apex
EOF

# Set permissions
set_perm_recursive $MODPATH 0 0 0755 0644
set_perm_recursive $APEX_DIR 0 0 0755 0644
set_perm $MODPATH/service.sh 0 0 0755
set_perm $MODPATH/post-fs-data.sh 0 0 0755
set_perm $LIB_DIR/libph_spoof.so 0 0 0755 2>/dev/null || true

# Enable systemless hosts
touch $APEX_DIR/system_hosts

echo "[APEX-Root] Installation complete. Reboot recommended."
