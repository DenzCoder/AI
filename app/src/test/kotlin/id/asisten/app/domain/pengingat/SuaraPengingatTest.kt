package id.asisten.app.domain.pengingat

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SuaraPengingatTest {

    private val uriSistem = "content://media/internal/audio/media/42"
    private val uriBerkas = "content://media/external/audio/media/1000000123"

    // ---------- daftar nada ----------

    @Test
    fun kodeNadaUnikBerhuruf_kecilDanDikenaliKembali() {
        val kode = DaftarNada.SEMUA.map { it.kode }
        assertEquals(kode.size, kode.toSet().size)
        assertTrue(kode.all { Regex("[a-z][a-z_]*").matches(it) })
        for (n in DaftarNada.SEMUA) assertEquals(n, DaftarNada.cari(n.kode))
        assertNull(DaftarNada.cari("tidak-ada"))
        assertNull(DaftarNada.cari(null))
        assertTrue(DaftarNada.SEMUA.all { it.nama.isNotBlank() && it.keterangan.isNotBlank() })
    }

    @Test
    fun kodeNadaYangSudahDisimpanTidakBerubah() {
        // disimpan di ponsel pengguna dan dipakai sebagai nama berkas: mengubahnya memutus pilihan mereka
        assertEquals(listOf("lonceng", "kicau", "kotak_musik", "bel_sekolah", "alarm_digital"), DaftarNada.SEMUA.map { it.kode })
    }

    // ---------- penyimpanan sebagai teks ----------

    @Test
    fun setiapPilihanBisaDisimpanDanDibacaKembali() {
        val semua = listOf(
            SuaraPengingat.Bawaan,
            SuaraPengingat.Aplikasi("kicau"),
            SuaraPengingatCodec.sistem(uriSistem, "Nada Pagi")!!,
            SuaraPengingatCodec.berkas(uriBerkas, "Lagu | favorit")!!,
        )
        for (s in semua) assertEquals(s, SuaraPengingatCodec.decode(SuaraPengingatCodec.encode(s)))
    }

    @Test
    fun namaBerisiGarisTegakTidakMerusakPembacaan() {
        val s = SuaraPengingatCodec.berkas(uriBerkas, "a|b|c")!!
        val balik = SuaraPengingatCodec.decode(SuaraPengingatCodec.encode(s)) as SuaraPengingat.Berkas
        assertEquals("a|b|c", balik.nama)
        assertEquals(uriBerkas, balik.uri)
    }

    @Test
    fun teksRusakKembaliKeBawaanTanpaMelempar() {
        for (rusak in listOf(null, "", "   ", "xyz", "app", "app|", "app|tidak-ada", "sistem", "sistem|content://a", "berkas||x",
            "sistem|http://jahat/x|nama", "sistem|file:///sdcard/x|nama", "sistem|content://a b|nama", "|", "||", "bawaan|x")) {
            val hasil = SuaraPengingatCodec.decode(rusak)
            if (rusak == "bawaan|x") assertEquals(SuaraPengingat.Bawaan, hasil) else assertEquals("'$rusak'", SuaraPengingat.Bawaan, hasil)
        }
    }

    @Test
    fun hanyaAlamatContentYangDiterima() {
        assertTrue(SuaraPengingatCodec.uriSah(uriSistem))
        for (buruk in listOf("", "http://x/y", "https://x", "file:///sdcard/a.mp3", "android.resource://x/1", "content://a b",
            "content://a|b", "content://a\nb", "content://" + "a".repeat(600))) {
            assertFalse(buruk, SuaraPengingatCodec.uriSah(buruk))
            assertNull(buruk, SuaraPengingatCodec.sistem(buruk, "n"))
            assertNull(buruk, SuaraPengingatCodec.berkas(buruk, "n"))
        }
    }

    @Test
    fun namaDibersihkanDariKarakterKontrolDanDipotong() {
        assertEquals("Tanpa nama", SuaraPengingatCodec.bersihkanNama("   "))
        assertEquals("Tanpa nama", SuaraPengingatCodec.bersihkanNama("\n\t"))
        assertEquals("a b c", SuaraPengingatCodec.bersihkanNama("a\nb\u0000   c"))
        assertEquals(80, SuaraPengingatCodec.bersihkanNama("x".repeat(200)).length)
    }

    // ---------- saluran ----------

    @Test
    fun pengaturanBawaanTetapMemakaiSaluranLama() {
        assertEquals("pengingat", SaluranSuara.id(PengaturanSuaraPengingat()))
        assertEquals("Pengingat", SaluranSuara.nama(PengaturanSuaraPengingat()))
    }

    @Test
    fun setiapPilihanBerbedaPunyaSaluranBerbeda() {
        val semua = buildList {
            for (alarm in listOf(false, true)) {
                add(PengaturanSuaraPengingat(SuaraPengingat.Bawaan, alarm))
                for (n in DaftarNada.SEMUA) add(PengaturanSuaraPengingat(SuaraPengingat.Aplikasi(n.kode), alarm))
                add(PengaturanSuaraPengingat(SuaraPengingatCodec.sistem(uriSistem, "A")!!, alarm))
                add(PengaturanSuaraPengingat(SuaraPengingatCodec.berkas(uriSistem, "A")!!, alarm))
                add(PengaturanSuaraPengingat(SuaraPengingatCodec.berkas(uriBerkas, "A")!!, alarm))
            }
        }
        val id = semua.map { SaluranSuara.id(it) }
        assertEquals(id.size, id.toSet().size)
        assertFalse(SaluranSuara.ID_SENYAP in id)
        for (x in id.filter { it != SaluranSuara.ID_BAWAAN }) {
            assertTrue(x, x.startsWith(SaluranSuara.AWALAN))
            assertEquals(x, SaluranSuara.AWALAN.length + 16, x.length)
            assertTrue(x, x.all { it.isLetterOrDigit() || it == '_' })
        }
    }

    @Test
    fun idSalurannyaStabil() {
        val p = PengaturanSuaraPengingat(SuaraPengingat.Aplikasi("lonceng"), true)
        assertEquals(SaluranSuara.id(p), SaluranSuara.id(p.copy()))
        // nilai tetap: berubahnya id membuat saluran lama ditinggalkan dan pengaturan pengguna di sistem hilang
        assertEquals("pengingat_" + SaluranSuara.hashFnv64("app|lonceng#true"), SaluranSuara.id(p))
    }

    @Test
    fun hashFnvSesuaiVektorBakuYangDipublikasikan() {
        assertEquals("cbf29ce484222325", SaluranSuara.hashFnv64(""))
        assertEquals("af63dc4c8601ec8c", SaluranSuara.hashFnv64("a"))
        assertEquals("85944171f73967e8", SaluranSuara.hashFnv64("foobar"))
    }

    @Test
    fun namaSaluranMenyebutSuaranya() {
        val n = SaluranSuara.nama(PengaturanSuaraPengingat(SuaraPengingat.Aplikasi("kicau")))
        assertEquals("Pengingat (Kicau)", n)
        assertEquals("Pengingat (Bawaan ponsel)", SaluranSuara.nama(PengaturanSuaraPengingat(SuaraPengingat.Bawaan, true)))
    }

    @Test
    fun saluranUsangDikenaliTetapiSenyapDanSaluranLainTidakDisentuh() {
        val sekarang = "pengingat_abc"
        assertTrue(SaluranSuara.usang("pengingat", sekarang))
        assertTrue(SaluranSuara.usang("pengingat_zzz", sekarang))
        assertFalse(SaluranSuara.usang(sekarang, sekarang))
        assertFalse(SaluranSuara.usang(SaluranSuara.ID_SENYAP, sekarang))
        assertFalse(SaluranSuara.usang("saluran_lain", sekarang))
        assertFalse(SaluranSuara.usang("pengingatan", sekarang))
        assertFalse(SaluranSuara.usang("pengingat", "pengingat"))
    }

    // ---------- label ----------

    @Test
    fun labelMenjelaskanPilihan() {
        assertEquals("Bawaan ponsel", LabelSuaraPengingat.ringkas(PengaturanSuaraPengingat()))
        assertEquals("Lonceng · volume alarm", LabelSuaraPengingat.ringkas(PengaturanSuaraPengingat(SuaraPengingat.Aplikasi("lonceng"), true)))
        assertEquals("Lagu", LabelSuaraPengingat.nama(SuaraPengingatCodec.berkas(uriBerkas, "Lagu")!!))
        val jenis = listOf(
            SuaraPengingat.Bawaan, SuaraPengingat.Aplikasi("kicau"),
            SuaraPengingatCodec.sistem(uriSistem, "x")!!, SuaraPengingatCodec.berkas(uriBerkas, "x")!!,
        ).map { LabelSuaraPengingat.jenis(it) }
        assertEquals(4, jenis.toSet().size)
    }

    // ---------- berkas musik ----------

    @Test
    fun berkasAudioDiterimaDanBukanAudioDitolak() {
        assertNull(BatasBerkasNada.periksa("audio/mpeg", "a.mp3", 3_000_000L))
        assertNull(BatasBerkasNada.periksa("audio/mpeg; charset=x", "a.mp3", null))
        assertNull(BatasBerkasNada.periksa("application/ogg", "a.ogg", 1000L))
        assertEquals(AlasanTolakBerkas.BUKAN_AUDIO, BatasBerkasNada.periksa("image/png", "a.png", 100L))
        assertEquals(AlasanTolakBerkas.BUKAN_AUDIO, BatasBerkasNada.periksa("video/mp4", "a.mp4", 100L))
        assertEquals(AlasanTolakBerkas.BUKAN_AUDIO, BatasBerkasNada.periksa("application/pdf", "a.mp3", 100L))
        assertEquals(AlasanTolakBerkas.BUKAN_AUDIO, BatasBerkasNada.periksa(null, "a.txt", 100L))
    }

    @Test
    fun jenisUmumDitebakDariEkstensiBilaPenyediaTidakMenyebutnya() {
        assertEquals("audio/mpeg", BatasBerkasNada.mimeUntukSimpan("application/octet-stream", "Lagu.MP3"))
        assertEquals("audio/mpeg", BatasBerkasNada.mimeUntukSimpan(null, "lagu.mp3"))
        assertEquals("audio/mpeg", BatasBerkasNada.mimeUntukSimpan("", "lagu.mp3"))
        assertEquals("audio/mp4", BatasBerkasNada.mimeUntukSimpan(null, "x.m4a"))
        assertEquals("audio/ogg", BatasBerkasNada.mimeUntukSimpan(null, "x.opus"))
        assertEquals("audio/flac", BatasBerkasNada.mimeUntukSimpan("AUDIO/FLAC", "x"))
        assertNull(BatasBerkasNada.mimeUntukSimpan(null, "tanpa-ekstensi"))
        assertNull(BatasBerkasNada.mimeUntukSimpan("application/octet-stream", "x.exe"))
    }

    @Test
    fun ukuranDibatasi() {
        assertNull(BatasBerkasNada.periksa("audio/mpeg", "a.mp3", BatasBerkasNada.MAKS_BYTE))
        assertEquals(AlasanTolakBerkas.TERLALU_BESAR, BatasBerkasNada.periksa("audio/mpeg", "a.mp3", BatasBerkasNada.MAKS_BYTE + 1))
        assertEquals(AlasanTolakBerkas.KOSONG, BatasBerkasNada.periksa("audio/mpeg", "a.mp3", 0L))
        assertEquals(AlasanTolakBerkas.KOSONG, BatasBerkasNada.periksa("audio/mpeg", "a.mp3", -1L))
    }

    @Test
    fun namaBerkasAmanTanpaFolderDanKarakterAneh() {
        assertEquals("lagu.mp3", BatasBerkasNada.namaBerkasAman("lagu.mp3"))
        assertEquals("lagu.mp3", BatasBerkasNada.namaBerkasAman("../../etc/lagu.mp3"))
        assertEquals("lagu.mp3", BatasBerkasNada.namaBerkasAman("C:\\musik\\lagu.mp3"))
        assertEquals("lagu.mp3", BatasBerkasNada.namaBerkasAman("la<g>u?.mp3"))
        assertEquals("nada", BatasBerkasNada.namaBerkasAman(""))
        assertEquals("nada", BatasBerkasNada.namaBerkasAman("..."))
        assertEquals("nada", BatasBerkasNada.namaBerkasAman("///"))
        val panjang = BatasBerkasNada.namaBerkasAman("x".repeat(300) + ".mp3")
        assertEquals(80, panjang.length)
        assertTrue(panjang.endsWith(".mp3"))
        assertNotEquals("", BatasBerkasNada.namaBerkasAman("\u0000\u0001"))
    }

    @Test
    fun judulDiambilDariNamaBerkasTanpaEkstensi() {
        assertEquals("Lagu Kesukaan", BatasBerkasNada.judulDariNama("Lagu Kesukaan.mp3"))
        assertEquals("a.b", BatasBerkasNada.judulDariNama("a.b.flac"))
        assertEquals("nada", BatasBerkasNada.judulDariNama(""))
    }

    @Test
    fun pesanPenolakanMenyebutBatasYangSungguhDipakai() {
        assertEquals(15L * 1024 * 1024, BatasBerkasNada.MAKS_BYTE)
        assertTrue(LabelSuaraPengingat.tolak(AlasanTolakBerkas.TERLALU_BESAR).contains("15 MB"))
        val pesan = AlasanTolakBerkas.entries.map { LabelSuaraPengingat.tolak(it) }
        assertEquals(pesan.size, pesan.toSet().size)
        assertTrue(pesan.all { it.isNotBlank() })
    }

    @Test
    fun kodeNadaAdaBerkasSuaranyaDiRes() {
        // setiap kode di daftar harus punya res/raw/nada_<kode>.ogg, kalau tidak pilihannya diam-diam jatuh ke bawaan
        val raw = java.io.File("app/src/main/res/raw")
        if (!raw.isDirectory) return // dijalankan dari folder lain: pemeriksaan ini dilakukan juga oleh tools/cek-statis.py
        for (n in DaftarNada.SEMUA) assertTrue(n.kode, java.io.File(raw, "nada_${n.kode}.ogg").length() > 1000L)
    }
}

