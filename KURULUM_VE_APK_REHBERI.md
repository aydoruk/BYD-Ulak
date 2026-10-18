# ULAK — BYD Seal U DM-i (2025 • DiLink 3.0) & Samsung Galaxy S26 Ultra Kurulum Rehberi

Bu proje paketi iki adet yerel (native) Android uygulamasını barındırır:
1. **`UlakCar_DiLink3_v2.4.0-DiLink3-debug.apk` (`:app-car`)**: BYD Seal U DM-i (2025) DiLink 3.0 (Android 10 / API 29) multimedya ekranı için geliştirilmiştir.
2. **`UlakMobile_S26Ultra_v2.4.0-S26Ultra-debug.apk` (`:app-mobile`)**: Samsung Galaxy S26 Ultra (Android 16 / One UI 8) için geliştirilmiştir.

---

## YÖNTEM 1: Bilgisayara Hiçbir Program Kurmadan Bulutta (GitHub) Otomatik APK Oluşturma (2 Dakika)
Proje içerisine **`.github/workflows/build-apks.yml`** otomatik bulut derleyicisi eklenmiştir:
1. Ücretsiz bir GitHub hesabında yeni bir depo (Repository) açın.
2. Bu ZIP paketinin içindeki tüm dosya ve klasörleri depoya yükleyin.
3. Üst menüdeki **Actions** sekmesine tıklayın. **"ULAK BYD APK Otomatik Derleyici"** otomatik olarak çalışacak ve ~2 dakika içinde her iki APK dosyasını imzalanmış olarak (`UlakCar_DiLink3_APK` ve `UlakMobile_S26Ultra_APK`) indirmeye hazır hale getirecektir.

---

## YÖNTEM 2: Bilgisayarda Tek Tıkla APK Üretme
1. ZIP dosyasını bir klasöre çıkartın.
2. Klasörü **Android Studio** ile açın ve Gradle senkronizasyonunun bitmesini bekleyin.
3. Klasör içindeki **`APK_OLUSTUR_WINDOWS.bat`** (veya Mac/Linux için **`APK_OLUSTUR_MAC_LINUX.sh`**) dosyasına çift tıklayın.
4. Oluşturulan APK dosyalarınız şu dizinlerde hazır olacaktır:
   - `app-car/build/outputs/apk/debug/UlakCar_DiLink3_v2.4.0-DiLink3-debug.apk`
   - `app-mobile/build/outputs/apk/debug/UlakMobile_S26Ultra_v2.4.0-S26Ultra-debug.apk`

---

## YÖNTEM 3: BYD Seal U DM-i (DiLink 3.0) Ekranına Kurulum
1. FAT32 formatlı bir USB belleğin ana dizinine **`Third Party Apps`** adında bir klasör oluşturun.
2. `UlakCar_DiLink3_v2.4.0-DiLink3-debug.apk` dosyasını bu klasörün içine kopyalayın.
3. USB belleği BYD Seal U DM-i aracınızın orta konsolundaki veri USB portuna takın.
4. DiLink 3.0 ekranında beliren şifre penceresine **`BYD6125`** veya **`20210930`** şifresini girin ve **Ulak Araç** uygulamasını yükleyin.
5. Kurulum sonrasında **DiLink 3.0 Ayarlar -> Uygulama Yöneticisi -> Ulak Araç** ekranından **Otomatik Başlatma** ve **Arka Planda Çalışma** izinlerini açın.
