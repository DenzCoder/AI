package id.asisten.app.domain.formulir

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.JangkaTujuan
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tonggak
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.aktivitas.Tujuan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FormulirTest {

    private fun <T> ok(h: Hasil<T>): T = (h as? Hasil.Ok<T>)?.nilai ?: error("Harusnya Ok tetapi: $h")
    private fun galat(h: Hasil<*>): String = (h as? Hasil.Galat)?.pesan ?: error("Harusnya Galat tetapi: $h")

    // ---- kegiatan ----

    @Test
    fun kegiatanSekaliValid() {
        val k = ok(
            FormulirJadwal.kegiatan(
                0,
                DrafKegiatan(
                    judul = "  Belajar   IPA ", sekali = true, tanggalTeks = "02-11-2026",
                    mulaiTeks = "19:00", selesaiTeks = "20.30", prioritas = Prioritas.TINGGI, catatan = " bab 3 ",
                ),
            ),
        )
        assertEquals("Belajar IPA", k.judul)
        assertEquals(LocalDate.of(2026, 11, 2), k.tanggal)
        assertNull(k.hariMingguan)
        assertEquals(LocalTime.of(19, 0), k.mulai)
        assertEquals(LocalTime.of(20, 30), k.selesai)
        assertEquals(Prioritas.TINGGI, k.prioritas)
        assertEquals("bab 3", k.catatan)
    }

    @Test
    fun kegiatanMingguanMengabaikanTeksTanggal() {
        val k = ok(
            FormulirJadwal.kegiatan(
                7,
                DrafKegiatan(
                    judul = "Les", sekali = false, tanggalTeks = "sembarang", hari = DayOfWeek.THURSDAY,
                    mulaiTeks = "16:00", selesaiTeks = "17:00",
                ),
            ),
        )
        assertEquals(7L, k.id)
        assertNull(k.tanggal)
        assertEquals(DayOfWeek.THURSDAY, k.hariMingguan)
    }

    @Test
    fun kegiatanMenolakInputSalahDenganPesanJelas() {
        val dasar = DrafKegiatan(judul = "A", tanggalTeks = "02-11-2026", mulaiTeks = "08:00", selesaiTeks = "09:00")
        assertEquals(Pesan.JUDUL_KOSONG, galat(FormulirJadwal.kegiatan(0, dasar.copy(judul = "   "))))
        assertEquals(Pesan.JAM_MULAI, galat(FormulirJadwal.kegiatan(0, dasar.copy(mulaiTeks = "8"))))
        assertEquals(Pesan.JAM_SELESAI, galat(FormulirJadwal.kegiatan(0, dasar.copy(selesaiTeks = "25:00"))))
        assertEquals(Pesan.JAM_URUT, galat(FormulirJadwal.kegiatan(0, dasar.copy(selesaiTeks = "08:00"))))
        assertEquals(Pesan.JAM_URUT, galat(FormulirJadwal.kegiatan(0, dasar.copy(mulaiTeks = "10:00"))))
        assertEquals(Pesan.TANGGAL, galat(FormulirJadwal.kegiatan(0, dasar.copy(tanggalTeks = "31-02-2026"))))
    }

    @Test
    fun judulKegiatanDipotongSesuaiBatas() {
        val k = ok(
            FormulirJadwal.kegiatan(
                0,
                DrafKegiatan(judul = "x".repeat(500), tanggalTeks = "02-11-2026", mulaiTeks = "08:00", selesaiTeks = "09:00"),
            ),
        )
        assertEquals(FormulirJadwal.MAKS_JUDUL, k.judul.length)
    }

    // ---- sekolah ----

    @Test
    fun jamSekolahValidDanTidakValid() {
        val j = ok(FormulirJadwal.jamSekolah(DayOfWeek.MONDAY, "6.45", "14:00"))
        assertEquals(DayOfWeek.MONDAY, j.hari)
        assertEquals(LocalTime.of(6, 45), j.masuk)
        assertEquals(Pesan.JAM_MASUK, galat(FormulirJadwal.jamSekolah(DayOfWeek.MONDAY, "x", "14:00")))
        assertEquals(Pesan.JAM_PULANG, galat(FormulirJadwal.jamSekolah(DayOfWeek.MONDAY, "07:00", "")))
        assertEquals(Pesan.JAM_SEKOLAH_URUT, galat(FormulirJadwal.jamSekolah(DayOfWeek.MONDAY, "14:00", "07:00")))
    }

    @Test
    fun pelajaranHarusBernama() {
        val d = DrafPelajaran(nama = "  ", mulaiTeks = "07:00", selesaiTeks = "08:00")
        assertEquals(Pesan.NAMA_KOSONG, galat(FormulirJadwal.pelajaran(0, DayOfWeek.MONDAY, d)))
    }

    @Test
    fun istirahatBolehTanpaNamaDanMembuangGuruRuang() {
        val p = ok(
            FormulirJadwal.pelajaran(
                0, DayOfWeek.MONDAY,
                DrafPelajaran(nama = "", guru = "Bu A", ruang = "R1", mulaiTeks = "09:30", selesaiTeks = "10:00", istirahat = true),
            ),
        )
        assertEquals(FormulirJadwal.NAMA_ISTIRAHAT_BAWAAN, p.nama)
        assertTrue(p.istirahat)
        assertEquals("", p.guru)
        assertEquals("", p.ruang)
    }

    @Test
    fun pelajaranValidMenyimpanGuruDanRuang() {
        val p = ok(
            FormulirJadwal.pelajaran(
                3, DayOfWeek.WEDNESDAY,
                DrafPelajaran(nama = "Matematika", guru = " Bu  Sari ", ruang = "Lab 2", mulaiTeks = "07:00", selesaiTeks = "08:30"),
            ),
        )
        assertEquals("Bu Sari", p.guru)
        assertEquals("Lab 2", p.ruang)
        assertEquals(DayOfWeek.WEDNESDAY, p.hari)
        assertEquals(3L, p.id)
    }

    @Test
    fun pelajaranMenolakJamSalah() {
        val d = DrafPelajaran(nama = "IPA", mulaiTeks = "09:00", selesaiTeks = "08:00")
        assertEquals(Pesan.JAM_URUT, galat(FormulirJadwal.pelajaran(0, DayOfWeek.MONDAY, d)))
        assertEquals(Pesan.JAM_MULAI, galat(FormulirJadwal.pelajaran(0, DayOfWeek.MONDAY, d.copy(mulaiTeks = ""))))
    }

    @Test
    fun piketHarusBerisi() {
        assertEquals(Pesan.KETERANGAN_KOSONG, galat(FormulirJadwal.piket(0, DayOfWeek.MONDAY, "  ")))
        val p = ok(FormulirJadwal.piket(0, DayOfWeek.MONDAY, "  Menyapu   kelas "))
        assertEquals("Menyapu kelas", p.keterangan)
    }

    // ---- tugas ----

    @Test
    fun tugasTanpaTenggatDiperbolehkan() {
        val t = ok(FormulirAktivitas.tugas(0, DrafTugas(judul = "Baca buku", jenis = JenisTugas.PR, mapel = " IPA ")))
        assertNull(t.tenggat)
        assertEquals("IPA", t.mapel)
        assertEquals(JenisTugas.PR, t.jenis)
    }

    @Test
    fun tugasMenolakTenggatSalahDanJudulKosong() {
        assertEquals(Pesan.JUDUL_KOSONG, galat(FormulirAktivitas.tugas(0, DrafTugas(judul = ""))))
        assertEquals(Pesan.TANGGAL, galat(FormulirAktivitas.tugas(0, DrafTugas(judul = "A", tenggatTeks = "besok"))))
    }

    @Test
    fun tugasDiubahTetapMempertahankanStatusSelesai() {
        val lama = Tugas(id = 9, judul = "Lama", selesai = true, selesaiPada = 1234L)
        val baru = ok(FormulirAktivitas.tugas(9, DrafTugas(judul = "Baru", tenggatTeks = "05-11-2026"), lama))
        assertTrue(baru.selesai)
        assertEquals(1234L, baru.selesaiPada)
        assertEquals(LocalDate.of(2026, 11, 5), baru.tenggat)
    }

    // ---- tujuan ----

    @Test
    fun tujuanDiubahMempertahankanTonggakDanStatus() {
        val lama = Tujuan(
            id = 4, judul = "Lama", selesai = false,
            tonggak = listOf(Tonggak(1, 4, "langkah 1", selesai = true)),
        )
        val baru = ok(FormulirAktivitas.tujuan(4, DrafTujuan(judul = "Baru", jangka = JangkaTujuan.PANJANG), lama))
        assertEquals("Baru", baru.judul)
        assertEquals(JangkaTujuan.PANJANG, baru.jangka)
        assertEquals(lama.tonggak, baru.tonggak)
    }

    @Test
    fun tujuanMenolakJudulKosongDanTenggatSalah() {
        assertEquals(Pesan.JUDUL_KOSONG, galat(FormulirAktivitas.tujuan(0, DrafTujuan(judul = " "))))
        assertEquals(Pesan.TANGGAL, galat(FormulirAktivitas.tujuan(0, DrafTujuan(judul = "A", tenggatTeks = "2026"))))
    }

    @Test
    fun tonggakHarusBerjudul() {
        assertEquals(Pesan.JUDUL_KOSONG, galat(FormulirAktivitas.tonggak(1, "  ")))
        val t = ok(FormulirAktivitas.tonggak(5, " Selesai bab 1 "))
        assertEquals(5L, t.tujuanId)
        assertEquals("Selesai bab 1", t.judul)
        assertEquals(false, t.selesai)
    }
}
