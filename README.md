# Diplomacia Bot

Diplomacia icin Java ile gelistirilmis Android ve desktop bot uygulamasi.

Hazirlayan: @thekehribar

## Genel Bakis

Diplomacia Bot, kaydedilen kullanici tokeni ile skill gelistirme isteklerini otomatik zamanlar. Android surumu APK olarak, desktop surumu ise Windows uzerinde kontrol paneli olarak kullanilabilir.

Proje lisans kontrolu veya harici aktivasyon sistemi icermez. Tokenler kullanicinin kendi cihazinda saklanir.

## Kullanici Kurulumu

Bu bolum uygulamayi sadece kullanmak isteyenler icindir. JDK, Android Studio veya Gradle gerekmez.

En kolay indirme yeri GitHub Releases bolumudur. Android icin `DiplomaciaBot.apk`, Windows icin `DiplomaciaBot-Windows.zip` dosyasini indir.

### Android APK

Hazir APK ana klasordedir:

```text
DiplomaciaBot.apk
```

Kurulum:

1. `DiplomaciaBot.apk` dosyasini telefona aktar.
2. Android izin isterse `Bilinmeyen uygulamalari yukle` iznini ac.
3. APK dosyasini ac ve `Yukle` butonuna bas.
4. Uygulamayi ac.

### Windows Desktop

Desktop surumunu kullanmak icin JDK kurman gerekmez. Paylasilan paket klasoru komple durmalidir.

Calistirilacak dosya:

```text
desktop/build/jpackage/DiplomaciaBot/DiplomaciaBot.exe
```

Paylasirken tek basina `DiplomaciaBot.exe` dosyasini degil, su klasoru komple zipleyip gondermek gerekir:

```text
desktop/build/jpackage/DiplomaciaBot/
```

## Uygulama Kullanimi

1. `HESAP 1` veya `HESAP 2` sec.
2. Token gir veya `BAGLAN` butonuyla token al.
3. `Kaydet` butonuna bas.
4. Odeme tipini sec: `Para` veya `Elmas`.
5. Skill sec: `Kisla`, `Savas` veya `Bilim`.
6. `BASLAT` ile otomatik islemi baslat.
7. `DURDUR` ile botu durdur.
8. Emir kuyrugu ile sirali gelistirme ekle.

## Ozellikler

- Android APK ve Windows/desktop uygulamasi
- Iki ayri hesap destegi
- Token kaydetme ve token alma ekrani
- Skill secimi: `savas_teknikleri`, `kisla`, `bilim_insani`
- Odeme tipi secimi: `money`, `diamond`
- Emir kuyrugu ile sirali gelistirme
- API cevabindaki bekleme suresine gore otomatik zamanlama
- 401 durumunda botu otomatik durdurma
- Log ekrani
- Geliştiriciye uygulama icinden bagis gonderme
- Lisans veya aktivasyon kontrolu olmadan kullanim

## Geliştirici Gereksinimleri

Bu bolum projeyi kaynak koddan build etmek veya gelistirmek isteyenler icindir.

Android build icin:

- Android Studio
- Android SDK 35
- JDK 17 veya 21

Desktop build icin:

- JDK 17 veya 21
- EXE/app paketi uretmek icin JDK icinde `jpackage`

## Android Build

Android Studio ile:

1. Proje klasorunu ac: `android-app`
2. Gradle sync isleminin bitmesini bekle.
3. `Build > Build APK(s)` sec.

Komut satiri ile:

```bash
gradlew.bat :app:assembleDebug
```

Gradle APK ciktisi:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Paylasimi kolaylastirmak icin APK'yi ana klasore kopyala:

```powershell
Copy-Item app\build\outputs\apk\debug\app-debug.apk DiplomaciaBot.apk
```

## Desktop Build

Windows icin onerilen paketleme komutu:

```bash
gradlew.bat :desktop:packageExe
```

EXE/app klasoru:

```text
desktop/build/jpackage/DiplomaciaBot/
```

EXE dosyasi:

```text
desktop/build/jpackage/DiplomaciaBot/DiplomaciaBot.exe
```

Gelistirme sirasinda Gradle ile direkt calistirma:

```bash
gradlew.bat :desktop:run
```

## Proje Yapisi

```text
android-app/
  DiplomaciaBot.apk          Kolay kurulum icin hazir APK
  app/                       Android uygulamasi
    src/main/java/...        Android kaynak kodlari
    src/main/res/...         Android kaynaklari
    build.gradle             Android modul ayarlari
  desktop/                   Desktop uygulamasi
    src/main/java/...        Desktop kaynak kodlari
    build.gradle             Desktop modul ayarlari
  gradle/wrapper/            Gradle wrapper
  build.gradle               Kok Gradle ayarlari
  settings.gradle            Modul tanimlari
  README.md                  Kurulum ve kullanim dokumani
```

## Geliştiriciye Bagis

Uygulamadaki `Bagis yap` bolumu, gelistiriciye destek olmak icin eklenmistir. Kullanici miktar girer ve uygulama secili hesapta kayitli token ile sabit gelistirici hesabina transfer istegi gonderir.

Endpoint:

```text
POST https://diplomacia.com.tr/api/transfer/send
```

Gonderilen veri:

```json
{
  "recipient_id": "ec756c8c-d06a-474f-973e-6fdec9cb58c6",
  "amount": 5000000
}
```

`amount` kullanicinin girdigi miktardir. `recipient_id` gelistirici hesabidir ve uygulama icinde sabittir.

## Guvenlik ve Notlar

- Repo icinde gomulu kullanici tokeni bulunmaz.
- Tokenler kullanicinin kendi cihazinda veya bilgisayarinda saklanir.
- `local.properties`, build ciktilari ve IDE dosyalari repo disinda tutulur.
- Android arka plan zamanlamasi WorkManager ile yapilir; pil optimizasyonlari nedeniyle saniyesi saniyesine calisma garanti edilmez.
- `DiplomaciaBot.apk` debug imzali APK'dir; Android kurulumda bilinmeyen kaynak/debug imza uyarilari normaldir.
