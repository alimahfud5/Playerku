# Alunan — Pemutar Musik Latar Belakang

Aplikasi pemutar musik Android berbasis **Jetpack Compose** dan **Jetpack Media3 (`MediaSessionService` + `ExoPlayer`)** yang mendukung pemutaran di latar belakang, kontrol notifikasi media, timer tidur, pengatur kecepatan putar, serta impor file audio lokal dari perangkat.

## Fitur Utama

- **Pemutaran Latar Belakang**: Menggunakan `MediaSessionService` dengan `foregroundServiceType="mediaPlayback"` dan kontrol notifikasi media (Putar/Jeda, Berikutnya, Sebelumnya, SeekBar, serta sampul album).
- **Audio Siap Putar + Musik Lokal**: Dilengkapi 4 trek instrumental bawaan serta dukungan impor berkas audio perangkat (`MP3`, `WAV`, `M4A`, `FLAC`, `OGG`) melalui Storage Access Framework (`OpenMultipleDocuments`) tanpa izin penyimpanan luas.
- **Timer Tidur & Kecepatan Putar**: Hitung mundur timer tidur berjalan langsung di service latar belakang dan otomatis menjeda musik saat waktu habis.
- **Antrean, Acak, Ulangi & Favorit**: Tersimpan lokal dengan Room Database dan DataStore Preferences.

---

## Panduan Build & Deploy APK di GitHub

Repositori ini sudah dilengkapi workflow GitHub Actions di `.github/workflows/deploy-apk.yml` untuk membangun dan mendistribusikan file `.apk` secara otomatis.

### 1. Build Otomatis (Debug APK)
Setiap kali Anda melakukan `push` ke branch `main` atau `master`, atau menjalankan workflow secara manual melalui tab **Actions → Deploy Android APK → Run workflow**:
1. GitHub Actions akan menjalankan unit test (`testDebugUnitTest`).
2. Membangun `app-debug.apk` (`assembleDebug`).
3. Mengunggah file APK ke bagian **Artifacts** di halaman hasil run workflow.

### 2. Rilis APK Otomatis via Tag GitHub Releases
Untuk membuat **GitHub Release** beserta lampiran APK yang siap diunduh:

```bash
git tag v1.0.0
git push origin v1.0.0
```

Workflow akan otomatis membuat halaman Release baru untuk tag `v1.0.0` dan melampirkan APK hasil build.

### 3. (Opsional) Build Release APK Bertanda Tangan (Signed Release)
Jika Anda ingin GitHub Actions juga menghasilkan `app-release.apk` yang ditandatangani dengan keystore produksi Anda:

1. Encode file keystore `.jks` Anda ke base64 (pastikan `keyAlias` adalah `upload`):
   ```bash
   base64 -w 0 my-upload-key.jks
   ```
2. Buka repositori GitHub Anda → **Settings → Secrets and variables → Actions → New repository secret**, lalu tambahkan 3 secret berikut:
   - `KEYSTORE_BASE64`: hasil base64 dari file `.jks`
   - `STORE_PASSWORD`: password keystore
   - `KEY_PASSWORD`: password key alias `upload`

Jika ketiga secret tersebut belum diisi, workflow tetap berhasil membangun dan mengunggah Debug APK tanpa error.
