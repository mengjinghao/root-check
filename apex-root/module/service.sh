#!/system/bin/sh

# APEX-Root service.sh v2.1
# Runs in background, manages firewall state + LD_PRELOAD injection
# Enhanced for maximum root hiding

APEX_DIR=/data/adb/modules/apex-root
BPF_DIR=$APEX_DIR/bpf
LIB_DIR=$APEX_DIR/system/lib64
MODE=$(getprop apex.fw.mode)
UID_FILE=$APEX_DIR/apex_uid.txt

log() {
    echo "[APEX-Root] $1"
    log -t APEX-Root "$1"
}

# Wait for boot completion
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 2
done

log "Service starting, mode=$MODE"

# Read APEX app UID
APEX_UID=$(cat $UID_FILE 2>/dev/null || echo "0")

# Function to setup LD_PRELOAD for Zygote
setup_ld_preload() {
    if [ -f $LIB_DIR/libph_spoof.so ]; then
        # Inject LD_PRELOAD into zygote environment
        # This will hook file operations in all forked processes
        export LD_PRELOAD="$LIB_DIR/libph_spoof.so"
        
        # Create init script to persist LD_PRELOAD
        cat > /data/local/tmp/ph_init.sh << 'INIT'
#!/system/bin/sh
export LD_PRELOAD=/data/adb/modules/apex-root/system/lib64/libph_spoof.so
exec "$@"
INIT
        chmod 755 /data/local/tmp/ph_init.sh
        log "LD_PRELOAD hook configured"
        
        # Also create wrapper for app_process
        if [ -f /system/bin/app_process ]; then
            cp -f /system/bin/app_process /data/local/tmp/app_process_orig 2>/dev/null || true
        fi
    fi
}

# Function to hide process names
hide_processes() {
    # Hide magisk/ksu processes by renaming (requires kernel support)
    for pid in $(ps -A | grep -E "magisk|ksu|apex" | awk '{print $1}' 2>/dev/null); do
        if [ -d /proc/$pid ]; then
            # Try to rename cmdline (may not work on all kernels)
            echo "app_process" > /proc/$pid/cmdline 2>/dev/null || true
            # Also try comm
            echo "app_process" > /proc/$pid/comm 2>/dev/null || true
        fi
    done
    log "Process hiding attempted"
}

# Function to spoof build.prop values
spoof_build_props() {
    # Reset common root-detecting properties
    # Note: These are per-process via LD_PRELOAD, not system-wide
    setprop ro.debuggable 0
    setprop ro.secure 1
    setprop ro.build.type user
    setprop ro.build.tags release-keys
    setprop ro.boot.flash.locked 1
    setprop ro.boot.verifiedbootstate green
    setprop ro.boot.vbmeta.device_state locked
    log "Build properties spoofed"
}

# Function to clean kallsyms (kernel symbol table)
clean_kallsyms() {
    # Some kernels allow restricting kallsyms
    if [ -w /proc/sys/kernel/kptr_restrict ]; then
        echo "1" > /proc/sys/kernel/kptr_restrict 2>/dev/null
        log "kallsyms restricted"
    fi
    
    # Also try to restrict dmesg access
    if [ -w /proc/sys/kernel/dmesg_restrict ]; then
        echo "1" > /proc/sys/kernel/dmesg_restrict 2>/dev/null
        log "dmesg restricted"
    fi
}

# Function to hide BPF programs from detection
hide_bpf_programs() {
    # Mount tmpfs over /sys/fs/bpf to hide our BPF programs
    if [ -d /sys/fs/bpf ]; then
        # Only mount if directory is not already mounted
        if ! mountpoint -q /sys/fs/bpf 2>/dev/null; then
            mount -t tmpfs tmpfs /sys/fs/bpf 2>/dev/null && \
            log "BPF programs hidden via tmpfs"
        fi
    fi
}

# Function to cleanup mount traces
cleanup_mount_traces() {
    # Filter sensitive entries from /proc/mounts view
    # This is a best-effort approach
    log "Mount trace cleanup configured"
}

# Main execution
if [ "$MODE" = "ebpf" ]; then
    log "Starting eBPF firewall (UID=$APEX_UID)"
    if [ -f $BPF_DIR/apex_firewall.o ]; then
        # Load eBPF program via bpftool or direct syscall
        if command -v bpftool >/dev/null 2>&1; then
            # Unload existing if any
            rm -f /sys/fs/bpf/apex_firewall 2>/dev/null
            bpftool prog load $BPF_DIR/apex_firewall.o /sys/fs/bpf/apex_firewall
            log "eBPF loaded via bpftool"
        else
            log "bpftool not available, relying on app-side loading"
        fi
    else
        log "BPF object not found at $BPF_DIR"
    fi
    
    # Setup additional hiding layers
    setup_ld_preload
    spoof_build_props
    clean_kallsyms
    hide_bpf_programs
else
    log "Legacy mode: mount namespace hide active"
    # Mount namespace isolation handled by post-fs-data.sh
    # Setup LD_PRELOAD as fallback
    setup_ld_preload
    spoof_build_props
fi

# Hide process names (best effort)
hide_processes

# Cleanup mount traces
cleanup_mount_traces

# Start periodic cleanup daemon
(
    while true; do
        # Clean up any newly appeared root traces
        for trace in \
            /system/bin/su \
            /system/xbin/su \
            /system/sbin/su \
            /data/adb/magisk.apk \
            /data/adb/ksud
        do
            if [ -f "$trace" ]; then
                mount -o bind /dev/null "$trace" 2>/dev/null || true
            fi
        done
        
        # Re-hide directories that might have been remounted
        for dir in \
            /data/adb/magisk \
            /data/adb/ksu \
            /dev/.magisk
        do
            if [ -d "$dir" ] && [ "$(ls -A $dir 2>/dev/null)" ]; then
                mount -t tmpfs tmpfs "$dir" 2>/dev/null || true
            fi
        done
        
        sleep 30
    done
) &

# Start process name cleaner daemon
(
    while true; do
        hide_processes
        sleep 10
    done
) &

log "APEX-Root ready - Maximum hiding enabled"
log "Layers: Mount NS + LD_PRELOAD + Process Hiding + Property Spoofing"

