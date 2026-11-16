package id.asisten.app.domain.hanako

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.ValidasiJadwal
import id.asisten.app.domain.pengingat.ModePengingat
import id.asisten.app.domain.pengingat.PengaturanPengingat
import id.asisten.app.domain.waktu.Waktu
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** The function names Hanako may call. [DeklarasiAksi] declares exactly these, and [UraiAksi] parses exactly these. */
object NamaAksi {
    const val TAMBAH_TUGAS = "tambah_tugas"
    const val UBAH_TUGAS = "ubah_tugas"
    const val TANDAI_TUGAS = "tandai_tugas"
    const val HAPUS_TUGAS = "hapus_tugas"
    const val TAMBAH_KEGIATAN = "tambah_kegiatan"
    const val UBAH_KEGIATAN = "ubah_kegiatan"
    const val HAPUS_KEGIATAN = "hapus_kegiatan"
    const val TAMBAH_PELAJARAN = "tambah_pelajaran"
    const val UBAH_PELAJARAN = "ubah_pelajaran"
    const val HAPUS_PELAJARAN = "hapus_pelajaran"
    const val ATUR_PENGINGAT = "atur_pengingat"
    const val MULAI_MODE = "mulai_mode"
    const val AKHIRI_MODE = "akhiri_mode"

    val SEMUA = listOf(
        TAMBAH_TUGAS, UBAH_TUGAS, TANDAI_TUGAS, HAPUS_TUGAS,
        TAMBAH_KEGIATAN, UBAH_KEGIATAN, HAPUS_KEGIATAN,
        TAMBAH_PELAJARAN, UBAH_PELAJARAN, HAPUS_PELAJARAN,
        ATUR_PENGINGAT, MULAI_MODE, AKHIRI_MODE,
    )
}

sealed interface HasilUrai {
    data class Sah(val aksi: AksiHanako) : HasilUrai

    /** [alasan] is an Indonesian sentence the user can read ("Tugas dengan id 7 tidak ditemukan."). */
    data class Tidak(val alasan: String) : HasilUrai
}

/**
 * Turns a function call from the model into a checked [AksiHanako], or explains why it is refused.
 *
 * The model is not trusted: every id must exist right now, every date, time and number is range-checked, text is
 * cleaned and length-limited, and unknown function names are refused. A call that passes here is still only a
 * proposal; nothing changes until the user approves it.
 */
object UraiAksi {
    const val MAKS_JUDUL = 100
    const val MAKS_TEKS = 60
    const val MAKS_CATATAN = 300
    const val MAKS_MENIT_MODE = 480
    private const val MENIT_FOKUS_BAWAAN = 25
    private const val MENIT_ISTIRAHAT_BAWAAN = 15

    fun urai(panggilan: PanggilanFungsi, data: DataSaatIni): HasilUrai = try {
        HasilUrai.Sah(bangun(panggilan, data))
    } catch (e: Tolak) {
        HasilUrai.Tidak(e.message ?: "Usulan tidak sah.")
    }

    private class Tolak(alasan: String) : Exception(alasan)

    private fun tolak(alasan: String): Nothing = throw Tolak(alasan)

