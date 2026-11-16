package id.asisten.app.domain.hanako

/**
 * The functions Hanako may call, in the shape the Gemini API expects (`tools[].functionDeclarations`).
 * Types are the API's upper-case names. The descriptions are what the model reads to decide when to call them,
 * so they say what is needed and what the app does with it. Every call is only a proposal that the user approves.
 */
object DeklarasiAksi {

    private val PRIORITAS = listOf("RENDAH", "SEDANG", "TINGGI")
    private val HARI = listOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU", "MINGGU")
    private const val FORMAT_TANGGAL = "Format TTTT-BB-HH, misalnya 2026-11-20."
    private const val FORMAT_JAM = "Format JJ:MM 24 jam, misalnya 16:30."

    private fun teks(deskripsi: String, pilihan: List<String>? = null): Map<String, Any?> =
        buildMap {
            put("type", "STRING")
            put("description", deskripsi)
            if (pilihan != null) put("enum", pilihan)
        }

    private fun bulat(deskripsi: String) = mapOf("type" to "INTEGER", "description" to deskripsi)

    private fun benar(deskripsi: String) = mapOf("type" to "BOOLEAN", "description" to deskripsi)

    private fun fungsi(nama: String, deskripsi: String, sifat: Map<String, Any?>, wajib: List<String>): Map<String, Any?> =
        buildMap {
            put("name", nama)
            put("description", deskripsi)
            if (sifat.isNotEmpty()) {
                put(
                    "parameters",
                    buildMap {
                        put("type", "OBJECT")
                        put("properties", sifat)
                        if (wajib.isNotEmpty()) put("required", wajib)
                    },
                )
            }
        }

    private val ID_TUGAS = bulat("Id tugas, persis seperti di daftar tugas.")
    private val ID_KEGIATAN = bulat("Id kegiatan, persis seperti di daftar kegiatan.")
    private val ID_PELAJARAN = bulat("Id pelajaran, persis seperti di jadwal pelajaran.")

