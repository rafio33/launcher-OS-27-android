# iOS 27 Spatial Launcher (Android 14)

Proyek ini sudah dikonfigurasi penuh dengan **GitHub Actions Cloud Builder**. Anda tidak perlu menginstal Android Studio untuk menghasilkan file `.apk`.

## Cara Mendapatkan File `.apk` via GitHub:
1. Buat repository baru di GitHub (pilih **Public** agar kuota GitHub Actions gratis tanpa batas).
2. Ekstrak seluruh isi file ZIP ini, lalu upload/push semua file dan foldernya (termasuk folder `.github`) ke repository tersebut.
3. Buka tab **Actions** di bagian atas halaman GitHub repository Anda.
4. Klik workflow **Build iOS 27 Spatial Launcher APK** yang sedang berjalan, tunggu sekitar **2 - 3 menit** hingga centang hijau ✅.
5. Scroll ke bagian paling bawah halaman workflow tersebut (bagian **Artifacts**), lalu klik **`iOS27-Spatial-Launcher-APK`** untuk mengunduh file ZIP yang berisi:
   - **`app-release.apk`** (Sangat disarankan: sudah dioptimasi R8, super lancar & responsif 60-120 FPS tanpa lag).
   - **`app-debug.apk`** (Versi debug standar).
