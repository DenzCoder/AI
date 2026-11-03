package id.asisten.app.domain.dasbor

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.aktivitas.Tujuan
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.Piket
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PenyusunBerandaTest {

    private val senin = LocalDate.of(2026, 11, 2)

    @Test
    fun tanpaDataSemuaKosongDanTidakAdaDataKarangan() {
        val r = PenyusunBeranda.susun(senin, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), emptyList())
        assertTrue(r.jadwalHariIni.isEmpty())
        assertNull(r.jamSekolahHariIni)
        assertTrue(r.piketHariIni.isEmpty())
        assertTrue(r.tugasTerdekat.isEmpty())
        assertTrue(r.tujuanAktif.isEmpty())
    }

    @Test
    fun jamSekolahDanPiketHanyaUntukHariIni() {
        val r = PenyusunBeranda.susun(
            hariIni = senin,
            kegiatan = emptyList(),
            pelajaran = emptyList(),
            jamSekolah = listOf(
                JamSekolah(DayOfWeek.TUESDAY, LocalTime.of(7, 0), LocalTime.of(13, 0)),
                JamSekolah(DayOfWeek.MONDAY, LocalTime.of(6, 30), LocalTime.of(14, 0)),
            ),
            piket = listOf(
                Piket(1, DayOfWeek.MONDAY, "Menyapu"),
                Piket(2, DayOfWeek.TUESDAY, "Menghapus papan"),
            ),
            tugas = emptyList(),
            tujuan = emptyList(),
        )
        assertEquals(LocalTime.of(6, 30), r.jamSekolahHariIni?.masuk)
        assertEquals(listOf("Menyapu"), r.piketHariIni.map { it.keterangan })
    }

    @Test
    fun tugasTerdekatMembuangYangSelesaiDanUjianLaluDibatasi() {
        val tugas = listOf(
            Tugas(1, "selesai", tenggat = senin, selesai = true),
            Tugas(2, "ujian", jenis = JenisTugas.UJIAN, tenggat = senin),
            Tugas(3, "a", tenggat = senin.plusDays(1)),
            Tugas(4, "b", tenggat = senin.plusDays(2)),
            Tugas(5, "c", tenggat = senin.plusDays(3)),
            Tugas(6, "d", tenggat = senin.plusDays(4), prioritas = Prioritas.TINGGI),
        )
        val r = PenyusunBeranda.susun(senin, emptyList(), emptyList(), emptyList(), emptyList(), tugas, emptyList())
        assertEquals(PenyusunBeranda.BATAS_TUGAS, r.tugasTerdekat.size)
        assertEquals(listOf(3L, 4L, 5L), r.tugasTerdekat.map { it.id })
    }

    @Test
    fun tujuanAktifMembuangYangSudahSelesaiDanDibatasi() {
        val tujuan = listOf(
            Tujuan(1, "selesai", selesai = true),
            Tujuan(2, "a"), Tujuan(3, "b"), Tujuan(4, "c"), Tujuan(5, "d"),
        )
        val r = PenyusunBeranda.susun(senin, emptyList(), emptyList(), emptyList(), emptyList(), emptyList(), tujuan)
        assertEquals(listOf(2L, 3L, 4L), r.tujuanAktif.map { it.id })
    }

    @Test
    fun jadwalHariIniMemuatKegiatanDanPelajaranHariItu() {
        val r = PenyusunBeranda.susun(
            hariIni = senin,
            kegiatan = listOf(
                Kegiatan(1, "Olahraga", tanggal = senin, mulai = LocalTime.of(5, 30), selesai = LocalTime.of(6, 0)),
            ),
            pelajaran = listOf(Pelajaran(1, DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(8, 0), "IPA")),
            jamSekolah = emptyList(),
            piket = emptyList(),
            tugas = emptyList(),
            tujuan = emptyList(),
        )
        assertEquals(listOf("Olahraga", "IPA"), r.jadwalHariIni.map { it.judul })
    }
}
