package id.asisten.app.domain.hanako

import id.asisten.app.domain.pengingat.ModePengingat
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EkspresiHanakoTest {

    @Test
    fun ekspresiMengikutiWaktuHari() {
        assertEquals(EkspresiHanako.SENYUM, PilihEkspresi.untukWaktu(LocalTime.of(4, 0)))
        assertEquals(EkspresiHanako.SENYUM, PilihEkspresi.untukWaktu(LocalTime.of(7, 30)))
        assertEquals(EkspresiHanako.NORMAL, PilihEkspresi.untukWaktu(LocalTime.of(12, 0)))
        assertEquals(EkspresiHanako.SENYUM, PilihEkspresi.untukWaktu(LocalTime.of(16, 0)))
        assertEquals(EkspresiHanako.NORMAL, PilihEkspresi.untukWaktu(LocalTime.of(19, 0)))
        assertEquals(EkspresiHanako.NORMAL, PilihEkspresi.untukWaktu(LocalTime.of(21, 59)))
    }

    @Test
    fun hanakoMengantukLarutMalamSampaiDiniHari() {
        assertEquals(EkspresiHanako.MENGANTUK, PilihEkspresi.untukWaktu(LocalTime.of(22, 0)))
        assertEquals(EkspresiHanako.MENGANTUK, PilihEkspresi.untukWaktu(LocalTime.of(23, 59)))
        assertEquals(EkspresiHanako.MENGANTUK, PilihEkspresi.untukWaktu(LocalTime.of(0, 0)))
        assertEquals(EkspresiHanako.MENGANTUK, PilihEkspresi.untukWaktu(LocalTime.of(3, 59)))
    }

    @Test
    fun layarPengingatMenyesuaikanModeDanIzin() {
        assertEquals(EkspresiHanako.SENYUM, PilihEkspresi.untukPengingat(ModePengingat.NORMAL, true))
        assertEquals(EkspresiHanako.BERPIKIR, PilihEkspresi.untukPengingat(ModePengingat.FOKUS, true))
        assertEquals(EkspresiHanako.MENGANTUK, PilihEkspresi.untukPengingat(ModePengingat.ISTIRAHAT, true))
    }

    @Test
    fun izinNotifikasiTertutupMengalahkanMode() {
        for (mode in ModePengingat.entries) {
            assertEquals(EkspresiHanako.BINGUNG, PilihEkspresi.untukPengingat(mode, false))
        }
    }

    @Test
    fun labelEkspresiUnikDanTidakKosong() {
        val label = EkspresiHanako.entries.map { it.label }
        assertEquals(12, label.size)
        assertEquals(label.size, label.toSet().size)
        assertTrue(label.all { it.isNotBlank() })
    }

    @Test
    fun ekspresiSaatGagalMenghubungiGemini() {
        assertEquals(EkspresiHanako.PANIK, PilihEkspresi.untukGalat(JenisGalat.SERVER))
        assertEquals(EkspresiHanako.SEDIH, PilihEkspresi.untukGalat(JenisGalat.KUOTA))
        assertEquals(EkspresiHanako.TERKEJUT, PilihEkspresi.untukGalat(JenisGalat.DIBLOKIR))
        for (j in listOf(JenisGalat.JARINGAN, JenisGalat.KUNCI_TIDAK_VALID, JenisGalat.TANPA_KUNCI, JenisGalat.KOSONG)) {
            assertEquals(EkspresiHanako.BINGUNG, PilihEkspresi.untukGalat(j))
        }
    }

    @Test
    fun wajahHeaderChatMengikutiKeadaan() {
        val pagi = LocalTime.of(8, 0)
        // menunggu jawaban mengalahkan semuanya
        assertEquals(EkspresiHanako.BERPIKIR, PilihEkspresi.untukChat(true, JenisGalat.SERVER, EkspresiHanako.TERTAWA, pagi))
        // galat mengalahkan suasana balasan terakhir
        assertEquals(EkspresiHanako.PANIK, PilihEkspresi.untukChat(false, JenisGalat.SERVER, EkspresiHanako.TERTAWA, pagi))
        // balasan terakhir
        assertEquals(EkspresiHanako.TERTAWA, PilihEkspresi.untukChat(false, null, EkspresiHanako.TERTAWA, pagi))
        // belum ada percakapan: wajah menurut waktu
        assertEquals(EkspresiHanako.SENYUM, PilihEkspresi.untukChat(false, null, null, pagi))
        assertEquals(EkspresiHanako.MENGANTUK, PilihEkspresi.untukChat(false, null, null, LocalTime.of(23, 0)))
    }
}
