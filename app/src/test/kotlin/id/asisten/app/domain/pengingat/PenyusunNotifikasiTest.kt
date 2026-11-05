package id.asisten.app.domain.pengingat

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Piket
import id.asisten.app.domain.waktu.Waktu
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PenyusunNotifikasiTest {

    private val zona = ZoneId.of("Asia/Jakarta")
    private val senin = LocalDate.of(2026, 11, 2)
    private val rabu = LocalDate.of(2026, 11, 4)
    private val kamis = LocalDate.of(2026, 11, 5)

    private fun ms(tgl: LocalDate, jam: Int, menit: Int = 0, detik: Int = 0): Long =
        tgl.atTime(jam, menit, detik).atZone(zona).toInstant().toEpochMilli()

    private val rentang = Waktu.rentang(LocalTime.of(15, 30), LocalTime.of(16, 30))

    private val rapat = Kegiatan(
        id = 1, judul = "Rapat OSIS", tanggal = rabu,
        mulai = LocalTime.of(15, 30), selesai = LocalTime.of(16, 30),
        prioritas = Prioritas.TINGGI, catatan = "Bawa laptop",
    )

    private fun pengingatRapat() = Pengingat(JenisPengingat.KEGIATAN, 1, ms(rabu, 15, 20), ms(rabu, 15, 30))

    // --- activities ---

    @Test
    fun kegiatanMenyebutSisaWaktuRentangDanCatatan() {
        val isi = PenyusunNotifikasi.susun(pengingatRapat(), DataPengingat(kegiatan = listOf(rapat)), ms(rabu, 15, 20), zona)
        assertNotNull(isi)
        assertEquals("Rapat OSIS", isi!!.judul)
        assertEquals("Mulai 10 menit lagi · $rentang · Bawa laptop", isi.isi)
        assertEquals(Prioritas.TINGGI, isi.prioritas)
    }

    @Test
    fun kegiatanTerlambatDikirimMenyesuaikanTeks() {
        val data = DataPengingat(kegiatan = listOf(rapat.copy(catatan = "")))
        val p = pengingatRapat()
        assertEquals("Mulai sekarang · $rentang", PenyusunNotifikasi.susun(p, data, ms(rabu, 15, 29, 30), zona)!!.isi)
        assertEquals("Sudah dimulai · $rentang", PenyusunNotifikasi.susun(p, data, ms(rabu, 15, 45), zona)!!.isi)
    }

    @Test
    fun kegiatanMingguanDikenaliPadaHariYangBenar() {
        val mingguan = rapat.copy(tanggal = null, hariMingguan = DayOfWeek.WEDNESDAY)
        val data = DataPengingat(kegiatan = listOf(mingguan))
        assertNotNull(PenyusunNotifikasi.susun(pengingatRapat(), data, ms(rabu, 15, 20), zona))
        val kamisIni = Pengingat(JenisPengingat.KEGIATAN, 1, ms(kamis, 15, 20), ms(kamis, 15, 30))
        assertNull(PenyusunNotifikasi.susun(kamisIni, data, ms(kamis, 15, 20), zona))
    }

    @Test
    fun kegiatanYangDihapusAtauDipindahTidakMenghasilkanNotifikasi() {
        val p = pengingatRapat()
        val sekarang = ms(rabu, 15, 20)
        assertNull(PenyusunNotifikasi.susun(p, DataPengingat(), sekarang, zona))
        assertNull(PenyusunNotifikasi.susun(p, DataPengingat(kegiatan = listOf(rapat.copy(mulai = LocalTime.of(16, 0), selesai = LocalTime.of(17, 0)))), sekarang, zona))
        assertNull(PenyusunNotifikasi.susun(p, DataPengingat(kegiatan = listOf(rapat.copy(tanggal = kamis))), sekarang, zona))
    }

    // --- tasks ---

    private val pr = Tugas(
        id = 7, judul = "Soal halaman 40", jenis = JenisTugas.PR, mapel = "Matematika",
        tenggat = kamis, prioritas = Prioritas.SEDANG,
    )
    private val acaraPr = kamis.atStartOfDay(zona).toInstant().toEpochMilli()
    private fun pengingatPr(waktu: Long) = Pengingat(JenisPengingat.TUGAS, 7, waktu, acaraPr)

    @Test
    fun tugasMenyebutJenisMapelDanTenggat() {
        val data = DataPengingat(tugas = listOf(pr))
        val besok = PenyusunNotifikasi.susun(pengingatPr(ms(rabu, 18)), data, ms(rabu, 18), zona)!!
        assertEquals("PR: Soal halaman 40", besok.judul)
        assertEquals("Matematika · Tenggat besok", besok.isi)

        assertEquals("Matematika · Tenggat hari ini", PenyusunNotifikasi.susun(pengingatPr(ms(kamis, 7)), data, ms(kamis, 7), zona)!!.isi)
        assertEquals("Matematika · Tenggat Kamis, 05-11-2026", PenyusunNotifikasi.susun(pengingatPr(ms(senin, 7)), data, ms(senin, 7), zona)!!.isi)
        assertEquals(
            "Matematika · Tenggat sudah lewat (05-11-2026)",
            PenyusunNotifikasi.susun(pengingatPr(ms(kamis, 7)), data, ms(kamis.plusDays(1), 9), zona)!!.isi,
        )
    }

    @Test
    fun tugasTanpaMapelTidakMemuatSeparatorKosong() {
        val data = DataPengingat(tugas = listOf(pr.copy(mapel = "  ")))
        assertEquals("Tenggat besok", PenyusunNotifikasi.susun(pengingatPr(ms(rabu, 18)), data, ms(rabu, 18), zona)!!.isi)
    }

    @Test
    fun tugasYangSelesaiAtauDiubahTenggatnyaTidakDiumumkan() {
        val p = pengingatPr(ms(rabu, 18))
        val sekarang = ms(rabu, 18)
        assertNull(PenyusunNotifikasi.susun(p, DataPengingat(tugas = listOf(pr.copy(selesai = true))), sekarang, zona))
        assertNull(PenyusunNotifikasi.susun(p, DataPengingat(tugas = listOf(pr.copy(tenggat = kamis.plusDays(3)))), sekarang, zona))
        assertNull(PenyusunNotifikasi.susun(p, DataPengingat(tugas = listOf(pr.copy(tenggat = null))), sekarang, zona))
        assertNull(PenyusunNotifikasi.susun(p, DataPengingat(), sekarang, zona))
    }

    // --- school ---

    private val rabuSekolah = JamSekolah(DayOfWeek.WEDNESDAY, LocalTime.of(7, 0), LocalTime.of(14, 0))
    private fun pengingatSekolah() = Pengingat(JenisPengingat.SEKOLAH, 3, ms(rabu, 6, 0), ms(rabu, 7, 0))

    @Test
    fun sekolahMenyebutJamMasukDanPiketHariItu() {
        val data = DataPengingat(
            jamSekolah = listOf(rabuSekolah),
            piket = listOf(
                Piket(hari = DayOfWeek.WEDNESDAY, keterangan = "Menyapu"),
                Piket(hari = DayOfWeek.WEDNESDAY, keterangan = "Hapus papan"),
                Piket(hari = DayOfWeek.THURSDAY, keterangan = "Siram tanaman"),
            ),
        )
        val isi = PenyusunNotifikasi.susun(pengingatSekolah(), data, ms(rabu, 6, 0), zona)!!
        assertEquals("Persiapan sekolah", isi.judul)
        assertEquals("Masuk pukul 07:00 · 1 jam lagi · Piket: Menyapu, Hapus papan", isi.isi)
    }

    @Test
    fun sekolahTanpaPiketDanTerlambat() {
        val data = DataPengingat(jamSekolah = listOf(rabuSekolah))
        assertEquals("Masuk pukul 07:00 · 1 jam lagi", PenyusunNotifikasi.susun(pengingatSekolah(), data, ms(rabu, 6, 0), zona)!!.isi)
        assertEquals("Masuk pukul 07:00", PenyusunNotifikasi.susun(pengingatSekolah(), data, ms(rabu, 7, 10), zona)!!.isi)
    }

    @Test
    fun sekolahYangJamnyaBerubahAtauHilangTidakDiumumkan() {
        val sekarang = ms(rabu, 6, 0)
        assertNull(PenyusunNotifikasi.susun(pengingatSekolah(), DataPengingat(), sekarang, zona))
        val digeser = rabuSekolah.copy(masuk = LocalTime.of(7, 30))
        assertNull(PenyusunNotifikasi.susun(pengingatSekolah(), DataPengingat(jamSekolah = listOf(digeser)), sekarang, zona))
        val aneh = Pengingat(JenisPengingat.SEKOLAH, 9, ms(rabu, 6, 0), ms(rabu, 7, 0))
        assertNull(PenyusunNotifikasi.susun(aneh, DataPengingat(jamSekolah = listOf(rabuSekolah)), sekarang, zona))
    }

    // --- formatting ---

    @Test
    fun menitLagiMemakaiJamBilaPerlu() {
        assertEquals("5 menit lagi", PenyusunNotifikasi.menitLagi(5))
        assertEquals("59 menit lagi", PenyusunNotifikasi.menitLagi(59))
        assertEquals("1 jam lagi", PenyusunNotifikasi.menitLagi(60))
        assertEquals("1 jam 5 menit lagi", PenyusunNotifikasi.menitLagi(65))
        assertEquals("2 jam lagi", PenyusunNotifikasi.menitLagi(120))
    }

    @Test
    fun labelWaktuMemakaiNamaHariIndonesia() {
        assertEquals("Rab, 04-11-2026 14:50", PenyusunNotifikasi.labelWaktu(ms(rabu, 14, 50), zona))
        assertEquals("Sen, 02-11-2026 06:00", PenyusunNotifikasi.labelWaktu(ms(senin, 6, 0), zona))
    }

    // --- upcoming list ---

    @Test
    fun daftarAkanDatangTerurutDenganTeksDanDibatasi() {
        val data = DataPengingat(
            kegiatan = listOf(
                rapat,
                rapat.copy(id = 2, judul = "Latihan", mulai = LocalTime.of(9, 0), selesai = LocalTime.of(10, 0), catatan = ""),
                rapat.copy(id = 3, judul = "Les", tanggal = kamis, mulai = LocalTime.of(8, 0), selesai = LocalTime.of(9, 0), catatan = ""),
            ),
        )
        val daftar = PenyusunNotifikasi.akanDatang(data, emptyList(), PengaturanPengingat(), ms(rabu, 0), zona)
        assertEquals(listOf("Latihan", "Rapat OSIS", "Les"), daftar.map { it.judul })
        assertTrue(daftar.all { it.isi.contains("menit lagi") })
        assertEquals(2, PenyusunNotifikasi.akanDatang(data, emptyList(), PengaturanPengingat(), ms(rabu, 0), zona, maks = 2).size)
    }

    @Test
    fun daftarAkanDatangKosongBilaTidakAdaApaPun() {
        assertTrue(PenyusunNotifikasi.akanDatang(DataPengingat(), emptyList(), PengaturanPengingat(), ms(rabu, 0), zona).isEmpty())
    }

    // --- snooze storage ---

    @Test
    fun tundaCodecBolakBalikTanpaKehilanganData() {
        val daftar = listOf(
            Tunda(JenisPengingat.KEGIATAN, 1, 100L, 200L),
            Tunda(JenisPengingat.TUGAS, 7, 300L, 400L),
        )
        assertEquals(daftar, TundaCodec.decode(TundaCodec.encode(daftar)))
        assertEquals("", TundaCodec.encode(emptyList()))
        assertTrue(TundaCodec.decode(null).isEmpty())
        assertTrue(TundaCodec.decode("").isEmpty())
    }

    @Test
    fun tundaCodecMembuangEntriRusakBukanCrash() {
        val teks = "KEGIATAN,1,100,200;ASING,1,2,3;TUGAS,x,2,3;SEKOLAH,1,2;;TUGAS,7,300,400"
        assertEquals(
            listOf(Tunda(JenisPengingat.KEGIATAN, 1, 100L, 200L), Tunda(JenisPengingat.TUGAS, 7, 300L, 400L)),
            TundaCodec.decode(teks),
        )
    }

    @Test
    fun menundaLagiItemYangSamaMenggantikanTundaLama() {
        val lama = Tunda(JenisPengingat.TUGAS, 7, 300L, 1000L)
        val lain = Tunda(JenisPengingat.KEGIATAN, 1, 100L, 1500L)
        val baru = Tunda(JenisPengingat.TUGAS, 7, 300L, 2000L)
        assertEquals(listOf(lain, baru), TundaCodec.tambah(listOf(lama, lain), baru))
    }

    @Test
    fun daftarTundaTidakTumbuhTanpaBatas() {
        var daftar = emptyList<Tunda>()
        for (i in 1..(TundaCodec.MAKS + 20)) {
            daftar = TundaCodec.tambah(daftar, Tunda(JenisPengingat.KEGIATAN, i.toLong(), 1L, i.toLong()))
        }
        assertEquals(TundaCodec.MAKS, daftar.size)
        assertEquals((TundaCodec.MAKS + 20).toLong(), daftar.last().idSumber)
        assertEquals(TundaCodec.MAKS, TundaCodec.decode(TundaCodec.encode(daftar)).size)
    }
}
