#!/usr/bin/env bash
set -e
echo "===================================================================="
echo "  ULAK - BYD Seal U DM-i (2025) DiLink 3.0 ve S26 Ultra APK Derleyici"
echo "===================================================================="
if [ -f "./gradlew" ]; then
    chmod +x ./gradlew
    ./gradlew assembleDebug
elif command -v gradle >/dev/null 2>&1; then
    gradle assembleDebug
else
    echo "[!] HATA: gradle bulunamadı. Lütfen projeyi Android Studio ile açın veya GitHub Actions kullanın."
    exit 1
fi
echo ""
echo "[OK] APK dosyaları hazır:"
echo " 1) Araç APK: app-car/build/outputs/apk/debug/UlakCar_DiLink3_v2.4.0-DiLink3-debug.apk"
echo " 2) Mobil APK: app-mobile/build/outputs/apk/debug/UlakMobile_S26Ultra_v2.4.0-S26Ultra-debug.apk"
