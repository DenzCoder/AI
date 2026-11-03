package id.asisten.app.domain.aktivitas

import id.asisten.app.domain.Prioritas
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AktivitasTest {

    private val hariIni = LocalDate.of(2026, 11, 2)

    private fun tugas(
        id: Long,
        judul: String = "T$id",
        tenggat: LocalDate? = null,
        prioritas: Prioritas = Prioritas.SEDANG,
        selesai: Boolean = false,
    ) = Tugas(id = id, judul = judul, tenggat = tenggat, prioritas = prioritas, selesai = selesai)

    @Test
    fun statusTenggatSemuaKemungkinan() {
        assertEquals(StatusTenggat.TANPA, tugas(1).statusTenggat(hariIni))
        assertEquals(StatusTenggat.LEWAT, tugas(1, tenggat = hariIni.minusDays(1)).statusTenggat(hariIni))
        assertEquals(StatusTenggat.HARI_INI, tugas(1, tenggat = hariIni).statusTenggat(hariIni))
        assertEquals(StatusTenggat.BESOK, tugas(1, tenggat = hariIni.plusDays(1)).statusTenggat(hariIni))
        assertEquals(StatusTenggat.MENDATANG, tugas(1, tenggat = hariIni.plusDays(2)).statusTenggat(hariIni))
    }

    @Test
    fun urutkanTugasMenaruhYangSelesaiPalingBawah() {
        val hasil = Aktivitas.urutkanTugas(
            listOf(tugas(1, selesai = true, tenggat = hariIni), tugas(2, tenggat = hariIni.plusDays(9))),
        )
        assertEquals(listOf(2L, 1L), hasil.map { it.id })
    }

    @Test
    fun urutkanTugasMenurutTenggatDenganYangTanpaTenggatTerakhir() {
        val hasil = Aktivitas.urutkanTugas(
            listOf(
                tugas(1),
                tugas(2, tenggat = hariIni.plusDays(5)),
                tugas(3, tenggat = hariIni.plusDays(1)),
            ),
        )
        assertEquals(listOf(3L, 2L, 1L), hasil.map { it.id })
    }

    @Test
    fun urutkanTugasPrioritasTinggiDuluBilaTenggatSama() {
        val hasil = Aktivitas.urutkanTugas(
            listOf(
                tugas(1, tenggat = hariIni, prioritas = Prioritas.RENDAH),
                tugas(2, tenggat = hariIni, prioritas = Prioritas.TINGGI),
                tugas(3, tenggat = hariIni, prioritas = Prioritas.SEDANG),
            ),
        )
        assertEquals(listOf(2L, 3L, 1L), hasil.map { it.id })
    }

    @Test
    fun kemajuanTujuanTanpaTonggakAdalahNolSampaiSelesai() {
        assertEquals(0f, Tujuan(judul = "A").kemajuan, 0.0001f)
        assertEquals(1f, Tujuan(judul = "A", selesai = true).kemajuan, 0.0001f)
    }

    @Test
    fun kemajuanTujuanDihitungDariTonggak() {
        val t = Tujuan(
            judul = "A",
            tonggak = listOf(
                Tonggak(judul = "1", selesai = true),
                Tonggak(judul = "2", selesai = false),
                Tonggak(judul = "3", selesai = true),
                Tonggak(judul = "4", selesai = false),
            ),
        )
        assertEquals(0.5f, t.kemajuan, 0.0001f)
    }

    @Test
    fun tujuanSelesaiSelaluSeratusPersenWalauTonggakBelumSemua() {
        val t = Tujuan(judul = "A", selesai = true, tonggak = listOf(Tonggak(judul = "1", selesai = false)))
        assertEquals(1f, t.kemajuan, 0.0001f)
    }

    @Test
    fun riwayatPencapaianHanyaYangSelesaiDariYangTerbaru() {
        val hasil = Aktivitas.riwayatPencapaian(
            listOf(
                Tujuan(id = 1, judul = "lama", selesai = true, selesaiPada = 100),
                Tujuan(id = 2, judul = "belum"),
                Tujuan(id = 3, judul = "baru", selesai = true, selesaiPada = 900),
            ),
        )
        assertEquals(listOf(3L, 1L), hasil.map { it.id })
        assertTrue(Aktivitas.riwayatPencapaian(emptyList()).isEmpty())
    }
}