    private fun bangun(p: PanggilanFungsi, d: DataSaatIni): AksiHanako {
        val a = Argumen(p.args, d.hariIni)
        return when (p.nama) {
            NamaAksi.TAMBAH_TUGAS -> {
                AksiHanako.TambahTugas(
                    Tugas(
                        judul = a.teks("judul", MAKS_JUDUL, Isian.WAJIB)!!,
                        jenis = a.enum<JenisTugas>("jenis") ?: JenisTugas.TUGAS,
                        mapel = a.teks("mapel", MAKS_TEKS) ?: "",
                        tenggat = a.tanggal("tenggat"),
                        prioritas = a.enum<Prioritas>("prioritas") ?: Prioritas.SEDANG,
                        catatan = a.teks("catatan", MAKS_CATATAN) ?: "",
                    ),
                )
            }
            NamaAksi.UBAH_TUGAS -> {
                val lama = tugasDari(a.id(), d)
                val hapusTenggat = a.boolean("hapus_tenggat") ?: false
                val tenggat = a.tanggal("tenggat")
                if (hapusTenggat && tenggat != null) tolak("Pilih salah satu: isi 'tenggat' atau 'hapus_tenggat', jangan keduanya.")
                val ubah = PerubahanTugas(
                    judul = a.teks("judul", MAKS_JUDUL, Isian.TIDAK_KOSONG),
                    jenis = a.enum<JenisTugas>("jenis"),
                    mapel = a.teks("mapel", MAKS_TEKS),
                    tenggat = tenggat,
                    hapusTenggat = hapusTenggat,
                    prioritas = a.enum<Prioritas>("prioritas"),
                    catatan = a.teks("catatan", MAKS_CATATAN),
                )
                if (ubah.kosong) tolak("Tidak ada yang diubah pada tugas \"${lama.judul}\".")
                if (lama.terapkan(ubah) == lama) tolak("Tugas \"${lama.judul}\" sudah seperti itu, tidak ada yang berubah.")
                AksiHanako.UbahTugas(lama.id, ubah)
            }
            NamaAksi.TANDAI_TUGAS -> {
                val lama = tugasDari(a.id(), d)
                val selesai = a.boolean("selesai") ?: tolak("Isian 'selesai' wajib diisi (true atau false).")
                if (lama.selesai == selesai) {
                    tolak("Tugas \"${lama.judul}\" sudah ${if (selesai) "ditandai selesai" else "berstatus belum selesai"}.")
                }
                AksiHanako.TandaiTugas(lama.id, selesai)
            }
            NamaAksi.HAPUS_TUGAS -> tugasDari(a.id(), d).let { AksiHanako.HapusTugas(it.id, it.judul) }

            NamaAksi.TAMBAH_KEGIATAN -> {
                val tanggal = a.tanggal("tanggal", bolehLampau = false)
                val hari = a.hari("hari")
                if (tanggal != null && hari != null) tolak("Pilih salah satu: 'tanggal' (sekali) atau 'hari' (mingguan), jangan keduanya.")
                if (tanggal == null && hari == null) tolak("Isi 'tanggal' untuk kegiatan sekali, atau 'hari' untuk kegiatan mingguan.")
                val mulai = a.jam("mulai") ?: tolak("Isian 'mulai' wajib diisi (format JJ:MM).")
                val selesai = a.jam("selesai") ?: tolak("Isian 'selesai' wajib diisi (format JJ:MM).")
                if (!ValidasiJadwal.waktuValid(mulai, selesai)) tolak("Jam selesai harus setelah jam mulai.")
                AksiHanako.TambahKegiatan(
                    Kegiatan(
                        judul = a.teks("judul", MAKS_JUDUL, Isian.WAJIB)!!,
                        tanggal = tanggal,
                        hariMingguan = hari,
                        mulai = mulai,
                        selesai = selesai,
                        prioritas = a.enum<Prioritas>("prioritas") ?: Prioritas.SEDANG,
                        catatan = a.teks("catatan", MAKS_CATATAN) ?: "",
                    ),
                )
            }
            NamaAksi.UBAH_KEGIATAN -> {
                val lama = kegiatanDari(a.id(), d)
                val tanggal = a.tanggal("tanggal", bolehLampau = false)
                val hari = a.hari("hari")
                if (tanggal != null && hari != null) tolak("Pilih salah satu: 'tanggal' (sekali) atau 'hari' (mingguan), jangan keduanya.")
                val ubah = PerubahanKegiatan(
                    judul = a.teks("judul", MAKS_JUDUL, Isian.TIDAK_KOSONG),
                    tanggal = tanggal,
                    hariMingguan = hari,
                    mulai = a.jam("mulai"),
                    selesai = a.jam("selesai"),
                    prioritas = a.enum<Prioritas>("prioritas"),
                    catatan = a.teks("catatan", MAKS_CATATAN),
                )
                if (ubah.kosong) tolak("Tidak ada yang diubah pada kegiatan \"${lama.judul}\".")
                val baru = lama.terapkan(ubah)
                if (!ValidasiJadwal.waktuValid(baru.mulai, baru.selesai)) tolak("Jam selesai harus setelah jam mulai.")
                if (baru == lama) tolak("Kegiatan \"${lama.judul}\" sudah seperti itu, tidak ada yang berubah.")
                AksiHanako.UbahKegiatan(lama.id, ubah)
            }
            NamaAksi.HAPUS_KEGIATAN -> kegiatanDari(a.id(), d).let { AksiHanako.HapusKegiatan(it.id, it.judul) }

            NamaAksi.TAMBAH_PELAJARAN -> {
                val mulai = a.jam("mulai") ?: tolak("Isian 'mulai' wajib diisi (format JJ:MM).")
                val selesai = a.jam("selesai") ?: tolak("Isian 'selesai' wajib diisi (format JJ:MM).")
                if (!ValidasiJadwal.waktuValid(mulai, selesai)) tolak("Jam selesai harus setelah jam mulai.")
                AksiHanako.TambahPelajaran(
                    Pelajaran(
                        hari = a.hari("hari") ?: tolak("Isian 'hari' wajib diisi."),
                        mulai = mulai,
                        selesai = selesai,
                        nama = a.teks("nama", MAKS_JUDUL, Isian.WAJIB)!!,
                        guru = a.teks("guru", MAKS_TEKS) ?: "",
                        ruang = a.teks("ruang", MAKS_TEKS) ?: "",
                    ),
                )
            }
            NamaAksi.UBAH_PELAJARAN -> {
                val lama = pelajaranDari(a.id(), d)
                val ubah = PerubahanPelajaran(
                    hari = a.hari("hari"),
                    mulai = a.jam("mulai"),
                    selesai = a.jam("selesai"),
                    nama = a.teks("nama", MAKS_JUDUL, Isian.TIDAK_KOSONG),
                    guru = a.teks("guru", MAKS_TEKS),
                    ruang = a.teks("ruang", MAKS_TEKS),
                )
                if (ubah.kosong) tolak("Tidak ada yang diubah pada pelajaran \"${lama.nama}\".")
                val baru = lama.terapkan(ubah)
                if (!ValidasiJadwal.waktuValid(baru.mulai, baru.selesai)) tolak("Jam selesai harus setelah jam mulai.")
                if (baru == lama) tolak("Pelajaran \"${lama.nama}\" sudah seperti itu, tidak ada yang berubah.")
                AksiHanako.UbahPelajaran(lama.id, ubah)
            }
            NamaAksi.HAPUS_PELAJARAN -> pelajaranDari(a.id(), d).let { AksiHanako.HapusPelajaran(it.id, it.nama) }

            NamaAksi.ATUR_PENGINGAT -> {
                val ubah = PerubahanPengingat(
                    aktif = a.boolean("aktif"),
                    menitSebelumKegiatan = a.bulat("menit_sebelum_kegiatan", 0, PengaturanPengingat.BATAS_SEBELUM),
                    menitSebelumSekolah = a.bulat("menit_sebelum_sekolah", 0, PengaturanPengingat.BATAS_SEBELUM),
                    tundaMenit = a.bulat("tunda_menit", 1, 120),
                    jamTenangAktif = a.boolean("jam_tenang_aktif"),
                    jamTenangMulai = a.jam("jam_tenang_mulai")?.let { it.hour * 60 + it.minute },
                    jamTenangSelesai = a.jam("jam_tenang_selesai")?.let { it.hour * 60 + it.minute },
                )
                if (ubah.kosong) tolak("Tidak ada pengaturan pengingat yang diubah.")
                if (d.pengingat.terapkan(ubah) == d.pengingat) tolak("Pengaturan pengingat sudah seperti itu, tidak ada yang berubah.")
                AksiHanako.AturPengingat(ubah)
            }
            NamaAksi.MULAI_MODE -> {
                val mode = a.enum<ModePengingat>("mode") ?: tolak("Isian 'mode' wajib diisi (FOKUS atau ISTIRAHAT).")
                if (mode == ModePengingat.NORMAL) tolak("Gunakan akhiri_mode untuk kembali ke mode normal.")
                val menit = a.bulat("menit", 1, MAKS_MENIT_MODE)
                    ?: if (mode == ModePengingat.FOKUS) MENIT_FOKUS_BAWAAN else MENIT_ISTIRAHAT_BAWAAN
                AksiHanako.MulaiMode(mode, menit)
            }
            NamaAksi.AKHIRI_MODE -> {
                if (d.pengingat.modeEfektif(d.sekarangMillis) == ModePengingat.NORMAL) tolak("Tidak ada mode Fokus atau Istirahat yang sedang aktif.")
                AksiHanako.AkhiriMode
            }
            else -> tolak("Aku tidak mengenal perintah \"${p.nama.take(40)}\".")
        }
    }

