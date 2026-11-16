package id.asisten.app.domain.hanako

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.hanako.DataUji.hariIni
import id.asisten.app.domain.pengingat.ModePengingat
import id.asisten.app.domain.pengingat.PengaturanPengingat
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerencanaAksiTest {

    private val data = DataUji.data()

    private fun siap(aksi: AksiHanako, d: DataSaatIni = data): LangkahAksi {
        val hasil = PerencanaAksi.rencanakan(aksi, d)
        assertTrue("diharapkan siap, tetapi $hasil", hasil is HasilRencana.Siap)
        return (hasil as HasilRencana.Siap).langkah
    }

    private fun gagal(aksi: AksiHanako, d: DataSaatIni = data): String {
        val hasil = PerencanaAksi.rencanakan(aksi, d)
        assertTrue("diharapkan gagal, tetapi $hasil", hasil is HasilRencana.Gagal)
        return (hasil as HasilRencana.Gagal).alasan
    }

    @Test
    fun tambahSelaluMenjadiBarisBaruDenganIdNol() {
        val tugas = siap(AksiHanako.TambahTugas(DataUji.tugasPr.copy(id = 1))) as LangkahAksi.SimpanTugas
        assertEquals(0L, tugas.tugas.id)
        val kegiatan = siap(AksiHanako.TambahKegiatan(DataUji.lesInggris.copy(id = 10))) as LangkahAksi.SimpanKegiatan
        assertEquals(0L, kegiatan.kegiatan.id)
        val pelajaran = siap(AksiHanako.TambahPelajaran(DataUji.matematika.copy(id = 20, istirahat = true))) as LangkahAksi.SimpanPelajaran
        assertEquals(0L, pelajaran.pelajaran.id)
        assertEquals(false, pelajaran.pelajaran.istirahat)
    }

    @Test
    fun ubahDiterapkanPadaDataTerkiniSehinggaEditanPenggunaTidakHilang() {
        val usulan = AksiHanako.UbahTugas(1, PerubahanTugas(prioritas = Prioritas.RENDAH))
        // Sementara kartu usulan tampil, pengguna sendiri mengubah catatan dan judul tugas itu.
        val diubahPengguna = DataUji.tugasPr.copy(catatan = "catatan baru dari pengguna", judul = "PR Matematika bab 4")
        val langkah = siap(usulan, DataUji.data(tugas = listOf(diubahPengguna))) as LangkahAksi.SimpanTugas
        assertEquals(Prioritas.RENDAH, langkah.tugas.prioritas)
        assertEquals("catatan baru dari pengguna", langkah.tugas.catatan)
        assertEquals("PR Matematika bab 4", langkah.tugas.judul)
        assertEquals(1L, langkah.tugas.id)
    }

    @Test
    fun ubahKegiatanDanPelajaranMemakaiIdYangSama() {
        val k = siap(AksiHanako.UbahKegiatan(10, PerubahanKegiatan(mulai = LocalTime.of(15, 0)))) as LangkahAksi.SimpanKegiatan
        assertEquals(10L, k.kegiatan.id)
        assertEquals(LocalTime.of(15, 0), k.kegiatan.mulai)
        assertEquals(DayOfWeek.WEDNESDAY, k.kegiatan.hariMingguan)
        val p = siap(AksiHanako.UbahPelajaran(20, PerubahanPelajaran(ruang = "7"))) as LangkahAksi.SimpanPelajaran
        assertEquals("7", p.pelajaran.ruang)
        assertEquals("Bu Ani", p.pelajaran.guru)
    }

    @Test
    fun itemYangSudahDihapusPenggunaMembuatLangkahGagalBukanMembuatBaru() {
        val kosong = DataUji.data(tugas = emptyList(), kegiatan = emptyList(), pelajaran = emptyList())
        for (aksi in listOf(
            AksiHanako.UbahTugas(1, PerubahanTugas(judul = "x")),
            AksiHanako.TandaiTugas(1, true),
            AksiHanako.HapusTugas(1, "x"),
            AksiHanako.UbahKegiatan(10, PerubahanKegiatan(judul = "x")),
            AksiHanako.HapusKegiatan(10, "x"),
            AksiHanako.UbahPelajaran(20, PerubahanPelajaran(nama = "x")),
            AksiHanako.HapusPelajaran(20, "x"),
        )) {
            assertTrue(aksi.toString(), gagal(aksi, kosong).contains("sudah tidak ada"))
        }
    }

    @Test
    fun hapusYangAdaMenghasilkanLangkahHapus() {
        assertEquals(LangkahAksi.HapusTugas(1), siap(AksiHanako.HapusTugas(1, "PR Matematika")))
        assertEquals(LangkahAksi.HapusKegiatan(10), siap(AksiHanako.HapusKegiatan(10, "Les Inggris")))
        assertEquals(LangkahAksi.HapusPelajaran(20), siap(AksiHanako.HapusPelajaran(20, "Matematika")))
    }

    @Test
    fun slotIstirahatTidakBisaDihapusMeskiIdnyaDikirim() {
        assertTrue(gagal(AksiHanako.HapusPelajaran(21, "Istirahat")).contains("sudah tidak ada"))
        assertTrue(gagal(AksiHanako.UbahPelajaran(21, PerubahanPelajaran(nama = "x"))).contains("sudah tidak ada"))
    }

    @Test
    fun tandaiMembawaTugasTerkini() {
        val l = siap(AksiHanako.TandaiTugas(1, true)) as LangkahAksi.TandaiTugas
        assertEquals(DataUji.tugasPr, l.tugas)
        assertEquals(true, l.selesai)
    }

    @Test
    fun kegiatanSekaliYangTanggalnyaSudahLewatSaatDisetujuiGagal() {
        val kemarin = DataUji.lesInggris.copy(hariMingguan = null, tanggal = hariIni.minusDays(1))
        assertTrue(gagal(AksiHanako.TambahKegiatan(kemarin)).contains("sudah lewat"))
        val hariIniOk = DataUji.lesInggris.copy(hariMingguan = null, tanggal = hariIni)
        siap(AksiHanako.TambahKegiatan(hariIniOk))
    }

    @Test
    fun jamYangMenjadiTidakSahKarenaEditanPenggunaDitolak() {
        // Usulan: mulai 15:00 (sah saat dibuat karena selesai 17:30). Pengguna lalu mengubah selesai menjadi 14:30.
        val usulan = AksiHanako.UbahKegiatan(10, PerubahanKegiatan(mulai = LocalTime.of(15, 0)))
        val diubah = DataUji.lesInggris.copy(selesai = LocalTime.of(14, 30))
        assertTrue(gagal(usulan, DataUji.data(kegiatan = listOf(diubah))).contains("setelah jam mulai"))
        val usulanP = AksiHanako.UbahPelajaran(20, PerubahanPelajaran(mulai = LocalTime.of(8, 0)))
        val diubahP = DataUji.matematika.copy(selesai = LocalTime.of(7, 30))
        assertTrue(gagal(usulanP, DataUji.data(pelajaran = listOf(diubahP))).contains("setelah jam mulai"))
    }

    @Test
    fun langkahPengingatDanModeMeneruskanAksi() {
        val ubah = PerubahanPengingat(menitSebelumKegiatan = 0)
        assertEquals(LangkahAksi.UbahPengingat(ubah), siap(AksiHanako.AturPengingat(ubah)))
        val mulai = AksiHanako.MulaiMode(ModePengingat.FOKUS, 25)
        assertEquals(LangkahAksi.MulaiMode(mulai), siap(mulai))
        assertEquals(LangkahAksi.AkhiriMode, siap(AksiHanako.AkhiriMode))
    }

    // ---------- fungsi terapkan ----------

    @Test
    fun terapkanTugasHanyaMengubahBagianYangDiisi() {
        val baru = DataUji.tugasPr.terapkan(PerubahanTugas(mapel = "Fisika"))
        assertEquals("Fisika", baru.mapel)
        assertEquals(DataUji.tugasPr.copy(mapel = "Fisika"), baru)
        assertEquals(null, DataUji.tugasPr.terapkan(PerubahanTugas(hapusTenggat = true)).tenggat)
    }

    @Test
    fun terapkanPengingatMenormalkanNilai() {
        val dasar = PengaturanPengingat()
        val baru = dasar.terapkan(PerubahanPengingat(menitSebelumKegiatan = 5000, tundaMenit = 0, jamTenangMulai = 99999))
        assertEquals(PengaturanPengingat.BATAS_SEBELUM, baru.menitSebelumKegiatan)
        assertEquals(1, baru.tundaMenit)
        assertEquals(PengaturanPengingat.MENIT_SEHARI - 1, baru.jamTenangMulai)
        assertEquals(dasar, dasar.terapkan(PerubahanPengingat()))
    }
}
