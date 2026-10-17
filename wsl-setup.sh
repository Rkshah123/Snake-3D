#!/bin/bash
# One-time setup of the Linux (WSL Ubuntu) toolchain needed to build the
# Android APK with GluonFX. Run as root inside WSL:
#   wsl -d Ubuntu -u root -- bash /mnt/c/Users/ShahrukhKhan/Desktop/Snake3D/wsl-setup.sh
set -e
echo "=== [1/4] installing apt packages ==="
export DEBIAN_FRONTEND=noninteractive
apt-get update -qq
apt-get install -y -qq wget curl unzip ca-certificates build-essential fontconfig

echo "=== [2/4] installing GraalVM Community JDK 17 (includes native-image) ==="
if [ ! -d /opt/graalvm-community-openjdk-17.0.7+7.1 ]; then
  cd /opt
  wget -q https://github.com/graalvm/graalvm-ce-builds/releases/download/jdk-17.0.7/graalvm-community-jdk-17.0.7_linux-x64_bin.tar.gz -O graalvm.tar.gz
  tar -xzf graalvm.tar.gz
  rm graalvm.tar.gz
fi
GRAAL_VM_DIR=$(ls -d /opt/graalvm-community-openjdk-17.0.7* 2>/dev/null | head -n1)
echo "GraalVM at: $GRAAL_VM_DIR"

echo "=== [3/4] installing Android SDK (cmdline-tools, platform-tools, platform 33, build-tools, NDK 25) ==="
mkdir -p /opt/android-sdk/cmdline-tools
if [ ! -f /opt/android-sdk/cmdline-tools/latest/bin/sdkmanager ]; then
  wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip -O /tmp/cmdtools.zip
  unzip -q /tmp/cmdtools.zip -d /tmp/cmdtools
  mv /tmp/cmdtools/cmdline-tools /opt/android-sdk/cmdline-tools/latest
fi
export JAVA_HOME="$GRAAL_VM_DIR"
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH="$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools:$PATH"
yes | sdkmanager --sdk_root=$ANDROID_SDK_ROOT --licenses >/dev/null
sdkmanager --sdk_root=$ANDROID_SDK_ROOT "platform-tools" "platforms;android-33" "build-tools;33.0.2" "ndk;25.2.9519653"

echo "=== [4/4] writing env vars to /root/.bashrc ==="
cat >> /root/.bashrc <<EOF
export JAVA_HOME="$GRAAL_VM_DIR"
export GRAALVM_HOME="\$JAVA_HOME"
export ANDROID_SDK_ROOT=/opt/android-sdk
export PATH="\$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:\$ANDROID_SDK_ROOT/platform-tools:\$PATH"
EOF

echo "WSL setup complete."
