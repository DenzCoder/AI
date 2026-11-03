package id.asisten.app.domain.formulir

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.JangkaTujuan
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tonggak
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.aktivitas.Tujuan
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.Piket
import id.asisten.app.domain.jadwal.ValidasiJadwal
import id.asisten.app.domain.waktu.Waktu
import java.time.DayOfWeek
import java.time.LocalDate

/** Result of validating a form: a ready-to-save value or a message for the user (Indonesian). */
sealed interface Hasil<out T> {
    data class Ok<out T>(val nilai: T) : Hasil<T>
    data class Galat(val pesan: String) : Hasil<Nothing>
}

object Pesan {
    const val JUDUL_KOSONG = "Judul tidak boleh kosong."
    const val NAMA_KOSONG = "Nama tidak boleh kosong."
    const val KETERANGAN_KOSONG = "Keterangan tidak boleh kosong."
    const val JAM_MULAI = "Format jam mulai harus JJ:MM, contoh 07:30."
    const val JAM_SELESAI = "Format jam selesai harus JJ:MM, contoh 09:00."
    const val JAM_MASUK = "Format jam masuk harus JJ:MM, contoh 07:00."
    const val JAM_PULANG = "Format jam pulang harus JJ:MM, contoh 14:00."
    const val JAM_URUT = "Jam selesai harus setelah jam mulai."
    const val JAM_SEKOLAH_URUT = "Jam pulang harus setelah jam masuk."
    const val TANGGAL = "Format tanggal harus HH-BB-TTTT, contoh 02-11-2026."
}

private fun bersih(teks: String, maks: Int): String =
    teks.trim().replace(Regex("\\s+"), " ").take(maks).trim()

data class DrafKegiatan(
    val judul: String = "",
    val sekali: Boolean = true,
    val tanggalTeks: String = "",
    val hari: DayOfWeek = DayOfWeek.MONDAY,
    val mulaiTeks: String = "",
    val selesaiTeks: String = "",
    val prioritas: Prioritas = Prioritas.SEDANG,
    val catatan: String = "",
)

data class DrafPelajaran(
    val nama: String = "",
    val guru: String = "",
    val ruang: String = "",
    val mulaiTeks: String = "",
    val selesaiTeks: String = "",
    val istirahat: Boolean = false,
)

object FormulirJadwal {
    const val MAKS_JUDUL = 80
    const val MAKS_CATATAN = 200
    const val MAKS_NAMA = 60
    const val MAKS_PIKET = 80
    const val NAMA_ISTIRAHAT_BAWAAN = "Istirahat"

    /** For a weekly activity the date text is ignored; for a one-time activity the weekday is ignored. */
    fun kegiatan(id: Long, d: DrafKegiatan): Hasil<Kegiatan> {
        val judul = bersih(d.judul, MAKS_JUDUL)
        if (judul.isEmpty()) return Hasil.Galat(Pesan.JUDUL_KOSONG)
        val mulai = Waktu.parseJam(d.mulaiTeks) ?: return Hasil.Galat(Pesan.JAM_MULAI)
        val selesai = Waktu.parseJam(d.selesaiTeks) ?: return Hasil.Galat(Pesan.JAM_SELESAI)
        if (!ValidasiJadwal.waktuValid(mulai, selesai)) return Hasil.Galat(Pesan.JAM_URUT)
        val tanggal: LocalDate? =
            if (d.sekali) (Waktu.parseTanggal(d.tanggalTeks) ?: return Hasil.Galat(Pesan.TANGGAL)) else null
        return Hasil.Ok(
            Kegiatan(
                id = id,
                judul = judul,
                tanggal = tanggal,
                hariMingguan = if (d.sekali) null else d.hari,
                mulai = mulai,
                selesai = selesai,
                prioritas = d.prioritas,
                catatan = d.catatan.trim().take(MAKS_CATATAN),
            ),
        )
    }

    fun jamSekolah(hari: DayOfWeek, masukTeks: String, pulangTeks: String): Hasil<JamSekolah> {
        val masuk = Waktu.parseJam(masukTeks) ?: return Hasil.Galat(Pesan.JAM_MASUK)
        val pulang = Waktu.parseJam(pulangTeks) ?: return Hasil.Galat(Pesan.JAM_PULANG)
        if (!ValidasiJadwal.waktuValid(masuk, pulang)) return Hasil.Galat(Pesan.JAM_SEKOLAH_URUT)
        return Hasil.Ok(JamSekolah(hari, masuk, pulang))
    }

