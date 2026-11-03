package id.asisten.app.data.db

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
import id.asisten.app.domain.memori.ItemMemori
import id.asisten.app.domain.memori.KategoriMemori
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PemetaanTest {

    @Test
    fun memoriBolakBalik() {
        val asli = ItemMemori(3, KategoriMemori.KEBIASAAN, "Bangun jam 5", sensitif = true, dibuat = 99L)
        assertEquals(asli, asli.keEntitas().keDomain())
        assertEquals("KEBIASAAN", asli.keEntitas().kategori)
    }

    @Test
    fun memoriKategoriTidakDikenalJatuhKeKonteks() {
        val e = MemoriEntitas(1, "KATEGORI_MASA_DEPAN", "x", sensitif = false, dibuat = 0)
        assertEquals(KategoriMemori.KONTEKS, e.keDomain().kategori)
    }

    @Test
    fun kegiatanSekaliBolakBalikDanKolomBenar() {
        val asli = Kegiatan(
            id = 4, judul = "Rapat", tanggal = LocalDate.of(2026, 11, 2), hariMingguan = null,
            mulai = LocalTime.of(5, 30), selesai = LocalTime.of(21, 45),
            prioritas = Prioritas.TINGGI, catatan = "bawa laptop",
        )
        val e = asli.keEntitas()
        assertEquals(LocalDate.of(2026, 11, 2).toEpochDay(), e.tanggalHari)
        assertNull(e.hariMingguan)
        assertEquals(5 * 60 + 30, e.mulaiMenit)
        assertEquals(21 * 60 + 45, e.selesaiMenit)
        assertEquals("TINGGI", e.prioritas)
        assertEquals(asli, e.keDomain())
    }

    @Test
    fun kegiatanMingguanBolakBalik() {
        val asli = Kegiatan(
            id = 8, judul = "Les", hariMingguan = DayOfWeek.SUNDAY,
            mulai = LocalTime.of(0, 0), selesai = LocalTime.of(23, 59),
        )
        val e = asli.keEntitas()
        assertNull(e.tanggalHari)
        assertEquals(7, e.hariMingguan)
        assertEquals(asli, e.keDomain())
    }

    @Test
    fun nilaiTersimpanRusakDijepitBukanMelempar() {
        val e = KegiatanEntitas(
            id = 1, judul = "x", tanggalHari = null, hariMingguan = 99,
            mulaiMenit = -5, selesaiMenit = 99999, prioritas = "ANEH", catatan = "",
        )
        val d = e.keDomain()
        assertEquals(DayOfWeek.SUNDAY, d.hariMingguan)
        assertEquals(LocalTime.of(0, 0), d.mulai)
        assertEquals(LocalTime.of(23, 59), d.selesai)
        assertEquals(Prioritas.SEDANG, d.prioritas)
        val hariNol = e.copy(hariMingguan = 0).keDomain()
        assertEquals(DayOfWeek.MONDAY, hariNol.hariMingguan)
    }

    @Test
    fun jamSekolahBolakBalik() {
        val asli = JamSekolah(DayOfWeek.FRIDAY, LocalTime.of(6, 45), LocalTime.of(11, 30))
        val e = asli.keEntitas()
        assertEquals(5, e.hari)
        assertEquals(asli, e.keDomain())
    }

    @Test
    fun pelajaranDanIstirahatBolakBalik() {
        val pel = Pelajaran(2, DayOfWeek.TUESDAY, LocalTime.of(7, 0), LocalTime.of(8, 30), "IPA", "Bu Sari", "R2", false)
        val rehat = Pelajaran(3, DayOfWeek.TUESDAY, LocalTime.of(9, 30), LocalTime.of(10, 0), "Istirahat", "", "", true)
        assertEquals(pel, pel.keEntitas().keDomain())
        assertEquals(rehat, rehat.keEntitas().keDomain())
        assertEquals(true, rehat.keEntitas().istirahat)
    }

    @Test
    fun piketBolakBalik() {
        val asli = Piket(5, DayOfWeek.THURSDAY, "Menyapu")
        assertEquals(asli, asli.keEntitas().keDomain())
    }

    @Test
    fun tugasBolakBalik() {
        val asli = Tugas(
            id = 6, judul = "PR Mat", jenis = JenisTugas.PR, mapel = "Matematika",
            tenggat = LocalDate.of(2026, 11, 5), prioritas = Prioritas.RENDAH,
            selesai = true, selesaiPada = 123456789L, catatan = "no 1-10",
        )
        assertEquals(asli, asli.keEntitas().keDomain())
        val tanpaTenggat = Tugas(judul = "Bebas")
        assertNull(tanpaTenggat.keEntitas().tenggatHari)
        assertEquals(tanpaTenggat, tanpaTenggat.keEntitas().keDomain())
    }

    @Test
    fun tujuanMenggabungkanHanyaTonggakMilikSendiri() {
        val tujuan = TujuanEntitas(1, "Juara kelas", "PANJANG", LocalDate.of(2027, 6, 1).toEpochDay(), false, null)
        val tonggak = listOf(
            TonggakEntitas(10, 1, "Nilai UTS bagus", true),
            TonggakEntitas(11, 2, "milik tujuan lain", false),
            TonggakEntitas(12, 1, "Nilai UAS bagus", false),
        )
        val d = tujuan.keDomain(tonggak)
        assertEquals(JangkaTujuan.PANJANG, d.jangka)
        assertEquals(LocalDate.of(2027, 6, 1), d.tenggat)
        assertEquals(listOf("Nilai UTS bagus", "Nilai UAS bagus"), d.tonggak.map { it.judul })
        assertEquals(0.5f, d.kemajuan, 0.0001f)
    }

    @Test
    fun tujuanKeEntitasTidakMembawaTonggak() {
        val d = Tujuan(
            id = 2, judul = "A", jangka = JangkaTujuan.PENDEK, selesai = true, selesaiPada = 55L,
            tonggak = listOf(Tonggak(1, 2, "x", false)),
        )
        val e = d.keEntitas()
        assertEquals(2L, e.id)
        assertEquals("PENDEK", e.jangka)
        assertEquals(true, e.selesai)
        assertEquals(55L, e.selesaiPada)
        assertEquals(Tonggak(1, 2, "x", false), Tonggak(1, 2, "x", false).keEntitas().keDomain())
    }
}
