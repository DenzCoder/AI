package id.asisten.app.domain.hanako

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.hanako.DataUji.hariIni
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.pengingat.ModePengingat
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LabelAksiTest {

    private val data = DataUji.data()

    private fun ringkas(a: AksiHanako, d: DataSaatIni = data) = LabelAksi.ringkas(a, d)
    private fun peringatan(a: AksiHanako, d: DataSaatIni = data) = LabelAksi.peringatan(a, d)

    @Test
    fun tambahTugasMenyebutSemuaRincian() {
        val teks = ringkas(
            AksiHanako.TambahTugas(
                Tugas(judul = "Laporan", jenis = JenisTugas.PR, mapel = "IPA", tenggat = hariIni.plusDays(2), prioritas = Prioritas.TINGGI, catatan = "A4"),
            ),
        )
        assertEquals("Tambah pr \"Laporan\" (IPA, tenggat Rabu, 18 November 2026, prioritas tinggi, catatan: A4)", teks)
    }

    @Test
    fun tambahTugasTanpaTenggatDisebutTerusTerang() {
        assertTrue(ringkas(AksiHanako.TambahTugas(Tugas(judul = "a"))).contains("tanpa tenggat"))
    }

    @Test
    fun ubahMenampilkanNilaiLamaDanBaru() {
        val teks = ringkas(AksiHanako.UbahTugas(1, PerubahanTugas(tenggat = hariIni.plusDays(5), prioritas = Prioritas.RENDAH, catatan = "")))
        assertTrue(teks.startsWith("Ubah tugas \"PR Matematika\": "))
        assertTrue(teks.contains("tenggat Rabu, 18 November 2026 → Sabtu, 21 November 2026"))
        assertTrue(teks.contains("prioritas Tinggi → Rendah"))
        assertTrue(teks.contains("catatan \"bab 3\" → (kosong)"))
    }

    @Test
    fun ubahKegiatanMenampilkanHariDanJam() {
        val teks = ringkas(AksiHanako.UbahKegiatan(10, PerubahanKegiatan(hariMingguan = DayOfWeek.FRIDAY, mulai = LocalTime.of(15, 0))))
        assertTrue(teks.contains("hari setiap Rabu → setiap Jumat"))
        assertTrue(teks.contains("jam 16:00–17:30 → 15:00–17:30"))
        val sekali = ringkas(AksiHanako.UbahKegiatan(10, PerubahanKegiatan(tanggal = hariIni.plusDays(2))))
        assertTrue(sekali.contains("setiap Rabu → Rabu, 18 November 2026"))
    }

    @Test
    fun hapusDanTandaiDanMode() {
        assertEquals("Hapus tugas \"PR Matematika\"", ringkas(AksiHanako.HapusTugas(1, "PR Matematika")))
        assertEquals("Tandai tugas \"PR Matematika\" selesai", ringkas(AksiHanako.TandaiTugas(1, true)))
        assertEquals("Tandai tugas \"Laporan IPA\" belum selesai", ringkas(AksiHanako.TandaiTugas(2, false)))
        assertEquals("Mulai mode Fokus selama 25 menit", ringkas(AksiHanako.MulaiMode(ModePengingat.FOKUS, 25)))
        assertEquals("Mulai mode Istirahat selama 1 jam 30 menit", ringkas(AksiHanako.MulaiMode(ModePengingat.ISTIRAHAT, 90)))
        assertEquals(
            "Akhiri mode Fokus dan kembali ke pengingat normal",
            ringkas(AksiHanako.AkhiriMode, DataUji.dataModeAktif()),
        )
    }

    @Test
    fun aturPengingatMenampilkanPerbedaan() {
        val teks = ringkas(AksiHanako.AturPengingat(PerubahanPengingat(menitSebelumKegiatan = 0, jamTenangAktif = true)))
        assertEquals(
            "Ubah pengaturan pengingat: pengingat kegiatan 10 menit sebelum → tepat waktu; jam tenang dinyalakan",
            teks,
        )
    }

    @Test
    fun tambahPelajaranDanKegiatanMingguan() {
        val k = ringkas(AksiHanako.TambahKegiatan(Kegiatan(judul = "Futsal", hariMingguan = DayOfWeek.FRIDAY, mulai = LocalTime.of(16, 0), selesai = LocalTime.of(17, 0))))
        assertEquals("Tambah kegiatan \"Futsal\" (setiap Jumat, 16:00–17:00, prioritas sedang)", k)
        val p = ringkas(AksiHanako.TambahPelajaran(DataUji.matematika.copy(id = 0)))
        assertEquals("Tambah pelajaran \"Matematika\" (setiap Senin 07:00–08:30, guru Bu Ani, ruang 3)", p)
    }

    // ---------- peringatan ----------

    @Test
    fun hapusSelaluDiberiPeringatanTidakBisaDibatalkan() {
        for (a in listOf(AksiHanako.HapusTugas(1, "x"), AksiHanako.HapusKegiatan(10, "x"), AksiHanako.HapusPelajaran(20, "x"))) {
            assertTrue(peringatan(a).contains(LabelAksi.CATATAN_TIDAK_BISA_BATAL))
        }
        assertFalse(peringatan(AksiHanako.TandaiTugas(1, true)).contains(LabelAksi.CATATAN_TIDAK_BISA_BATAL))
        assertFalse(peringatan(AksiHanako.TambahTugas(Tugas(judul = "baru"))).contains(LabelAksi.CATATAN_TIDAK_BISA_BATAL))
    }

    @Test
    fun kegiatanYangBentrokDenganKegiatanLainDiperingatkan() {
        val futsal = Kegiatan(judul = "Futsal", hariMingguan = DayOfWeek.WEDNESDAY, mulai = LocalTime.of(17, 0), selesai = LocalTime.of(18, 0))
        val w = peringatan(AksiHanako.TambahKegiatan(futsal))
        assertEquals(listOf("Bentrok dengan Les Inggris 16:00–17:30."), w)
    }

    @Test
    fun kegiatanYangBentrokDenganPelajaranDiperingatkan() {
        val pagi = Kegiatan(judul = "Piket", hariMingguan = DayOfWeek.MONDAY, mulai = LocalTime.of(7, 30), selesai = LocalTime.of(8, 0))
        assertEquals(listOf("Bentrok dengan Matematika 07:00–08:30."), peringatan(AksiHanako.TambahKegiatan(pagi)))
    }

    @Test
    fun istirahatTidakDianggapBentrok() {
        val saatIstirahat = Kegiatan(judul = "Makan", hariMingguan = DayOfWeek.MONDAY, mulai = LocalTime.of(9, 0), selesai = LocalTime.of(9, 30))
        assertTrue(peringatan(AksiHanako.TambahKegiatan(saatIstirahat)).isEmpty())
    }

    @Test
    fun mengubahKegiatanTidakBentrokDenganDirinyaSendiri() {
        assertTrue(peringatan(AksiHanako.UbahKegiatan(10, PerubahanKegiatan(selesai = LocalTime.of(17, 45)))).isEmpty())
        val ke = peringatan(AksiHanako.UbahKegiatan(10, PerubahanKegiatan(hariMingguan = DayOfWeek.MONDAY, mulai = LocalTime.of(7, 0), selesai = LocalTime.of(8, 0))))
        assertEquals(listOf("Bentrok dengan Matematika 07:00–08:30."), ke)
    }

    @Test
    fun pelajaranBaruYangBentrokDenganPelajaranLain() {
        val bentrok = DataUji.matematika.copy(id = 0, nama = "IPA", mulai = LocalTime.of(8, 0), selesai = LocalTime.of(9, 0))
        assertEquals(listOf("Bentrok dengan Matematika 07:00–08:30."), peringatan(AksiHanako.TambahPelajaran(bentrok)))
        // Memperpanjang pelajaran yang ada tidak bentrok dengan dirinya sendiri.
        assertTrue(peringatan(AksiHanako.UbahPelajaran(20, PerubahanPelajaran(selesai = LocalTime.of(8, 45)))).isEmpty())
    }

    @Test
    fun banyakBentrokDiringkas() {
        val banyak = (1..5).map { DataUji.lesInggris.copy(id = 100L + it, judul = "K$it") }
        val d = DataUji.data(kegiatan = banyak)
        val w = peringatan(AksiHanako.TambahKegiatan(DataUji.lesInggris.copy(id = 0, judul = "Baru")), d)
        assertEquals(1, w.size)
        assertTrue(w[0].endsWith(", dan 2 lagi."))
    }

    @Test
    fun judulTugasKembarDiperingatkanHanyaBilaYangLamaBelumSelesai() {
        assertTrue(peringatan(AksiHanako.TambahTugas(Tugas(judul = "pr matematika"))).isNotEmpty())
        assertTrue(peringatan(AksiHanako.TambahTugas(Tugas(judul = "Laporan IPA"))).isEmpty())
    }

    @Test
    fun mematikanPengingatDanModeDiberiPenjelasan() {
        assertTrue(peringatan(AksiHanako.AturPengingat(PerubahanPengingat(aktif = false))).single().contains("berhenti"))
        assertTrue(peringatan(AksiHanako.AturPengingat(PerubahanPengingat(aktif = true))).isEmpty())
        assertTrue(peringatan(AksiHanako.MulaiMode(ModePengingat.FOKUS, 25)).single().contains("rendah dan sedang"))
        assertTrue(peringatan(AksiHanako.MulaiMode(ModePengingat.ISTIRAHAT, 25)).single().contains("semua pengingat"))
    }

    @Test
    fun laporanDiChatMengatakanApaYangSungguhTerjadi() {
        assertEquals("Sudah dilakukan: Hapus tugas \"x\"", LabelAksi.laporSetuju("Hapus tugas \"x\""))
        assertEquals("Usulan dibatalkan, tidak ada yang diubah: Hapus tugas \"x\"", LabelAksi.laporTolak("Hapus tugas \"x\""))
        assertEquals("Tidak jadi: Hapus tugas \"x\". Tugas itu sudah tidak ada.", LabelAksi.laporGagal("Hapus tugas \"x\"", "Tugas itu sudah tidak ada."))
        assertTrue(LabelAksi.laporTidakBerlaku("Alasan.").contains("tidak ada yang berubah") || LabelAksi.laporTidakBerlaku("Alasan.").contains("tidak dijalankan"))
        assertTrue(LabelAksi.laporError("X").contains("mungkin belum berubah"))
    }
}
