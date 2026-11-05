package id.asisten.app.domain.pengingat

import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.jadwal.Kegiatan
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProsesPengingatTest {

    private val zona = ZoneId.of("Asia/Jakarta")
    private val rabu = LocalDate.of(2026, 11, 4)

    private fun ms(tgl: LocalDate, jam: Int, menit: Int = 0): Long =
        tgl.atTime(jam, menit).atZone(zona).toInstant().toEpochMilli()

    private val kegiatan = Kegiatan(
        id = 1, judul = "Rapat", tanggal = rabu, mulai = LocalTime.of(15, 30), selesai = LocalTime.of(16, 30),
    )
    private val data = DataPengingat(kegiatan = listOf(kegiatan))
    private val p = PengaturanPengingat()

    @Test
    fun mengirimYangJatuhTempoAntaraPenandaDanSekarang() {
        val hasil = ProsesPengingat.jatuhTempo(data, emptyList(), p, ms(rabu, 15, 0), ms(rabu, 15, 25), zona)
        assertEquals(listOf(ms(rabu, 15, 20)), hasil.map { it.waktu })
    }

    @Test
    fun tidakMengirimBilaBelumPernahBerjalan() {
        assertTrue(ProsesPengingat.jatuhTempo(data, emptyList(), p, 0L, ms(rabu, 15, 25), zona).isEmpty())
    }

    @Test
    fun tidakMengirimBilaJamSistemMundur() {
        assertTrue(ProsesPengingat.jatuhTempo(data, emptyList(), p, ms(rabu, 16, 0), ms(rabu, 15, 25), zona).isEmpty())
        assertTrue(ProsesPengingat.jatuhTempo(data, emptyList(), p, ms(rabu, 15, 25), ms(rabu, 15, 25), zona).isEmpty())
    }

    @Test
    fun pengingatYangTerlaluLamaDibuangBukanDikirimTerlambat() {
        // Reminder at 15:20; delivered 2h10m later it would be noise, 1h40m later it is still useful.
        assertTrue(ProsesPengingat.jatuhTempo(data, emptyList(), p, ms(rabu, 10, 0), ms(rabu, 17, 30), zona).isEmpty())
        assertEquals(
            1,
            ProsesPengingat.jatuhTempo(data, emptyList(), p, ms(rabu, 10, 0), ms(rabu, 17, 0), zona).size,
        )
    }

    @Test
    fun yangSudahDiprosesTidakDikirimLagi() {
        val pertama = ProsesPengingat.jatuhTempo(data, emptyList(), p, ms(rabu, 15, 0), ms(rabu, 15, 25), zona)
        assertEquals(1, pertama.size)
        // The scheduler stores `sekarang` as the new marker, so the same window yields nothing.
        val kedua = ProsesPengingat.jatuhTempo(data, emptyList(), p, ms(rabu, 15, 25), ms(rabu, 15, 26), zona)
        assertTrue(kedua.isEmpty())
    }

    @Test
    fun satuItemDenganDuaPengingatJatuhTempoHanyaMengirimYangTerbaru() {
        val tenggat = LocalDate.of(2026, 11, 10)
        val dekat = p.copy(menitTugasSebelum = 23 * 60 + 30, menitTugasHariH = 30)
        val dataTugas = DataPengingat(tugas = listOf(Tugas(id = 5, judul = "PR", tenggat = tenggat)))
        val hasil = ProsesPengingat.jatuhTempo(
            dataTugas, emptyList(), dekat, ms(tenggat.minusDays(1), 23, 0), ms(tenggat, 1, 0), zona,
        )
        assertEquals(listOf(ms(tenggat, 0, 30)), hasil.map { it.waktu })
    }

    @Test
    fun tundaYangJatuhTempoIkutDikirim() {
        val tunda = listOf(Tunda(JenisPengingat.KEGIATAN, 1, ms(rabu, 15, 30), ms(rabu, 15, 40)))
        val hasil = ProsesPengingat.jatuhTempo(data, tunda, p, ms(rabu, 15, 30), ms(rabu, 15, 45), zona)
        assertEquals(listOf(ms(rabu, 15, 40)), hasil.map { it.waktu })
        assertEquals(JenisPengingat.KEGIATAN, hasil.single().jenis)
    }

    @Test
    fun tundaYangBelumJatuhTempoTidakDikirim() {
        val tunda = listOf(Tunda(JenisPengingat.KEGIATAN, 1, ms(rabu, 15, 30), ms(rabu, 15, 50)))
        assertTrue(ProsesPengingat.jatuhTempo(data, tunda, p, ms(rabu, 15, 30), ms(rabu, 15, 45), zona).isEmpty())
    }

    @Test
    fun pengingatDimatikanTidakMengirimApaPun() {
        val hasil = ProsesPengingat.jatuhTempo(data, emptyList(), p.copy(aktif = false), ms(rabu, 15, 0), ms(rabu, 15, 25), zona)
        assertTrue(hasil.isEmpty())
    }
}