    private fun tugasDari(id: Long, d: DataSaatIni): Tugas =
        d.tugas.firstOrNull { it.id == id } ?: tolak("Tugas dengan id $id tidak ditemukan.")

    private fun kegiatanDari(id: Long, d: DataSaatIni): Kegiatan =
        d.kegiatan.firstOrNull { it.id == id } ?: tolak("Kegiatan dengan id $id tidak ditemukan.")

    /** Breaks (istirahat) are not shown to the model, so they cannot be addressed either. */
    private fun pelajaranDari(id: Long, d: DataSaatIni): Pelajaran {
        val p = d.pelajaran.firstOrNull { it.id == id } ?: tolak("Pelajaran dengan id $id tidak ditemukan.")
        if (p.istirahat) tolak("Id $id adalah slot istirahat, bukan pelajaran.")
        return p
    }

    private enum class Isian { OPSIONAL, TIDAK_KOSONG, WAJIB }

    /** Invisible characters that could hide text from the user (zero-width, bidirectional controls). */
    private val TAK_TERLIHAT = Regex("[\u200B-\u200F\u202A-\u202E\u2060-\u2064\uFEFF]")
    private val SPASI = Regex("[\\p{Cntrl}\\s]+")

    /** Reads the loosely typed arguments of a function call. Anything that does not fit throws [Tolak]. */
    private class Argumen(private val peta: Map<String, Any?>, private val hariIni: LocalDate) {


