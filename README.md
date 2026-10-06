# PulseChat - Realtime Messaging, Premium Codes & Customization Platform

PulseChat (ConnectPro) adalah aplikasi pesan instan modern berbasis **Kotlin** dan **Jetpack Compose (Material Design 3)** dengan backend **Google Firebase (Authentication, Cloud Firestore, Cloud Messaging)** yang dilengkapi sistem **Premium Code**, **Daily Spinner**, **Custom Check Messaging**, dan **Admin Panel**.

Repository: [zuccheroxix-lab/ConnectPro-React](https://github.com/zuccheroxix-lab/ConnectPro-React)

---

## ⬇️ Download APK

File APK resmi hasil build dari source code repository:

### 1. Debug APK
- **File:** `app-debug.apk`
- **Ukuran:** 28 MB
- **Package ID:** `com.aistudio.pulsechat.kvyqtz`
- **Tipe:** Development / Debug Build (Ditandatangani dengan keystore debug)
- **Direct Download Link:** [Download app-debug.apk](https://github.com/zuccheroxix-lab/ConnectPro-React/releases/download/v1.0.0/app-debug.apk)

### 2. Release APK
- **File:** `app-release.apk`
- **Ukuran:** 20 MB (Teroptimasi R8 ProGuard)
- **Package ID:** `com.aistudio.pulsechat.kvyqtz`
- **Tipe:** Production / Release Build (Ditandatangani dengan upload signing keystore)
- **Direct Download Link:** [Download app-release.apk](https://github.com/zuccheroxix-lab/ConnectPro-React/releases/download/v1.0.0/app-release.apk)

### 🔗 Halaman Rilis & CI/CD
- **GitHub Release v1.0.0:** [Halaman Rilis Resmi v1.0.0](https://github.com/zuccheroxix-lab/ConnectPro-React/releases/tag/v1.0.0)
- **GitHub Actions Build Run:** [Status Workflow CI/CD](https://github.com/zuccheroxix-lab/ConnectPro-React/actions/workflows/build-apk.yml)

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

## 🛠️ Cara Menjalankan Build Secara Mandiri

### 1. Build Debug APK
```bash
gradle assembleDebug
```
Output APK terverifikasi:
- `app/build/outputs/apk/debug/app-debug.apk` (28 MB)

### 2. Build Release APK
```bash
export STORE_PASSWORD="your-password"
export KEY_PASSWORD="your-password"
gradle assembleRelease
```
Output APK terverifikasi:
- `app/build/outputs/apk/release/app-release.apk` (20 MB)

---

## 🚀 Otomasi CI/CD GitHub Actions (`.github/workflows/build-apk.yml`)

Setiap push ke branch `main` atau tag `v*` akan memicu:
1. Setup lingkungan JDK 17 & Android SDK.
2. Build `app-debug.apk` & `app-release.apk`.
3. Validasi fisik keberadaan dan ukuran file APK.
4. Upload kedua APK sebagai artifact GitHub Actions.
5. Publikasi otomatis ke GitHub Release v1.0.0 dengan kedua asset APK terlampir.
