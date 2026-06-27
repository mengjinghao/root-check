#!/bin/bash
# APEX-Root LD_PRELOAD Library Build Script
# Builds libph_spoof.so for Android ARM64

set -e

echo "=== APEX-Root LD_PRELOAD Builder ==="

# Configuration
NDK_PATH=${ANDROID_NDK_ROOT:-"$HOME/android-ndk-r28"}
API_LEVEL=29
ARCH="aarch64"
OUTPUT_DIR="$(dirname "$0")"

# Check NDK
if [ ! -d "$NDK_PATH" ]; then
    echo "Error: Android NDK not found at $NDK_PATH"
    echo "Please set ANDROID_NDK_ROOT or install NDK r28+"
    exit 1
fi

# Toolchain setup
TOOLCHAIN="$NDK_PATH/toolchains/llvm/prebuilt/linux-x86_64"
CC="$TOOLCHAIN/bin/$ARCH-linux-android$API_LEVEL-clang++"

if [ ! -f "$CC" ]; then
    echo "Error: Compiler not found at $CC"
    exit 1
fi

echo "Using NDK: $NDK_PATH"
echo "Compiler: $CC"

# Build
echo "Building libph_spoof.so..."
$CC \
    -shared \
    -fPIC \
    -O2 \
    -ffunction-sections \
    -fdata-sections \
    -Wl,--gc-sections \
    -static-libstdc++ \
    -o "$OUTPUT_DIR/libph_spoof.so" \
    "$OUTPUT_DIR/ph_spoof.cpp"

# Verify
if [ -f "$OUTPUT_DIR/libph_spoof.so" ]; then
    SIZE=$(stat -c%s "$OUTPUT_DIR/libph_spoof.so")
    echo "✓ Build successful: libph_spoof.so ($SIZE bytes)"
    
    # Show symbols
    echo ""
    echo "Exported hooks:"
    nm -D "$OUTPUT_DIR/libph_spoof.so" | grep " T " | awk '{print "  - " $3}' | head -20
else
    echo "✗ Build failed"
    exit 1
fi

echo ""
echo "=== Build Complete ==="
echo "Copy libph_spoof.so to module/system/lib64/"