        fun teks(kunci: String, maks: Int, isian: Isian = Isian.OPSIONAL): String? {
            val mentah = peta[kunci]
            if (mentah == null) {
                if (isian == Isian.WAJIB) tolak("Isian '$kunci' wajib diisi.")
                return null
            }
            if (mentah !is String) tolak("Isian '$kunci' harus berupa teks.")
            val bersih = SPASI.replace(TAK_TERLIHAT.replace(mentah, ""), " ").trim()
            if (bersih.isEmpty() && isian != Isian.OPSIONAL) tolak("Isian '$kunci' tidak boleh kosong.")
            if (bersih.length > maks) tolak("Isian '$kunci' terlalu panjang (maksimal $maks karakter).")
            return bersih
        }

        /** Models sometimes send "" for a field they mean to leave out: for non-text fields that counts as absent. */
        private fun nilai(kunci: String): Any? = peta[kunci].let { if (it is String && it.isBlank()) null else it }

        fun bulat(kunci: String, min: Int, maks: Int): Int? {
            val mentah = nilai(kunci) ?: return null
            val angka: Double = when (mentah) {
                is Number -> mentah.toDouble()
                is String -> mentah.trim().toDoubleOrNull() ?: tolak("Isian '$kunci' harus berupa angka.")
                else -> tolak("Isian '$kunci' harus berupa angka.")
            }
            if (angka.isNaN() || angka.isInfinite() || angka != Math.rint(angka)) tolak("Isian '$kunci' harus berupa bilangan bulat.")
            if (angka < min || angka > maks) tolak("Isian '$kunci' harus antara $min dan $maks.")
            return angka.toInt()
        }

        fun id(): Long {
            val mentah = nilai("id") ?: tolak("Isian 'id' wajib diisi. Pakai id dari daftar di catatan.")
            val angka: Double = when (mentah) {
                is Number -> mentah.toDouble()
                is String -> mentah.trim().toDoubleOrNull() ?: tolak("Isian 'id' harus berupa angka.")
                else -> tolak("Isian 'id' harus berupa angka.")
            }
            if (angka.isNaN() || angka.isInfinite() || angka != Math.rint(angka) || angka < 1 || angka > 9.0e15) {
                tolak("Isian 'id' tidak sah.")
            }
            return angka.toLong()
        }

