# Proyek CRUD Vert.x Sederhana

Ini adalah proyek REST API sederhana yang dibangun menggunakan Vert.x dan Java. Proyek ini mengimplementasikan operasi dasar CRUD (Create, Read, Update, Delete) untuk data pengguna dan dilengkapi dengan sistem autentikasi menggunakan JWT (JSON Web Token).

## Fitur

-   Registrasi Pengguna
-   Login Pengguna dengan JWT
-   Manajemen data pengguna (CRUD)
-   Rute yang dilindungi menggunakan JWT
-   Akses berbasis peran (role-based) untuk beberapa endpoint

## Teknologi yang Digunakan

-   [Vert.x](https://vertx.io/) 4.5.1
-   Java 17
-   [Maven](https://maven.apache.org/)
-   [PostgreSQL](https://www.postgresql.org/)
-   [jBCrypt](https://github.com/jeremyh/jBCrypt) untuk hashing password

## API Endpoints

Berikut adalah daftar endpoint yang tersedia:

| Metode | Endpoint         | Deskripsi                                       | Memerlukan Autentikasi |
| :----- | :--------------- | :---------------------------------------------- | :--------------------- |
| `POST` | `/register`      | Mendaftarkan pengguna baru                      | Tidak                  |
| `POST` | `/login`         | Login untuk mendapatkan token JWT               | Tidak                  |
| `GET`  | `/users`         | Mendapatkan semua data pengguna (hanya admin)   | Ya                     |
| `GET`  | `/users/:id`     | Mendapatkan data pengguna berdasarkan ID        | Ya                     |
| `PUT`  | `/users/:id`     | Memperbarui data pengguna berdasarkan ID        | Ya                     |
| `DELETE`| `/users/:id`    | Menghapus data pengguna berdasarkan ID          | Ya                     |

**Contoh Body untuk Registrasi (`/register`):**

```json
{
  "name": "Nama Pengguna",
  "email": "email@contoh.com",
  "password": "password_rahasia",
  "role": "user"
}
```

**Contoh Body untuk Login (`/login`):**

```json
{
  "email": "email@contoh.com",
  "password": "password_rahasia"
}
```

## Cara Menjalankan Proyek

1.  **Prasyarat:**
    *   Pastikan Anda memiliki Java 17 (atau yang lebih baru) dan Maven terinstal.
    *   Pastikan Anda memiliki instance PostgreSQL yang sedang berjalan.

2.  **Konfigurasi Database:**
    *   Buka file `src/main/java/com/sanedge/example_crud/starter/MainVerticle.java`.
    *   Sesuaikan detail koneksi database PostgreSQL (host, port, nama database, user, dan password) di dalam metode `start()`.

3.  **Jalankan Aplikasi:**
    Gunakan perintah Maven berikut untuk menjalankan aplikasi:

    ```bash
    ./mvnw clean compile exec:java
    ```

    Server akan berjalan di `http://localhost:8888`.

## Perintah Build

Untuk menjalankan tes:

```bash
./mvnw clean test
```

Untuk mengemas aplikasi menjadi file JAR:

```bash
./mvnw clean package
```