    /** A break may leave the name empty (it then reads "Istirahat"); a lesson must be named. */
    fun pelajaran(id: Long, hari: DayOfWeek, d: DrafPelajaran): Hasil<Pelajaran> {
        var nama = bersih(d.nama, MAKS_NAMA)
        if (nama.isEmpty()) {
            if (d.istirahat) nama = NAMA_ISTIRAHAT_BAWAAN else return Hasil.Galat(Pesan.NAMA_KOSONG)
        }
        val mulai = Waktu.parseJam(d.mulaiTeks) ?: return Hasil.Galat(Pesan.JAM_MULAI)
        val selesai = Waktu.parseJam(d.selesaiTeks) ?: return Hasil.Galat(Pesan.JAM_SELESAI)
        if (!ValidasiJadwal.waktuValid(mulai, selesai)) return Hasil.Galat(Pesan.JAM_URUT)
        return Hasil.Ok(
            Pelajaran(
                id = id,
                hari = hari,
                mulai = mulai,
                selesai = selesai,
                nama = nama,
                guru = if (d.istirahat) "" else bersih(d.guru, MAKS_NAMA),
                ruang = if (d.istirahat) "" else bersih(d.ruang, MAKS_NAMA),
                istirahat = d.istirahat,
            ),
        )
    }

    fun piket(id: Long, hari: DayOfWeek, keterangan: String): Hasil<Piket> {
        val teks = bersih(keterangan, MAKS_PIKET)
        if (teks.isEmpty()) return Hasil.Galat(Pesan.KETERANGAN_KOSONG)
        return Hasil.Ok(Piket(id, hari, teks))
    }
}

data class DrafTugas(
    val judul: String = "",
    val jenis: JenisTugas = JenisTugas.TUGAS,
    val mapel: String = "",
    val tenggatTeks: String = "",
    val prioritas: Prioritas = Prioritas.SEDANG,
    val catatan: String = "",
)

data class DrafTujuan(
    val judul: String = "",
    val jangka: JangkaTujuan = JangkaTujuan.PENDEK,
    val tenggatTeks: String = "",
)

object FormulirAktivitas {
    const val MAKS_JUDUL = 100
    const val MAKS_MAPEL = 50
    const val MAKS_CATATAN = 200

    private fun tenggat(teks: String): Hasil<LocalDate?> {
        if (teks.isBlank()) return Hasil.Ok(null)
        val tanggal = Waktu.parseTanggal(teks) ?: return Hasil.Galat(Pesan.TANGGAL)
        return Hasil.Ok(tanggal)
    }

    /** [lama] is the task being edited; its completion state is kept. */
    fun tugas(id: Long, d: DrafTugas, lama: Tugas? = null): Hasil<Tugas> {
        val judul = bersih(d.judul, MAKS_JUDUL)
        if (judul.isEmpty()) return Hasil.Galat(Pesan.JUDUL_KOSONG)
        val batas = when (val h = tenggat(d.tenggatTeks)) {
            is Hasil.Ok -> h.nilai
            is Hasil.Galat -> return h
        }
        return Hasil.Ok(
            Tugas(
                id = id,
                judul = judul,
                jenis = d.jenis,
                mapel = bersih(d.mapel, MAKS_MAPEL),
                tenggat = batas,
                prioritas = d.prioritas,
                selesai = lama?.selesai ?: false,
                selesaiPada = lama?.selesaiPada,
                catatan = d.catatan.trim().take(MAKS_CATATAN),
            ),
        )
    }

    /** [lama] is the goal being edited; its completion state and milestones are kept. */
    fun tujuan(id: Long, d: DrafTujuan, lama: Tujuan? = null): Hasil<Tujuan> {
        val judul = bersih(d.judul, MAKS_JUDUL)
        if (judul.isEmpty()) return Hasil.Galat(Pesan.JUDUL_KOSONG)
        val batas = when (val h = tenggat(d.tenggatTeks)) {
            is Hasil.Ok -> h.nilai
            is Hasil.Galat -> return h
        }
        return Hasil.Ok(
            Tujuan(
                id = id,
                judul = judul,
                jangka = d.jangka,
                tenggat = batas,
                selesai = lama?.selesai ?: false,
                selesaiPada = lama?.selesaiPada,
                tonggak = lama?.tonggak ?: emptyList(),
            ),
        )
    }

    fun tonggak(tujuanId: Long, judul: String): Hasil<Tonggak> {
        val teks = bersih(judul, MAKS_JUDUL)
        if (teks.isEmpty()) return Hasil.Galat(Pesan.JUDUL_KOSONG)
        return Hasil.Ok(Tonggak(tujuanId = tujuanId, judul = teks))
    }
}
