# PulseChat - Realtime Messaging, Premium Codes & Customization Platform

PulseChat adalah aplikasi pesan instan modern berbasis **Kotlin** dan **Jetpack Compose (Material Design 3)** dengan backend **Google Firebase (Authentication, Cloud Firestore, Cloud Messaging)** yang dilengkapi sistem **Premium Code**, **Daily Spinner**, **Custom Check Messaging**, dan **Admin Panel**.

---

## 📱 Ringkasan Fitur Aplikasi

### 1. Carrier Phone Authentication & SMS OTP
- Login & Register utama menggunakan nomor ponsel internasional dengan kode negara.
- Verifikasi SMS OTP melalui Firebase Phone Authentication (`PhoneAuthProvider`).
- Dilengkapi timer cooldown 60 detik untuk kirim ulang OTP, validasi batas percobaan, dan penanganan rate limiting.

### 2. User Profiles & Unique Username Validation
- Pembuatan profil pengguna setelah login pertama (Display Name, Username unik, Foto Profil, Bio).
- Validasi username (`@username`) secara real-time langsung di Firestore untuk mencegah duplikasi.

### 3. Realtime 1-on-1 Chat & Centang Pengiriman
- Obrolan dua arah instan dengan sinkronisasi Cloud Firestore.
- **1 Centang (`✓`)**: Pesan tersimpan di server.
- **2 Centang (`✓✓`)**: Pesan terkirim ke perangkat penerima (`deliveredAt`).
- **2 Centang Biru (`✓✓` Cyan/Biru)**: Pesan telah dibaca oleh penerima (`readAt`).
- Dukungan quote reply, copy pesan, dan hapus pesan sendiri.

### 4. Sistem Entitlement & Paket Premium
- Paket didukung: `FREE`, `TRIAL`, `PREMIUM` (30 hari), `PERMANENT`, dan `CUSTOM_CHECK`.
- Validasi status dan masa berlaku diproses di server-side (`user_entitlements`).
- Alur Pembayaran Manual: Pengguna memilih paket di aplikasi -> melihat instruksi dan rekening -> menekan "Chat Admin" -> Admin memverifikasi mutasi dan menerbitkan kode voucher.

### 5. Sistem Redeem Kode Premium Atomik
- Kode dibuat secara eksklusif oleh Admin Panel.
- Menggunakan `db.runTransaction` Firestore untuk mencegah race condition / penggunaan ganda.
- Status kode: `AVAILABLE`, `USED`, `EXPIRED`, `REVOKED`.

### 6. Premium Spinner (Daily Fortune Wheel)
- Kanvas roda berputar interaktif dengan animasi halus.
- Jatah putaran harian (1 gratis per 24 jam) + putaran bonus dari paket / voucher.
- Penentuan hadiah secara server-side: *ZONK / Coba Lagi*, *Trial 1 Hari*, *Trial 3 Hari*, *Custom Check 7 Hari*, *+1 Extra Spin*, *Premium 7 Hari*, hingga *Jackpot Permanent*.
- Riwayat spin tercatat ke Firestore (`spin_history`).

### 7. Custom Check Styling
- Pemilik akses Custom Check dapat mengubah warna centang (Neon Cyan, Electric Blue, Purple Glow, Pink Neon, Emerald Green, Gold Amber, Custom Hex) dan style (Classic, Shield, Glow).
- Centang pada obrolan dirender secara dinamis sesuai style yang dipilih pengguna.

### 8. Admin Panel Terproteksi
- Dashboard statistik pengguna, kode aktif, dan antrean pembayaran.
- Generator kode batch dengan pemilihan durasi dan paket.
- Manajemen pengguna: cari user, grant/revoke entitlement, set role Admin.
- Konfigurasi paket dan hadiah spinner.
- Audit log aktivitas administratif.

---

## 📦 Struktur Build & File APK

| Build Variant | Nama File APK | Lokasi Output Build | Deskripsi |
| :--- | :--- | :--- | :--- |
| **Debug** | `app-debug.apk` | `app/build/outputs/apk/debug/app-debug.apk` | APK development yang sudah ditandatangani dengan keystore debug dan siap diinstall langsung di perangkat Android. |
| **Release** | `app-release.apk` | `app/build/outputs/apk/release/app-release.apk` | APK produksi yang ditandatangani dengan upload signing keystore resmi. |

### Versi Aplikasi
- **Application ID:** `com.aistudio.pulsechat.kvyqtz`
- **versionName:** `1.0.0`
- **versionCode:** `1`

---

## 🛠️ Cara Menjalankan Build Secara Mandiri

### 1. Build Debug APK
```bash
gradle assembleDebug
```
Output APK valid:
`app/build/outputs/apk/debug/app-debug.apk` (Ukuran ~28 MB)

### 2. Build Release APK
Untuk melakukan build release yang telah disign, siapkan variabel environment berikut:
```bash
export KEYSTORE_PATH="/path/to/my-upload-key.jks"
export STORE_PASSWORD="your-store-password"
export KEY_ALIAS="your-key-alias"
export KEY_PASSWORD="your-key-password"

gradle assembleRelease
```
Output APK:
`app/build/outputs/apk/release/app-release.apk`

---

## 🚀 CI/CD GitHub Actions (`.github/workflows/build-apk.yml`)

Workflow GitHub Actions telah dikonfigurasi untuk menjalankan build otomatis dan mempublikasikan file APK:

1. **Trigger:** Push ke branch `main`/`master`, push Git Tag `v*`, atau manual dispatch melalui tab Actions.
2. **Setup:** JDK 17 Temurin dan Android SDK build tools.
3. **Build Debug APK:** Menghasilkan `app-debug.apk` dan memvalidasi keberadaan filenya.
4. **Build Release APK:** Dijalankan secara otomatis jika secrets keystore tersedia di GitHub Repository.
5. **Upload Artifacts:** Kedua APK diunggah sebagai GitHub Actions Artifacts (`app-debug` dan `app-release`) yang dapat diunduh langsung dari halaman run action.
6. **GitHub Release:** Saat push git tag (contoh: `v1.0.0`), action akan membuat release resmi dan melampirkan asset `app-debug.apk` dan `app-release.apk`.

### Konfigurasi GitHub Repository Secrets (Untuk Release Signing):
Untuk mengaktifkan release signing otomatis di GitHub Actions tanpa memasukkan file keystore ke repository publik:
1. Buka **Repository Settings -> Secrets and variables -> Actions**.
2. Tambahkan secret berikut:
   - `RELEASE_KEYSTORE_BASE64`: Isi dengan hasil perintah `base64 -w 0 my-upload-key.jks`
   - `STORE_PASSWORD`: Password keystore
   - `KEY_ALIAS`: Alias key dalam keystore (contoh: `upload`)
   - `KEY_PASSWORD`: Password key alias

---

## ⬇️ Link Download & Akses APK

- **Debug APK (Tersedia)**: Dapat diunduh dari folder build lokal `app/build/outputs/apk/debug/app-debug.apk` atau melalui GitHub Actions Artifact `app-debug`.
- **Halaman Download di Aplikasi:** Terdapat pada menu **Settings -> APK & Build Info** di dalam aplikasi PulseChat.
