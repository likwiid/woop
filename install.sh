#!/usr/bin/env bash
# Builds the debug APK and installs it on a connected Android device or emulator.
#   ./install.sh            build + install on the only connected device
#   ./install.sh -s SERIAL  pick a device when several are connected (see `adb devices`)
set -euo pipefail
cd "$(dirname "$0")"

./gradlew assembleDebug
APK=app/build/outputs/apk/debug/app-debug.apk

adb ${1+"$@"} install -r "$APK"
adb ${1+"$@"} shell am start -n com.likwiid.woop/.MainActivity >/dev/null
echo "Installed and launched WOOP."
