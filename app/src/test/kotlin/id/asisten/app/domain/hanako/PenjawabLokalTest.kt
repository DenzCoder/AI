package id.asisten.app.domain.hanako

import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.aktivitas.Tujuan
import id.asisten.app.domain.jadwal.ItemHarian
import id.asisten.app.domain.jadwal.SumberItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PenjawabLokalTest {

    private val sekarang = LocalDateTime.of(2026, 11, 5, 16, 20)
    private val hariIni = LocalDate.of(2026, 11, 5)

    private val jadwal = listOf(
        ItemHarian("a", "Matematika", "", LocalTime.of(7, 0), LocalTime.of(8, 30), SumberItem.PELAJARAN, null),
    )

    private fun latar(
        jadwal: List<ItemHarian> = this.jadwal,
        tugas: List<Tugas> = listOf(Tugas(judul = "PR Fisika", tenggat = hariIni.plusDays(1)), Tugas(judul = "Tugas lama", selesai = true)),
        tujuan: List<Tujuan> = listOf(Tujuan(judul = "Lulus UTBK")),
        dibagikan: Boolean = true,
    ) = LatarHanako("Arju", sekarang, jadwal, tugas, tujuan, dataDibagikan = dibagikan)

    @Test
    fun menjawabJadwalDariDataNyata() {
        val b = PenjawabLokal.jawab("Apa jadwalku hari ini?", latar())
        assertTrue(b.teks.contains("• 07:00–08:30 Matematika"))
        assertEquals(EkspresiHanako.SENYUM, b.ekspresi)
    }

    @Test
    fun jadwalKosongDikatakanApaAdanya() {
        val b = PenjawabLokal.jawab("jadwal", latar(jadwal = emptyList()))
        assertTrue(b.teks.contains("belum ada jadwal"))
        assertFalse(b.teks.contains("•"))
    }

    @Test
    fun menjawabTugasYangBelumSelesaiSaja() {
        val b = PenjawabLokal.jawab("tugas apa yang mendesak?", latar())
        assertTrue(b.teks.contains("PR Fisika"))
        assertTrue(b.teks.contains("tenggat besok"))
        assertFalse(b.teks.contains("Tugas lama"))
    }

    @Test
    fun tidakAdaTugasMembuatHanakoTertawaDanTidakMengarang() {
        val b = PenjawabLokal.jawab("ada tugas?", latar(tugas = emptyList()))
        assertTrue(b.teks.contains("Tidak ada tugas"))
        assertEquals(EkspresiHanako.TERTAWA, b.ekspresi)
    }

    @Test
    fun menjawabTujuan() {
        assertTrue(PenjawabLokal.jawab("apa target saya", latar()).teks.contains("Lulus UTBK"))
        assertTrue(PenjawabLokal.jawab("tujuan", latar(tujuan = emptyList())).teks.contains("Belum ada tujuan aktif"))
    }

    @Test
    fun pertanyaanGandaMenghasilkanDuaBagian() {
        val b = PenjawabLokal.jawab("jadwal dan tugas dong", latar())
        assertTrue(b.teks.contains("Jadwalmu hari ini"))
        assertTrue(b.teks.contains("Tugas yang belum selesai"))
    }

    @Test
    fun kataPendekHarusPersisTidakSekadarAwalan() {
        // "prioritas" dan "hi" di dalam kata lain tidak boleh dianggap "pr" atau "hai".
        val b = PenjawabLokal.jawab("prioritaskan hidupku", latar())
        assertEquals(EkspresiHanako.BINGUNG, b.ekspresi)
        assertTrue(PenjawabLokal.jawab("PR hari ini?", latar()).teks.contains("PR Fisika"))
    }

    @Test
    fun sapaanMengikutiWaktuDanNama() {
        val sore = PenjawabLokal.jawab("halo", latar())
        assertTrue(sore.teks.startsWith("Selamat sore, Arju!"))
        val pagi = PenjawabLokal.jawab("hai", latar().copy(sekarang = LocalDateTime.of(2026, 11, 5, 6, 0), namaPengguna = ""))
        assertTrue(pagi.teks.startsWith("Selamat pagi!"))
    }

    @Test
    fun sapaanDenganPertanyaanTidakDobelJawab() {
        val b = PenjawabLokal.jawab("halo, jadwalku apa?", latar())
        assertFalse(b.teks.contains("Selamat"))
        assertTrue(b.teks.contains("Matematika"))
    }

    @Test
    fun terimaKasihMembuatHanakoMalu() {
        val b = PenjawabLokal.jawab("terima kasih ya", latar())
        assertEquals(EkspresiHanako.MALU, b.ekspresi)
    }

    @Test
    fun yangTidakDipahamiDijelaskanJujurDenganCaraMengaktifkanAi() {
        val b = PenjawabLokal.jawab("ceritakan lelucon tentang kucing", latar())
        assertEquals(EkspresiHanako.BINGUNG, b.ekspresi)
        assertTrue(b.teks.contains("mode lokal"))
        assertTrue(b.teks.contains("kunci AI"))
    }

    @Test
    fun jawabanLokalMemakaiDataMeskipBerbagiDenganAiMati() {
        val b = PenjawabLokal.jawab("jadwal", latar(dibagikan = false))
        assertTrue(b.teks.contains("Matematika"))
    }

    @Test
    fun masukanKosongAtauSimbolTidakMelempar() {
        for (s in listOf("", "   ", "???", "\n", "😀")) {
            assertEquals(EkspresiHanako.BINGUNG, PenjawabLokal.jawab(s, latar()).ekspresi)
        }
    }
}
