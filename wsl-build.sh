#!/bin/bash
# Builds the Android APK inside WSL and copies it to the project's dist/ folder.
#   wsl -d Ubuntu -u root -- bash /mnt/c/Users/ShahrukhKhan/Desktop/Snake3D/wsl-build.sh
set -e
GRAAL_VM_DIR=$(ls -d /opt/graalvm-community-openjdk-17.0.7* 2>/dev/null | head -n1)
export JAVA_HOME="$GRAAL_VM_DIR"
export GRAALVM_HOME="$GRAAL_VM_DIR"
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
cd /mnt/c/Users/ShahrukhKhan/Desktop/Snake3D

echo "JAVA_HOME=$JAVA_HOME"
echo "GRAALVM_HOME=$GRAALVM_HOME"
echo "ANDROID_SDK_ROOT=$ANDROID_SDK_ROOT"

echo "=== gluonfx:build (native image) — this takes a while ==="
mvn -Pandroid gluonfx:build

echo "=== gluonfx:package (APK packaging) ==="
mvn -Pandroid gluonfx:package

echo "=== copying APK to dist/ ==="
mkdir -p dist
find target/gluonfx -name '*.apk' -exec cp -v {} dist/ \;
ls -la dist/
echo "APK build complete."