        fun boolean(kunci: String): Boolean? = when (val mentah = nilai(kunci)) {
            null -> null
            is Boolean -> mentah
            is String -> when (mentah.trim().lowercase()) {
                "true" -> true
                "false" -> false
                else -> tolak("Isian '$kunci' harus true atau false.")
            }
            else -> tolak("Isian '$kunci' harus true atau false.")
        }

        /**
         * ISO `yyyy-MM-dd` (or `d-M-yyyy`). Must lie within a year back and three years ahead of today, so a
         * wrong guess of the current year is caught. With [bolehLampau] false the date may not be before today.
         */
        fun tanggal(kunci: String, bolehLampau: Boolean = true): LocalDate? {
            val mentah = nilai(kunci) ?: return null
            if (mentah !is String) tolak("Isian '$kunci' harus berupa tanggal (format TTTT-BB-HH).")
            val t = mentah.trim()
            val hasil = if (Regex("\\d{4}-\\d{2}-\\d{2}").matches(t)) {
                runCatching { LocalDate.parse(t) }.getOrNull()
            } else {
                Waktu.parseTanggal(t)
            } ?: tolak("Isian '$kunci' bukan tanggal yang sah (format TTTT-BB-HH).")
            if (hasil.isBefore(hariIni.minusDays(365)) || hasil.isAfter(hariIni.plusYears(3))) {
                tolak("Tanggal ${Waktu.formatTanggal(hasil)} terlalu jauh dari hari ini.")
            }
            if (!bolehLampau && hasil.isBefore(hariIni)) {
                tolak("Tanggal ${Waktu.formatTanggal(hasil)} sudah lewat.")
            }
            return hasil
        }

        fun jam(kunci: String): LocalTime? {
            val mentah = nilai(kunci) ?: return null
            if (mentah !is String) tolak("Isian '$kunci' harus berupa jam (format JJ:MM).")
            return Waktu.parseJam(mentah) ?: tolak("Isian '$kunci' bukan jam yang sah (format JJ:MM, 24 jam).")
        }

        fun hari(kunci: String): DayOfWeek? {
            val mentah = nilai(kunci) ?: return null
            if (mentah !is String) tolak("Isian '$kunci' harus berupa nama hari.")
            val k = mentah.lowercase().filter { it in 'a'..'z' }
            return NAMA_HARI[k] ?: tolak("Isian '$kunci' bukan nama hari (SENIN sampai MINGGU).")
        }

        inline fun <reified E : Enum<E>> enum(kunci: String): E? {
            val mentah = nilai(kunci) ?: return null
            if (mentah !is String) tolak("Isian '$kunci' harus berupa teks.")
            val k = mentah.trim().uppercase()
            return enumValues<E>().firstOrNull { it.name == k }
                ?: tolak("Isian '$kunci' tidak dikenal. Pilihan: ${enumValues<E>().joinToString(", ") { it.name }}.")
        }
    }

    private val NAMA_HARI: Map<String, DayOfWeek> = mapOf(
        "senin" to DayOfWeek.MONDAY, "monday" to DayOfWeek.MONDAY,
        "selasa" to DayOfWeek.TUESDAY, "tuesday" to DayOfWeek.TUESDAY,
        "rabu" to DayOfWeek.WEDNESDAY, "wednesday" to DayOfWeek.WEDNESDAY,
        "kamis" to DayOfWeek.THURSDAY, "thursday" to DayOfWeek.THURSDAY,
        "jumat" to DayOfWeek.FRIDAY, "friday" to DayOfWeek.FRIDAY,
        "sabtu" to DayOfWeek.SATURDAY, "saturday" to DayOfWeek.SATURDAY,
        "minggu" to DayOfWeek.SUNDAY, "ahad" to DayOfWeek.SUNDAY, "sunday" to DayOfWeek.SUNDAY,
    )
}
