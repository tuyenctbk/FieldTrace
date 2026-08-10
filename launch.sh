#!/bin/bash
export ANDROID_HOME=/Users/user/Library/Android/sdk
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export PATH=$ANDROID_HOME/platform-tools:$ANDROID_HOME/emulator:$PATH

echo "=== Building Debug APK ==="
/Users/user/.gradle/wrapper/dists/gradle-9.4.1-bin/arn2x92ynaizyzdaamcbpbhtj/gradle-9.4.1/bin/gradle assembleDebug --no-daemon --no-configuration-cache || exit 1

echo "=== Connected Devices ==="
adb devices

echo "=== Installing APK to Emulator ==="
adb install -r app/build/outputs/apk/debug/app-debug.apk || exit 1

echo "=== Launching App ==="
adb shell am start -n com.soloprono.fieldtrace/com.example.MainActivity
