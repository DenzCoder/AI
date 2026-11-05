package id.asisten.app.domain.pengingat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelPengingatTest {

    private val p = PengaturanPengingat()

    @Test
    fun selangMenyebutTepatWaktuAtauSebelum() {
        assertEquals("Tepat waktu", LabelPengingat.selang(0))
        assertEquals("10 menit sebelum", LabelPengingat.selang(10))
        assertEquals("1 jam sebelum", LabelPengingat.selang(60))
        assertEquals("1 jam 30 menit sebelum", LabelPengingat.selang(90))
    }

    @Test
    fun semuaPilihanPunyaLabelYangWajar() {
        val semua = PilihanPengingat.SEBELUM_KEGIATAN.map { LabelPengingat.selang(it) } +
            PilihanPengingat.SEBELUM_SEKOLAH.map { LabelPengingat.selang(it) } +
            (PilihanPengingat.TUNDA + PilihanPengingat.DURASI_FOKUS + PilihanPengingat.DURASI_ISTIRAHAT)
                .map { LabelPengingat.durasi(it) }
        assertTrue(semua.all { it.isNotBlank() && !it.startsWith("0") })
    }

    @Test
    fun statusModeMenyebutSisaWaktu() {
        assertEquals("Tidak ada mode yang aktif.", LabelPengingat.statusMode(p, 1_000L))
        val fokus = p.mulaiMode(ModePengingat.FOKUS, 25, 0L)
        assertEquals("Mode Fokus aktif, 25 menit lagi.", LabelPengingat.statusMode(fokus, 0L))
        assertEquals("Mode Fokus aktif, 15 menit lagi.", LabelPengingat.statusMode(fokus, 10 * 60_000L))
        val istirahat = p.mulaiMode(ModePengingat.ISTIRAHAT, 90, 0L)
        assertEquals("Mode Istirahat aktif, 1 jam 30 menit lagi.", LabelPengingat.statusMode(istirahat, 0L))
        assertEquals("Tidak ada mode yang aktif.", LabelPengingat.statusMode(fokus, 26 * 60_000L))
    }

    @Test
    fun hasilUjiMenjelaskanKenapaSenyap() {
        assertEquals(
            "Notifikasi uji dikirim. Kamu seharusnya mendengar suaranya.",
            LabelPengingat.hasilUji(Keputusan(Nada.BERSUARA)),
        )
        assertEquals(
            "Notifikasi uji dikirim tanpa suara karena mode Istirahat sedang aktif.",
            LabelPengingat.hasilUji(Keputusan(Nada.SENYAP, AlasanSenyap.MODE_ISTIRAHAT)),
        )
        assertEquals(
            "Notifikasi uji dikirim tanpa suara karena sekarang jam tenang.",
            LabelPengingat.hasilUji(Keputusan(Nada.SENYAP, AlasanSenyap.JAM_TENANG)),
        )
        assertTrue(LabelPengingat.hasilUji(Keputusan(Nada.SENYAP)).contains("tanpa suara"))
    }

    @Test
    fun sapaanHanakoMengikutiKeadaan() {
        assertTrue(LabelPengingat.sapaanHanako(p, 0L, false).contains("dimatikan"))
        assertEquals("Pengingat sedang dimatikan.", LabelPengingat.sapaanHanako(p.copy(aktif = false), 0L, true))
        assertEquals("Aku akan mengingatkanmu sesuai jadwal.", LabelPengingat.sapaanHanako(p, 0L, true))
        val fokus = p.mulaiMode(ModePengingat.FOKUS, 25, 0L)
        assertEquals("Fokus ya. Mode Fokus aktif, 25 menit lagi.", LabelPengingat.sapaanHanako(fokus, 0L, true))
        val istirahat = p.mulaiMode(ModePengingat.ISTIRAHAT, 30, 0L)
        assertTrue(LabelPengingat.sapaanHanako(istirahat, 0L, true).startsWith("Istirahat dulu."))
    }

    @Test
    fun namaModeDanAlasan() {
        assertEquals(listOf("Normal", "Fokus", "Istirahat"), ModePengingat.entries.map { it.namaMode() })
        assertTrue(AlasanSenyap.entries.all { it.keterangan().isNotBlank() })
    }
}
