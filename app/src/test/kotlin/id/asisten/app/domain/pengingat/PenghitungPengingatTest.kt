package id.asisten.app.domain.pengingat

import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Kegiatan
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PenghitungPengingatTest {

    private val zona = ZoneId.of("Asia/Jakarta")
    private val senin = LocalDate.of(2026, 11, 2)
    private val rabu = LocalDate.of(2026, 11, 4)
    private val kamis = LocalDate.of(2026, 11, 5)

    private fun ms(tgl: LocalDate, jam: Int, menit: Int = 0): Long =
        tgl.atTime(jam, menit).atZone(zona).toInstant().toEpochMilli()

    private fun kegiatanSekali(id: Long, tgl: LocalDate, jam: Int, menit: Int = 0) = Kegiatan(
        id = id, judul = "Kegiatan $id", tanggal = tgl,
        mulai = LocalTime.of(jam, menit), selesai = LocalTime.of(jam + 1, menit),
    )

    private val p = PengaturanPengingat()

    @Test
    fun kegiatanSekaliMengingatkanSebelumMulai() {
        val data = DataPengingat(kegiatan = listOf(kegiatanSekali(1, rabu, 15, 30)))
        val hasil = PenghitungPengingat.hitung(data, p, ms(rabu, 0), ms(rabu, 23, 59), zona)
        assertEquals(1, hasil.size)
        assertEquals(JenisPengingat.KEGIATAN, hasil[0].jenis)
        assertEquals(ms(rabu, 15, 20), hasil[0].waktu)
        assertEquals(ms(rabu, 15, 30), hasil[0].acara)
    }

    @Test
    fun selangNolMengingatkanTepatWaktu() {
        val data = DataPengingat(kegiatan = listOf(kegiatanSekali(1, rabu, 15, 30)))
        val hasil = PenghitungPengingat.hitung(data, p.copy(menitSebelumKegiatan = 0), ms(rabu, 0), ms(rabu, 23, 59), zona)
        assertEquals(ms(rabu, 15, 30), hasil.single().waktu)
    }

    @Test
    fun kegiatanMingguanMunculSetiapMinggu() {
        val k = Kegiatan(
            id = 2, judul = "Les", hariMingguan = DayOfWeek.MONDAY,
            mulai = LocalTime.of(8, 0), selesai = LocalTime.of(9, 0),
        )
        val hasil = PenghitungPengingat.hitung(
            DataPengingat(kegiatan = listOf(k)), p, ms(LocalDate.of(2026, 11, 1), 0), ms(LocalDate.of(2026, 11, 10), 23, 59), zona,
        )
        assertEquals(listOf(ms(senin, 7, 50), ms(senin.plusDays(7), 7, 50)), hasil.map { it.waktu })
    }

    @Test
    fun pengingatSebelumTengahMalamDiHitungDiHariSebelumnya() {
        val data = DataPengingat(kegiatan = listOf(kegiatanSekali(3, kamis, 0, 5)))
        val hasil = PenghitungPengingat.hitung(data, p, ms(rabu, 0), ms(kamis, 0), zona)
        assertEquals(ms(rabu, 23, 55), hasil.single().waktu)
    }

    @Test
    fun batasJendelaDariTerkecualiSampaiTermasuk() {
        val data = DataPengingat(kegiatan = listOf(kegiatanSekali(1, rabu, 15, 30)))
        val tepat = ms(rabu, 15, 20)
        assertTrue(PenghitungPengingat.hitung(data, p, tepat, tepat + 60_000, zona).isEmpty())
        assertEquals(1, PenghitungPengingat.hitung(data, p, tepat - 60_000, tepat, zona).size)
    }

    @Test
    fun tugasMengingatkanSoreSebelumDanPagiHariTenggat() {
        val selasaDepan = LocalDate.of(2026, 11, 10)
        val t = Tugas(id = 7, judul = "PR", jenis = JenisTugas.PR, tenggat = selasaDepan)
        val hasil = PenghitungPengingat.hitung(
            DataPengingat(tugas = listOf(t)), p, ms(selasaDepan.minusDays(1), 0), ms(selasaDepan, 23, 59), zona,
        )
        assertEquals(listOf(ms(selasaDepan.minusDays(1), 18, 0), ms(selasaDepan, 7, 0)), hasil.map { it.waktu })
        assertTrue(hasil.all { it.acara == selasaDepan.atStartOfDay(zona).toInstant().toEpochMilli() })
        assertTrue(hasil.all { it.jenis == JenisPengingat.TUGAS && it.idSumber == 7L })
    }

    @Test
    fun tugasSelesaiTanpaTenggatAtauDimatikanTidakMengingatkan() {
        val tenggat = LocalDate.of(2026, 11, 10)
        val jendelaDari = ms(LocalDate.of(2026, 11, 1), 0)
        val jendelaSampai = ms(LocalDate.of(2026, 11, 12), 0)
        fun hitung(t: Tugas, pengaturan: PengaturanPengingat = p) =
            PenghitungPengingat.hitung(DataPengingat(tugas = listOf(t)), pengaturan, jendelaDari, jendelaSampai, zona)

        assertTrue(hitung(Tugas(id = 1, judul = "A", tenggat = tenggat, selesai = true)).isEmpty())
        assertTrue(hitung(Tugas(id = 2, judul = "B", tenggat = null)).isEmpty())
        assertTrue(hitung(Tugas(id = 3, judul = "C", tenggat = tenggat), p.copy(tugasAktif = false)).isEmpty())
        assertEquals(2, hitung(Tugas(id = 4, judul = "D", tenggat = tenggat)).size)
    }

    @Test
    fun sekolahMengingatkanSebelumMasukPadaHariBerjamSekolah() {
        val data = DataPengingat(jamSekolah = listOf(JamSekolah(DayOfWeek.MONDAY, LocalTime.of(7, 0), LocalTime.of(14, 0))))
        val hasil = PenghitungPengingat.hitung(data, p, ms(senin.minusDays(1), 12), ms(senin.plusDays(2), 12), zona)
        val r = hasil.single()
        assertEquals(JenisPengingat.SEKOLAH, r.jenis)
        assertEquals(1L, r.idSumber)
        assertEquals(ms(senin, 6, 0), r.waktu)
        assertEquals(ms(senin, 7, 0), r.acara)
        assertTrue(PenghitungPengingat.hitung(data, p.copy(sekolahAktif = false), ms(senin.minusDays(1), 12), ms(senin.plusDays(2), 12), zona).isEmpty())
    }

    @Test
    fun pengingatDimatikanTidakMenghasilkanApaPun() {
        val data = DataPengingat(
            kegiatan = listOf(kegiatanSekali(1, rabu, 15, 30)),
            tugas = listOf(Tugas(id = 1, judul = "A", tenggat = kamis)),
        )
        val mati = p.copy(aktif = false)
        assertTrue(PenghitungPengingat.hitung(data, mati, ms(rabu, 0), ms(kamis, 23), zona).isEmpty())
        val tunda = listOf(Tunda(JenisPengingat.KEGIATAN, 1, ms(rabu, 15, 30), ms(rabu, 15, 40)))
        assertTrue(PenghitungPengingat.semua(data, tunda, mati, ms(rabu, 0), ms(kamis, 23), zona).isEmpty())
    }

    @Test
    fun hasilTerurutMenurutWaktu() {
        val data = DataPengingat(
            kegiatan = listOf(kegiatanSekali(1, rabu, 16), kegiatanSekali(2, rabu, 9), kegiatanSekali(3, rabu, 12)),
        )
        val hasil = PenghitungPengingat.hitung(data, p, ms(rabu, 0), ms(rabu, 23, 59), zona)
        assertEquals(listOf(2L, 3L, 1L), hasil.map { it.idSumber })
    }

    @Test
    fun tundaDigabungDenganPengingatBiasa() {
        val data = DataPengingat(kegiatan = listOf(kegiatanSekali(1, rabu, 15, 30)))
        val tunda = listOf(Tunda(JenisPengingat.KEGIATAN, 1, ms(rabu, 15, 30), ms(rabu, 15, 40)))
        val hasil = PenghitungPengingat.semua(data, tunda, p, ms(rabu, 0), ms(rabu, 23, 59), zona)
        assertEquals(listOf(ms(rabu, 15, 20), ms(rabu, 15, 40)), hasil.map { it.waktu })
        assertEquals(1, hasil.map { it.idNotifikasi }.distinct().size)
    }

    @Test
    fun idNotifikasiStabilDanTidakNegatif() {
        val a = Pengingat(JenisPengingat.KEGIATAN, 1, 100L, 200L)
        val b = Pengingat(JenisPengingat.KEGIATAN, 1, 999L, 200L)
        val c = Pengingat(JenisPengingat.KEGIATAN, 2, 100L, 200L)
        val d = Pengingat(JenisPengingat.TUGAS, 1, 100L, 200L)
        assertEquals(a.idNotifikasi, b.idNotifikasi)
        assertTrue(a.idNotifikasi != c.idNotifikasi)
        assertTrue(a.idNotifikasi != d.idNotifikasi)
        listOf(a, b, c, d, Pengingat(JenisPengingat.SEKOLAH, 7, -5L, Long.MAX_VALUE)).forEach {
            assertTrue(it.idNotifikasi >= 0)
        }
    }

    @Test
    fun berikutnyaMencariSetelahWaktuDanMemberiNullBilaKosong() {
        val mingguan = Kegiatan(
            id = 2, judul = "Les", hariMingguan = DayOfWeek.MONDAY,
            mulai = LocalTime.of(8, 0), selesai = LocalTime.of(9, 0),
        )
        val data = DataPengingat(kegiatan = listOf(mingguan))
        assertEquals(ms(senin.plusDays(7), 7, 50), ProsesPengingat.berikutnya(data, emptyList(), p, ms(rabu, 12), zona))
        assertNull(ProsesPengingat.berikutnya(DataPengingat(), emptyList(), p, ms(rabu, 12), zona))
        assertNull(ProsesPengingat.berikutnya(data, emptyList(), p.copy(aktif = false), ms(rabu, 12), zona))
    }

    @Test
    fun berikutnyaTidakPernahMengembalikanWaktuYangSudahLewatAtauSamaDenganSekarang() {
        val data = DataPengingat(kegiatan = listOf(kegiatanSekali(1, rabu, 15, 30)))
        assertNull(ProsesPengingat.berikutnya(data, emptyList(), p, ms(rabu, 15, 20), zona))
        assertEquals(ms(rabu, 15, 20), ProsesPengingat.berikutnya(data, emptyList(), p, ms(rabu, 15, 19), zona))
    }

    @Test
    fun berikutnyaHanyaMelihatDuaMingguKeDepan() {
        val jauh = DataPengingat(kegiatan = listOf(kegiatanSekali(1, rabu.plusDays(20), 10)))
        assertNull(ProsesPengingat.berikutnya(jauh, emptyList(), p, ms(rabu, 0), zona))
        val dekat = DataPengingat(kegiatan = listOf(kegiatanSekali(1, rabu.plusDays(10), 10)))
        assertEquals(ms(rabu.plusDays(10), 9, 50), ProsesPengingat.berikutnya(dekat, emptyList(), p, ms(rabu, 0), zona))
    }
}