    val fungsiAksi: List<Map<String, Any?>> = listOf(
        fungsi(
            NamaAksi.TAMBAH_TUGAS,
            "Usulkan menambah tugas, PR, atau ujian baru.",
            mapOf(
                "judul" to teks("Judul singkat."),
                "jenis" to teks("Jenis. Bawaan TUGAS.", listOf("TUGAS", "PR", "UJIAN")),
                "mapel" to teks("Mata pelajaran, boleh kosong."),
                "tenggat" to teks("Batas pengumpulan atau tanggal ujian. $FORMAT_TANGGAL Kosongkan bila tidak ada."),
                "prioritas" to teks("Bawaan SEDANG.", PRIORITAS),
                "catatan" to teks("Catatan tambahan, boleh kosong."),
            ),
            listOf("judul"),
        ),
        fungsi(
            NamaAksi.UBAH_TUGAS,
            "Usulkan mengubah tugas yang sudah ada. Isi hanya bagian yang berubah.",
            mapOf(
                "id" to ID_TUGAS,
                "judul" to teks("Judul baru."),
                "jenis" to teks("Jenis baru.", listOf("TUGAS", "PR", "UJIAN")),
                "mapel" to teks("Mata pelajaran baru. Teks kosong menghapusnya."),
                "tenggat" to teks("Tenggat baru. $FORMAT_TANGGAL"),
                "hapus_tenggat" to benar("true untuk menghapus tenggat. Jangan diisi bersama tenggat."),
                "prioritas" to teks("Prioritas baru.", PRIORITAS),
                "catatan" to teks("Catatan baru. Teks kosong menghapusnya."),
            ),
            listOf("id"),
        ),
        fungsi(
            NamaAksi.TANDAI_TUGAS,
            "Usulkan menandai tugas sudah selesai, atau belum selesai lagi.",
            mapOf("id" to ID_TUGAS, "selesai" to benar("true = selesai, false = belum selesai.")),
            listOf("id", "selesai"),
        ),
        fungsi(
            NamaAksi.HAPUS_TUGAS,
            "Usulkan menghapus tugas. Hanya bila pengguna jelas meminta menghapus, bukan menandai selesai.",
            mapOf("id" to ID_TUGAS),
            listOf("id"),
        ),
        fungsi(
            NamaAksi.TAMBAH_KEGIATAN,
            "Usulkan menambah kegiatan pribadi. Isi 'tanggal' untuk kegiatan sekali, atau 'hari' untuk kegiatan yang berulang tiap minggu, tidak keduanya. " +
                "Pengingat berbunyi sebelum jam mulai sesuai pengaturan pengingat.",
            mapOf(
                "judul" to teks("Judul singkat."),
                "tanggal" to teks("Tanggal kegiatan sekali. $FORMAT_TANGGAL"),
                "hari" to teks("Hari untuk kegiatan mingguan.", HARI),
                "mulai" to teks("Jam mulai. $FORMAT_JAM"),
                "selesai" to teks("Jam selesai, harus setelah jam mulai. $FORMAT_JAM"),
                "prioritas" to teks("Bawaan SEDANG. TINGGI tetap berbunyi saat mode Fokus.", PRIORITAS),
                "catatan" to teks("Catatan tambahan, boleh kosong."),
            ),
            listOf("judul", "mulai", "selesai"),
        ),
        fungsi(
            NamaAksi.UBAH_KEGIATAN,
            "Usulkan mengubah kegiatan yang sudah ada. Isi hanya bagian yang berubah. Memindahkan jam atau hari kegiatan juga memindahkan pengingatnya.",
            mapOf(
                "id" to ID_KEGIATAN,
                "judul" to teks("Judul baru."),
                "tanggal" to teks("Tanggal baru (menjadikannya kegiatan sekali). $FORMAT_TANGGAL"),
                "hari" to teks("Hari baru (menjadikannya kegiatan mingguan).", HARI),
                "mulai" to teks("Jam mulai baru. $FORMAT_JAM"),
                "selesai" to teks("Jam selesai baru. $FORMAT_JAM"),
                "prioritas" to teks("Prioritas baru.", PRIORITAS),
                "catatan" to teks("Catatan baru. Teks kosong menghapusnya."),
            ),
            listOf("id"),
        ),
        fungsi(
            NamaAksi.HAPUS_KEGIATAN,
            "Usulkan menghapus kegiatan. Hanya bila pengguna jelas meminta menghapus atau membatalkannya.",
            mapOf("id" to ID_KEGIATAN),
            listOf("id"),
        ),
        fungsi(
            NamaAksi.TAMBAH_PELAJARAN,
            "Usulkan menambah satu pelajaran ke jadwal sekolah mingguan.",
            mapOf(
                "hari" to teks("Hari pelajaran.", HARI),
                "mulai" to teks("Jam mulai. $FORMAT_JAM"),
                "selesai" to teks("Jam selesai. $FORMAT_JAM"),
                "nama" to teks("Nama mata pelajaran."),
                "guru" to teks("Nama guru, boleh kosong."),
                "ruang" to teks("Ruang kelas, boleh kosong."),
            ),
            listOf("hari", "mulai", "selesai", "nama"),
        ),
        fungsi(
            NamaAksi.UBAH_PELAJARAN,
            "Usulkan mengubah pelajaran di jadwal sekolah. Isi hanya bagian yang berubah.",
            mapOf(
                "id" to ID_PELAJARAN,
                "hari" to teks("Hari baru.", HARI),
                "mulai" to teks("Jam mulai baru. $FORMAT_JAM"),
                "selesai" to teks("Jam selesai baru. $FORMAT_JAM"),
                "nama" to teks("Nama baru."),
                "guru" to teks("Guru baru. Teks kosong menghapusnya."),
                "ruang" to teks("Ruang baru. Teks kosong menghapusnya."),
            ),
            listOf("id"),
        ),
        fungsi(
            NamaAksi.HAPUS_PELAJARAN,
            "Usulkan menghapus pelajaran dari jadwal sekolah. Hanya bila pengguna jelas memintanya.",
            mapOf("id" to ID_PELAJARAN),
            listOf("id"),
        ),
        fungsi(
            NamaAksi.ATUR_PENGINGAT,
            "Usulkan mengubah pengaturan pengingat. Isi hanya yang berubah. Pengingat kegiatan berbunyi sekian menit SEBELUM kegiatan mulai " +
                "(0 = tepat saat mulai). Alarm otomatis dijadwalkan ulang setelah pengaturan berubah.",
            mapOf(
                "aktif" to benar("Menyalakan atau mematikan semua pengingat."),
                "menit_sebelum_kegiatan" to bulat("Menit sebelum kegiatan dimulai, 0 sampai 720."),
                "menit_sebelum_sekolah" to bulat("Menit sebelum jam masuk sekolah, 0 sampai 720."),
                "tunda_menit" to bulat("Lama tunda saat pengingat ditunda, 1 sampai 120 menit."),
                "jam_tenang_aktif" to benar("Menyalakan atau mematikan jam tenang (pengingat tanpa suara)."),
                "jam_tenang_mulai" to teks("Awal jam tenang. $FORMAT_JAM"),
                "jam_tenang_selesai" to teks("Akhir jam tenang. $FORMAT_JAM"),
            ),
            emptyList(),
        ),
        fungsi(
            NamaAksi.MULAI_MODE,
            "Usulkan memulai mode Fokus (pengingat berprioritas rendah dan sedang tanpa suara) atau mode Istirahat (semua pengingat tanpa suara) untuk waktu terbatas.",
            mapOf(
                "mode" to teks("Mode yang dimulai.", listOf("FOKUS", "ISTIRAHAT")),
                "menit" to bulat("Lama mode dalam menit, 1 sampai 480. Bawaan 25 untuk FOKUS dan 15 untuk ISTIRAHAT."),
            ),
            listOf("mode"),
        ),
        fungsi(
            NamaAksi.AKHIRI_MODE,
            "Usulkan mengakhiri mode Fokus atau Istirahat yang sedang berjalan.",
            emptyMap(),
            emptyList(),
        ),
    )

    /** The value of the request's `tools` field. */
    val alat: List<Any?> = listOf(mapOf("functionDeclarations" to fungsiAksi))

    /** [alat] as JSON text, ready to put into the request body. */
    fun json(): String = MiniJson.tulis(alat)
}
