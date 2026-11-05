package id.asisten.app.domain.pengingat

import id.asisten.app.domain.Prioritas
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KebijakanTest {

    private val zona = ZoneId.of("Asia/Jakarta")
    private val rabu = LocalDate.of(2026, 11, 4)

    private fun ms(jam: Int, menit: Int = 0): Long =
        rabu.atTime(jam, menit).atZone(zona).toInstant().toEpochMilli()

    private val p = PengaturanPengingat()

    // --- quiet hours ---

    @Test
    fun jamTenangMelewatiTengahMalam() {
        val mulai = 22 * 60
        val selesai = 6 * 60
        assertTrue(JamTenang.dalam(23 * 60, mulai, selesai))
        assertTrue(JamTenang.dalam(22 * 60, mulai, selesai))
        assertTrue(JamTenang.dalam(0, mulai, selesai))
        assertTrue(JamTenang.dalam(5 * 60 + 59, mulai, selesai))
        assertFalse(JamTenang.dalam(6 * 60, mulai, selesai))
        assertFalse(JamTenang.dalam(12 * 60, mulai, selesai))
        assertFalse(JamTenang.dalam(21 * 60 + 59, mulai, selesai))
    }

    @Test
    fun jamTenangDalamSatuHari() {
        assertTrue(JamTenang.dalam(13 * 60, 13 * 60, 15 * 60))
        assertTrue(JamTenang.dalam(14 * 60 + 59, 13 * 60, 15 * 60))
        assertFalse(JamTenang.dalam(15 * 60, 13 * 60, 15 * 60))
        assertFalse(JamTenang.dalam(12 * 60, 13 * 60, 15 * 60))
    }

    @Test
    fun jendelaKosongTidakPernahCocok() {
        assertFalse(JamTenang.dalam(0, 600, 600))
        assertFalse(JamTenang.dalam(600, 600, 600))
    }

    @Test
    fun menitHariMengikutiZonaWaktu() {
        assertEquals(14 * 60 + 50, JamTenang.menitHari(ms(14, 50), zona))
        assertEquals(0, JamTenang.menitHari(ms(0, 0), zona))
    }

    // --- policy ---

    @Test
    fun modeNormalBersuara() {
        val k = KebijakanNotifikasi.putuskan(Prioritas.SEDANG, p, ms(12), zona)
        assertEquals(Nada.BERSUARA, k.nada)
        assertNull(k.alasan)
    }

    @Test
    fun istirahatMembisukanSemuaTermasukPrioritasTinggi() {
        val istirahat = p.mulaiMode(ModePengingat.ISTIRAHAT, 30, ms(12))
        for (prioritas in Prioritas.entries) {
            val k = KebijakanNotifikasi.putuskan(prioritas, istirahat, ms(12, 10), zona)
            assertEquals(Nada.SENYAP, k.nada)
            assertEquals(AlasanSenyap.MODE_ISTIRAHAT, k.alasan)
        }
    }

    @Test
    fun fokusHanyaMembiarkanPrioritasTinggiBersuara() {
        val fokus = p.mulaiMode(ModePengingat.FOKUS, 45, ms(12))
        assertEquals(Nada.BERSUARA, KebijakanNotifikasi.putuskan(Prioritas.TINGGI, fokus, ms(12, 10), zona).nada)
        val sedang = KebijakanNotifikasi.putuskan(Prioritas.SEDANG, fokus, ms(12, 10), zona)
        assertEquals(Nada.SENYAP, sedang.nada)
        assertEquals(AlasanSenyap.MODE_FOKUS, sedang.alasan)
        assertEquals(Nada.SENYAP, KebijakanNotifikasi.putuskan(Prioritas.RENDAH, fokus, ms(12, 10), zona).nada)
    }

    @Test
    fun modeYangSudahBerakhirTidakBerlaku() {
        val fokus = p.mulaiMode(ModePengingat.FOKUS, 25, ms(12))
        assertEquals(Nada.SENYAP, KebijakanNotifikasi.putuskan(Prioritas.SEDANG, fokus, ms(12, 24), zona).nada)
        assertEquals(Nada.BERSUARA, KebijakanNotifikasi.putuskan(Prioritas.SEDANG, fokus, ms(12, 25), zona).nada)
        assertEquals(ModePengingat.NORMAL, fokus.modeEfektif(ms(12, 26)))
    }

    @Test
    fun jamTenangMembisukanBilaAktif() {
        val tenang = p.copy(jamTenangAktif = true)
        val malam = KebijakanNotifikasi.putuskan(Prioritas.TINGGI, tenang, ms(23), zona)
        assertEquals(Nada.SENYAP, malam.nada)
        assertEquals(AlasanSenyap.JAM_TENANG, malam.alasan)
        assertEquals(Nada.BERSUARA, KebijakanNotifikasi.putuskan(Prioritas.SEDANG, tenang, ms(12), zona).nada)
        assertEquals(Nada.BERSUARA, KebijakanNotifikasi.putuskan(Prioritas.SEDANG, p, ms(23), zona).nada)
    }

    @Test
    fun istirahatMenangAtasJamTenangSebagaiAlasan() {
        val gabungan = p.copy(jamTenangAktif = true).mulaiMode(ModePengingat.ISTIRAHAT, 60, ms(22, 30))
        assertEquals(AlasanSenyap.MODE_ISTIRAHAT, KebijakanNotifikasi.putuskan(Prioritas.SEDANG, gabungan, ms(23), zona).alasan)
    }

    // --- settings ---

    @Test
    fun sisaModeDibulatkanKeAtas() {
        val fokus = p.mulaiMode(ModePengingat.FOKUS, 25, ms(10))
        assertEquals(15L, fokus.sisaModeMenit(ms(10, 10)))
        assertEquals(15L, fokus.sisaModeMenit(ms(10, 10) + 30_000))
        assertEquals(1L, fokus.sisaModeMenit(ms(10, 24) + 30_000))
        assertEquals(0L, fokus.sisaModeMenit(ms(10, 25)))
        assertEquals(0L, p.sisaModeMenit(ms(10)))
    }

    @Test
    fun akhiriModeMengembalikanKeNormal() {
        val fokus = p.mulaiMode(ModePengingat.FOKUS, 25, ms(10)).akhiriMode()
        assertEquals(ModePengingat.NORMAL, fokus.mode)
        assertEquals(0L, fokus.modeSampai)
        assertEquals(ModePengingat.NORMAL, p.mulaiMode(ModePengingat.NORMAL, 25, ms(10)).mode)
        assertEquals(ModePengingat.NORMAL, p.mulaiMode(ModePengingat.FOKUS, 0, ms(10)).mode)
    }

    @Test
    fun normalisasiMembatasiNilaiDiLuarRentang() {
        val rusak = PengaturanPengingat(
            menitSebelumKegiatan = 5000, menitTugasHariH = 5000, menitTugasSebelum = -3,
            menitSebelumSekolah = -1, tundaMenit = 0, jamTenangMulai = -5, jamTenangSelesai = 99999,
            mode = ModePengingat.NORMAL, modeSampai = 123L,
        ).normalisasi()
        assertEquals(PengaturanPengingat.BATAS_SEBELUM, rusak.menitSebelumKegiatan)
        assertEquals(1439, rusak.menitTugasHariH)
        assertEquals(0, rusak.menitTugasSebelum)
        assertEquals(0, rusak.menitSebelumSekolah)
        assertEquals(1, rusak.tundaMenit)
        assertEquals(0, rusak.jamTenangMulai)
        assertEquals(1439, rusak.jamTenangSelesai)
        assertEquals(0L, rusak.modeSampai)
    }

    @Test
    fun normalisasiTidakMengubahNilaiYangSudahBenar() {
        assertEquals(p, p.normalisasi())
        val fokus = p.mulaiMode(ModePengingat.FOKUS, 25, ms(10))
        assertEquals(fokus, fokus.normalisasi())
    }

    @Test
    fun semuaPilihanDiUiBeradaDalamRentangYangSah() {
        assertTrue(PilihanPengingat.SEBELUM_KEGIATAN.all { it in 0..PengaturanPengingat.BATAS_SEBELUM })
        assertTrue(PilihanPengingat.SEBELUM_SEKOLAH.all { it in 0..PengaturanPengingat.BATAS_SEBELUM })
        assertTrue(PilihanPengingat.TUNDA.all { it in 1..120 })
        assertTrue(PilihanPengingat.DURASI_FOKUS.all { it > 0 })
        assertTrue(PilihanPengingat.DURASI_ISTIRAHAT.all { it > 0 })
        assertTrue(p.menitSebelumKegiatan in PilihanPengingat.SEBELUM_KEGIATAN)
        assertTrue(p.menitSebelumSekolah in PilihanPengingat.SEBELUM_SEKOLAH)
        assertTrue(p.tundaMenit in PilihanPengingat.TUNDA)
    }

    @Test
    fun penggeserJamMelingkarDiTengahMalam() {
        assertEquals(15, GeserMenit.maju(23 * 60 + 45))
        assertEquals(1420, GeserMenit.mundur(10))
        assertEquals(0, GeserMenit.maju(23 * 60 + 30))
        assertEquals(23 * 60 + 30, GeserMenit.mundur(0))
        assertEquals(600, GeserMenit.geser(600, 1440))
    }

    @Test
    fun formatJamDuaDigit() {
        assertEquals("00:00", GeserMenit.format(0))
        assertEquals("07:05", GeserMenit.format(7 * 60 + 5))
        assertEquals("23:59", GeserMenit.format(1439))
        assertEquals("23:59", GeserMenit.format(99999))
    }
}
