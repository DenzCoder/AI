package id.asisten.app.domain.jadwal

import id.asisten.app.domain.Prioritas
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PenyusunHarianTest {

    private val senin = LocalDate.of(2026, 11, 2)
    private val selasa = senin.plusDays(1)

    private fun jam(h: Int, m: Int = 0) = LocalTime.of(h, m)

    private fun kegiatan(
        id: Long,
        judul: String,
        mulai: LocalTime,
        selesai: LocalTime,
        tanggal: LocalDate? = null,
        hari: DayOfWeek? = null,
    ) = Kegiatan(id = id, judul = judul, tanggal = tanggal, hariMingguan = hari, mulai = mulai, selesai = selesai)

    private fun pelajaran(
        id: Long,
        hari: DayOfWeek,
        mulai: LocalTime,
        selesai: LocalTime,
        nama: String = "Mapel$id",
        istirahat: Boolean = false,
        guru: String = "",
        ruang: String = "",
    ) = Pelajaran(id, hari, mulai, selesai, nama, guru, ruang, istirahat)

    // ---- Kegiatan.muncul / tanggalAcuan ----

    @Test
    fun kegiatanSekaliHanyaMunculDiTanggalnya() {
        val k = kegiatan(1, "Rapat", jam(9), jam(10), tanggal = senin)
        assertTrue(k.muncul(senin))
        assertFalse(k.muncul(selasa))
    }

    @Test
    fun kegiatanMingguanMunculTiapHariYangSama() {
        val k = kegiatan(1, "Les", jam(16), jam(17), hari = DayOfWeek.MONDAY)
        assertTrue(k.muncul(senin))
        assertTrue(k.muncul(senin.plusWeeks(3)))
        assertFalse(k.muncul(selasa))
    }

    @Test
    fun kegiatanTanpaTanggalDanHariTidakPernahMuncul() {
        assertFalse(kegiatan(1, "Aneh", jam(9), jam(10)).muncul(senin))
    }

    @Test
    fun tanggalAcuanMingguanMemilihHariBerikutnyaAtauHariItuSendiri() {
        val rabu = kegiatan(1, "X", jam(9), jam(10), hari = DayOfWeek.WEDNESDAY)
        assertEquals(LocalDate.of(2026, 11, 4), rabu.tanggalAcuan(senin))
        val seninLagi = kegiatan(2, "Y", jam(9), jam(10), hari = DayOfWeek.MONDAY)
        assertEquals(senin, seninLagi.tanggalAcuan(senin))
        val sekali = kegiatan(3, "Z", jam(9), jam(10), tanggal = LocalDate.of(2026, 12, 25))
        assertEquals(LocalDate.of(2026, 12, 25), sekali.tanggalAcuan(senin))
    }

    // ---- susun ----

    @Test
    fun susunMenggabungkanDanMengurutkanBerdasarkanWaktu() {
        val hasil = PenyusunHarian.susun(
            senin,
            listOf(kegiatan(1, "Les", jam(16), jam(17), hari = DayOfWeek.MONDAY)),
            listOf(
                pelajaran(1, DayOfWeek.MONDAY, jam(8), jam(9), "Matematika", guru = "Bu Sari", ruang = "R1"),
                pelajaran(2, DayOfWeek.MONDAY, jam(7), jam(8), "IPA"),
                pelajaran(3, DayOfWeek.TUESDAY, jam(7), jam(8), "Seni"),
            ),
        )
        assertEquals(listOf("IPA", "Matematika", "Les"), hasil.map { it.judul })
        assertEquals("Bu Sari · R1", hasil[1].keterangan)
        assertEquals(SumberItem.PELAJARAN, hasil[0].sumber)
        assertEquals(SumberItem.KEGIATAN, hasil[2].sumber)
        assertTrue(hasil.none { it.bentrok })
    }

    @Test
    fun susunHariTanpaDataMengembalikanDaftarKosong() {
        assertTrue(PenyusunHarian.susun(senin, emptyList(), emptyList()).isEmpty())
    }

    @Test
    fun susunMenandaiBlokYangBertabrakan() {
        val hasil = PenyusunHarian.susun(
            senin,
            listOf(kegiatan(1, "Rapat", jam(8, 30), jam(9, 30), tanggal = senin)),
            listOf(pelajaran(1, DayOfWeek.MONDAY, jam(8), jam(9), "Matematika")),
        )
        assertTrue(hasil.all { it.bentrok })
    }

    @Test
    fun blokYangHanyaBersentuhanTidakBentrok() {
        val hasil = PenyusunHarian.susun(
            senin,
            listOf(kegiatan(1, "Rapat", jam(9), jam(10), tanggal = senin)),
            listOf(pelajaran(1, DayOfWeek.MONDAY, jam(8), jam(9), "Matematika")),
        )
        assertTrue(hasil.none { it.bentrok })
    }

    @Test
    fun istirahatTidakPernahDianggapBentrok() {
        val hasil = PenyusunHarian.susun(
            senin,
            listOf(kegiatan(1, "Makan bareng", jam(9, 30), jam(10), tanggal = senin)),
            listOf(pelajaran(1, DayOfWeek.MONDAY, jam(9), jam(10), "Istirahat", istirahat = true)),
        )
        assertEquals(2, hasil.size)
        assertTrue(hasil.none { it.bentrok })
        assertEquals(SumberItem.ISTIRAHAT, hasil.first { it.judul == "Istirahat" }.sumber)
    }

    // ---- bentrokDengan ----

    @Test
    fun bentrokDenganMengembalikanBlokYangTumpangTindih() {
        val draf = kegiatan(0, "Belajar", jam(8, 30), jam(9, 30), tanggal = senin)
        val pel = listOf(
            pelajaran(1, DayOfWeek.MONDAY, jam(8), jam(9), "Matematika"),
            pelajaran(2, DayOfWeek.MONDAY, jam(10), jam(11), "IPA"),
        )
        val hasil = PenyusunHarian.bentrokDengan(draf, senin, emptyList(), pel)
        assertEquals(listOf("Matematika"), hasil.map { it.judul })
    }

    @Test
    fun bentrokDenganTidakMenghitungDirinyaSendiriSaatMengubah() {
        val ada = kegiatan(5, "Belajar", jam(8), jam(9), tanggal = senin)
        val draf = ada.copy(mulai = jam(8, 15), selesai = jam(9, 15))
        assertTrue(PenyusunHarian.bentrokDengan(draf, senin, listOf(ada), emptyList()).isEmpty())
    }

    @Test
    fun bentrokDenganKegiatanLainTetapTerdeteksi() {
        val lain = kegiatan(7, "Rapat", jam(8), jam(9), tanggal = senin)
        val draf = kegiatan(0, "Belajar", jam(8, 30), jam(9, 30), tanggal = senin)
        assertEquals(listOf("Rapat"), PenyusunHarian.bentrokDengan(draf, senin, listOf(lain), emptyList()).map { it.judul })
    }

    // ---- saranSlot ----

    private fun blok(mulai: LocalTime, selesai: LocalTime, sumber: SumberItem = SumberItem.KEGIATAN) =
        ItemHarian("x$mulai", "b", "", mulai, selesai, sumber, null)

    @Test
    fun saranSlotHariKosongMemakaiAwalRentang() {
        assertEquals(jam(6), PenyusunHarian.saranSlot(emptyList(), 60))
    }

    @Test
    fun saranSlotMemilihCelahPertamaYangCukup() {
        val item = listOf(blok(jam(6), jam(7)), blok(jam(7, 30), jam(9)))
        assertEquals(jam(7), PenyusunHarian.saranSlot(item, 30))
        assertEquals(jam(9), PenyusunHarian.saranSlot(item, 45))
    }

    @Test
    fun saranSlotMengabaikanIstirahat() {
        val item = listOf(blok(jam(6), jam(9), SumberItem.ISTIRAHAT))
        assertEquals(jam(6), PenyusunHarian.saranSlot(item, 60))
    }

    @Test
    fun saranSlotNullBilaTidakAdaYangMuat() {
        val item = listOf(blok(jam(6), jam(21, 30)))
        assertNull(PenyusunHarian.saranSlot(item, 60))
        assertNull(PenyusunHarian.saranSlot(emptyList(), 0))
        assertNull(PenyusunHarian.saranSlot(emptyList(), 17 * 60))
    }

    @Test
    fun saranSlotMenanganiBlokBertumpukan() {
        val item = listOf(blok(jam(6), jam(10)), blok(jam(7), jam(8)))
        assertEquals(jam(10), PenyusunHarian.saranSlot(item, 60))
    }

    // ---- ValidasiJadwal ----

    @Test
    fun waktuValidButuhSelesaiSetelahMulai() {
        assertTrue(ValidasiJadwal.waktuValid(jam(8), jam(9)))
        assertFalse(ValidasiJadwal.waktuValid(jam(9), jam(9)))
        assertFalse(ValidasiJadwal.waktuValid(jam(10), jam(9)))
    }

    // ---- Kalender ----

    @Test
    fun hariDalamMingguDimulaiSeninSampaiMinggu() {
        val rabu = LocalDate.of(2026, 11, 4)
        val hari = Kalender.hariDalamMinggu(rabu)
        assertEquals(7, hari.size)
        assertEquals(DayOfWeek.MONDAY, hari.first().dayOfWeek)
        assertEquals(DayOfWeek.SUNDAY, hari.last().dayOfWeek)
        assertEquals(senin, hari.first())
        assertTrue(rabu in hari)
        // Sunday belongs to the week that started the previous Monday.
        assertEquals(senin, Kalender.hariDalamMinggu(LocalDate.of(2026, 11, 8)).first())
    }

    @Test
    fun gridBulanNovember2026() {
        // 1 Nov 2026 is a Sunday: six leading blanks, 30 days, padded to 42 cells.
        val grid = Kalender.gridBulan(YearMonth.of(2026, 11))
        assertEquals(42, grid.size)
        assertEquals(0, grid.size % 7)
        assertTrue(grid.take(6).all { it == null })
        assertEquals(LocalDate.of(2026, 11, 1), grid[6])
        assertEquals(LocalDate.of(2026, 11, 30), grid[35])
        assertTrue(grid.drop(36).all { it == null })
        assertEquals(30, grid.count { it != null })
    }

    @Test
    fun gridBulanYangPasPenuhTanpaPadding() {
        // Feb 2027 starts on Monday and has 28 days.
        val grid = Kalender.gridBulan(YearMonth.of(2027, 2))
        assertEquals(28, grid.size)
        assertTrue(grid.all { it != null })
    }
}
