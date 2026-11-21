package id.asisten.app.domain.pengaturan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KontrasWarnaTest {

    @Test
    fun hitamDiAtasPutihAdalah21() {
        assertEquals(21.0, KontrasWarna.rasio(0xFF000000, 0xFFFFFFFF), 0.01)
        assertEquals(1.0, KontrasWarna.rasio(0xFF7E57C2, 0xFF7E57C2), 0.0001)
    }

    @Test
    fun luminansiMengikutiRumusWcag() {
        // Reference values from the WCAG 2 relative-luminance formula (sRGB linearisation with exponent 2.4).
        assertEquals(0.0, KontrasWarna.luminansi(0xFF000000), 0.0001)
        assertEquals(1.0, KontrasWarna.luminansi(0xFFFFFFFF), 0.0001)
        assertEquals(0.1845, KontrasWarna.luminansi(0xFF777777), 0.0005)
        assertEquals(0.2126, KontrasWarna.luminansi(0xFFFF0000), 0.0005)
        assertEquals(0.7152, KontrasWarna.luminansi(0xFF00FF00), 0.0005)
        assertEquals(0.0722, KontrasWarna.luminansi(0xFF0000FF), 0.0005)
        assertEquals(4.48, KontrasWarna.rasio(0xFF777777, 0xFFFFFFFF), 0.02)
    }

    @Test
    fun alfaDiabaikan() {
        assertEquals(KontrasWarna.luminansi(0xFFF28C38), KontrasWarna.luminansi(0x00F28C38), 0.0)
    }

    @Test
    fun latarGelapMemakaiTeksPutihDanLatarTerangMemakaiTintaGelap() {
        assertFalse(KontrasWarna.pakaiTintaGelap(0xFF000000))
        assertFalse(KontrasWarna.pakaiTintaGelap(0xFF1F2A52))
        assertTrue(KontrasWarna.pakaiTintaGelap(0xFFFFFFFF))
        assertTrue(KontrasWarna.pakaiTintaGelap(0xFFFFD83D))
    }

    @Test
    fun setiapAksenMemilihPasanganYangLebihJelasDanCukupTerbaca() {
        for (a in Aksen.entries) {
            val gelap = KontrasWarna.pakaiTintaGelap(a.argb)
            val dipakai = if (gelap) KontrasWarna.rasio(a.argb, KontrasWarna.TINTA_GELAP) else KontrasWarna.rasio(a.argb, 0xFFFFFFFF)
            val lain = if (gelap) KontrasWarna.rasio(a.argb, 0xFFFFFFFF) else KontrasWarna.rasio(a.argb, KontrasWarna.TINTA_GELAP)
            assertTrue("${a.name}: pilihan tidak lebih baik", dipakai >= lain)
            assertTrue("${a.name}: rasio $dipakai di bawah 3 (batas teks besar/tebal WCAG)", dipakai >= 3.0)
        }
    }

    @Test
    fun jinggaHijauDanBiruMemakaiTintaGelapSedangkanUnguMemakaiPutih() {
        assertTrue(KontrasWarna.pakaiTintaGelap(Aksen.JINGGA.argb))
        assertTrue(KontrasWarna.pakaiTintaGelap(Aksen.HIJAU.argb))
        assertTrue(KontrasWarna.pakaiTintaGelap(Aksen.BIRU.argb))
        assertFalse(KontrasWarna.pakaiTintaGelap(Aksen.UNGU.argb))
    }
}
