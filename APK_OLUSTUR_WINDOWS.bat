@echo off
chcp 65001 >nul
echo ====================================================================
echo   ULAK - BYD Seal U DM-i (2025) DiLink 3.0 ve S26 Ultra APK Derleyici
echo ====================================================================
echo.
if exist gradlew.bat (
    call gradlew.bat assembleDebug
) else (
    where gradle >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        call gradle assembleDebug
    ) else (
        echo [!] HATA: 'gradlew.bat' veya 'gradle' bulunamadi.
        echo Lutfen bu klasoru Android Studio ile acin veya GitHub Actions ile bulutta derleyin.
        pause
        exit /b 1
    )
)
echo.
echo [OK] APK dosyalari basariyla olusturuldu:
echo  1) Araç APK (DiLink 3.0): app-car\build\outputs\apk\debug\UlakCar_DiLink3_v2.4.0-DiLink3-debug.apk
echo  2) Mobil APK (S26 Ultra): app-mobile\build\outputs\apk\debug\UlakMobile_S26Ultra_v2.4.0-S26Ultra-debug.apk
pause
