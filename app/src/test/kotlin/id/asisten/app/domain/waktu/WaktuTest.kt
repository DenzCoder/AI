package id.asisten.app.domain.waktu

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WaktuTest {

    @Test
    fun parseJamMenerimaBeberapaFormat() {
        assertEquals(LocalTime.of(5, 30), Waktu.parseJam("05:30"))
        assertEquals(LocalTime.of(5, 30), Waktu.parseJam("5:30"))
        assertEquals(LocalTime.of(5, 30), Waktu.parseJam("05.30"))
        assertEquals(LocalTime.of(5, 30), Waktu.parseJam("  5.30  "))
        assertEquals(LocalTime.of(23, 59), Waktu.parseJam("23:59"))
        assertEquals(LocalTime.of(0, 0), Waktu.parseJam("00:00"))
    }

    @Test
    fun parseJamMenolakInputTidakValid() {
        assertNull(Waktu.parseJam(""))
        assertNull(Waktu.parseJam("abc"))
        assertNull(Waktu.parseJam("24:00"))
        assertNull(Waktu.parseJam("12:60"))
        assertNull(Waktu.parseJam("530"))
        assertNull(Waktu.parseJam("5:3"))
        assertNull(Waktu.parseJam("05:30:00"))
    }

    @Test
    fun formatJamSelaluDuaDigit() {
        assertEquals("05:07", Waktu.formatJam(LocalTime.of(5, 7)))
        assertEquals("23:00", Waktu.formatJam(LocalTime.of(23, 0)))
        assertEquals("07:00–14:30", Waktu.rentang(LocalTime.of(7, 0), LocalTime.of(14, 30)))
    }

    @Test
    fun parseTanggalMenerimaStripDanGarisMiring() {
        assertEquals(LocalDate.of(2026, 11, 2), Waktu.parseTanggal("02-11-2026"))
        assertEquals(LocalDate.of(2026, 11, 2), Waktu.parseTanggal("2/11/2026"))
        assertEquals(LocalDate.of(2026, 1, 31), Waktu.parseTanggal(" 31-1-2026 "))
    }

    @Test
    fun parseTanggalMenolakTanggalTidakAda() {
        assertNull(Waktu.parseTanggal("31-02-2026"))
        assertNull(Waktu.parseTanggal("29-02-2027"))
        assertNull(Waktu.parseTanggal("00-01-2026"))
        assertNull(Waktu.parseTanggal("2026-11-02"))
        assertNull(Waktu.parseTanggal("besok"))
        assertNull(Waktu.parseTanggal(""))
    }

    @Test
    fun tanggalKabisatDiterima() {
        assertEquals(LocalDate.of(2028, 2, 29), Waktu.parseTanggal("29-02-2028"))
    }

    @Test
    fun formatTanggalDanTanggalPanjang() {
        val senin = LocalDate.of(2026, 11, 2)
        assertEquals("02-11-2026", Waktu.formatTanggal(senin))
        assertEquals("Senin, 2 November 2026", Waktu.tanggalPanjang(senin))
        assertEquals(senin, Waktu.parseTanggal(Waktu.formatTanggal(senin)))
    }

    @Test
    fun namaHariBahasaIndonesiaTidakBergantungLocale() {
        assertEquals("Senin", NamaHari.untuk(DayOfWeek.MONDAY))
        assertEquals("Jumat", NamaHari.untuk(DayOfWeek.FRIDAY))
        assertEquals("Minggu", NamaHari.untuk(DayOfWeek.SUNDAY))
        assertEquals("Sel", NamaHari.singkat(DayOfWeek.TUESDAY))
    }
}
