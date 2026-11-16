package id.asisten.app.domain.hanako

import id.asisten.app.domain.hanako.DataUji.panggil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UsulanHanakoTest {

    private val data = DataUji.data()
    private val sekarang = 1_800_000_000_000L

    private fun mentah(id: Long, p: PanggilanFungsi, waktu: Long = sekarang) = RiwayatUsulan.mentah(id, waktu, p)!!

    // ---------- penyimpanan ----------

    @Test
    fun encodeDecodeBolakBalik() {
        val p = panggil(
            NamaAksi.TAMBAH_TUGAS, "judul" to "PR | \"Mat\"\nbaris dua \\ 😀", "tenggat" to "2026-11-20", "id" to 4.0, "selesai" to true,
        )
        val daftar = listOf(mentah(1, p), mentah(2, panggil(NamaAksi.HAPUS_TUGAS, "id" to 1.0)))
        val teks = RiwayatUsulan.encode(daftar)
        assertEquals(1, teks.count { it == '\n' })
        val balik = RiwayatUsulan.decode(teks)
        assertEquals(daftar, balik)
        assertEquals(p, RiwayatUsulan.panggilan(balik[0]))
    }

    @Test
    fun decodeMelewatiBarisRusak() {
        val bagus = RiwayatUsulan.encode(listOf(mentah(5, panggil(NamaAksi.HAPUS_TUGAS, "id" to 1.0))))
        val teks = listOf(
            "bukan usulan",
            "x|1|hapus_tugas|{}",
            "3|y|hapus_tugas|{}",
            "3|1|perintah_asing|{}",
            "3|1|hapus_tugas|[1,2]",
            "3|1|hapus_tugas|{rusak",
            bagus,
        ).joinToString("\n")
        val hasil = RiwayatUsulan.decode(teks)
        assertEquals(1, hasil.size)
        assertEquals(5L, hasil[0].id)
        assertTrue(RiwayatUsulan.decode(null).isEmpty())
        assertTrue(RiwayatUsulan.decode("").isEmpty())
    }

    @Test
    fun maksimalSepuluhUsulanTerbaru() {
        val banyak = (1L..15L).map { mentah(it, panggil(NamaAksi.HAPUS_TUGAS, "id" to it.toDouble())) }
        val hasil = RiwayatUsulan.decode(RiwayatUsulan.encode(banyak))
        assertEquals((6L..15L).toList(), hasil.map { it.id })
        assertEquals(10, RiwayatUsulan.tambah(banyak.take(8), banyak.drop(8)).size)
    }

    @Test
    fun usulanLebihDariSehariDibuang() {
        val baru = mentah(1, panggil(NamaAksi.AKHIRI_MODE), waktu = sekarang - 1000)
        val hampir = mentah(2, panggil(NamaAksi.AKHIRI_MODE), waktu = sekarang - RiwayatUsulan.UMUR_MS)
        val lama = mentah(3, panggil(NamaAksi.AKHIRI_MODE), waktu = sekarang - RiwayatUsulan.UMUR_MS - 1)
        val dariMasaDepan = mentah(4, panggil(NamaAksi.AKHIRI_MODE), waktu = sekarang + 5000)
        assertEquals(listOf(1L, 2L), RiwayatUsulan.segar(listOf(baru, hampir, lama, dariMasaDepan), sekarang).map { it.id })
    }

    @Test
    fun usulanKembarTidakDitumpuk() {
        val a = mentah(1, panggil(NamaAksi.HAPUS_TUGAS, "id" to 1.0))
        val kembar = mentah(2, panggil(NamaAksi.HAPUS_TUGAS, "id" to 1.0))
        val lain = mentah(3, panggil(NamaAksi.HAPUS_TUGAS, "id" to 2.0))
        assertEquals(listOf(1L), RiwayatUsulan.tambah(listOf(a), listOf(kembar)).map { it.id })
        assertEquals(listOf(1L, 3L), RiwayatUsulan.tambah(listOf(a), listOf(kembar, lain)).map { it.id })
        // Dua yang kembar dalam satu balasan juga hanya masuk satu.
        assertEquals(listOf(1L), RiwayatUsulan.tambah(emptyList(), listOf(a, kembar)).map { it.id })
    }

    @Test
    fun hapusDanIdBaru() {
        val a = mentah(10, panggil(NamaAksi.AKHIRI_MODE))
        val b = mentah(11, panggil(NamaAksi.AKHIRI_MODE))
        assertEquals(listOf(11L), RiwayatUsulan.hapus(listOf(a, b), 10).map { it.id })
        assertEquals(listOf(10L, 11L), RiwayatUsulan.hapus(listOf(a, b), 99).map { it.id })
        assertEquals(12L, RiwayatUsulan.idBaru(listOf(a, b), 10))
        assertEquals(20L, RiwayatUsulan.idBaru(listOf(a, b), 20))
    }

    @Test
    fun angkaTidakBerubahSetelahDisimpan() {
        val p = panggil(NamaAksi.UBAH_TUGAS, "id" to 1.0, "judul" to "Baru")
        val balik = RiwayatUsulan.panggilan(RiwayatUsulan.decode(RiwayatUsulan.encode(listOf(mentah(1, p))))[0])
        assertEquals(1.0, balik!!.args["id"])
        assertTrue(UraiAksi.urai(balik, data) is HasilUrai.Sah)
    }

    // ---------- tampilan ----------

    @Test
    fun tampilHanyaUsulanYangMasihSah() {
        val sah = mentah(1, panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "Beli buku", "tenggat" to "2026-11-20"))
        val hapusAda = mentah(2, panggil(NamaAksi.HAPUS_TUGAS, "id" to 1.0))
        val hapusHilang = mentah(3, panggil(NamaAksi.HAPUS_TUGAS, "id" to 55.0))
        val rusak = UsulanMentah(4, sekarang, NamaAksi.UBAH_TUGAS, "{rusak")
        val tampil = RiwayatUsulan.tampil(listOf(sah, hapusAda, hapusHilang, rusak), data)
        assertEquals(listOf(1L, 2L), tampil.map { it.id })
        assertTrue(tampil[0].ringkas.contains("Beli buku"))
        assertTrue(tampil[1].peringatan.contains(LabelAksi.CATATAN_TIDAK_BISA_BATAL))
    }

    @Test
    fun usulanHilangBilaDatanyaBerubahSebelumDisetujui() {
        val tandai = mentah(1, panggil(NamaAksi.TANDAI_TUGAS, "id" to 1.0, "selesai" to true))
        assertEquals(1, RiwayatUsulan.tampil(listOf(tandai), data).size)
        // Pengguna sendiri sudah menandai tugas itu selesai: usulannya tidak berarti lagi.
        val sudah = DataUji.data(tugas = listOf(DataUji.tugasPr.copy(selesai = true), DataUji.tugasSelesai))
        assertTrue(RiwayatUsulan.tampil(listOf(tandai), sudah).isEmpty())
    }

    // ---------- membangun balasan ----------

    @Test
    fun balasanDenganTeksModelSelaluMenyatakanBelumDijalankan() {
        val hasil = BalasanAksi.susun(
            "Aku usulkan tugas baru ya.",
            listOf(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "Beli buku")),
            data,
        )
        assertEquals(1, hasil.disimpan.size)
        assertTrue(hasil.teks.startsWith("Aku usulkan tugas baru ya."))
        assertTrue(hasil.teks.contains("Belum ada yang diubah"))
    }

    @Test
    fun modelYangBerbohongSudahDilakukanTetapDikoreksiOlehAplikasi() {
        val hasil = BalasanAksi.susun(
            "Sudah kuhapus semua tugasmu!",
            listOf(panggil(NamaAksi.HAPUS_TUGAS, "id" to 1.0)),
            data,
        )
        assertTrue(hasil.teks.contains("Belum ada yang diubah"))
    }

    @Test
    fun balasanTanpaTeksModelMemakaiKalimatBawaan() {
        val hasil = BalasanAksi.susun("", listOf(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a"), panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "b")), data)
        assertTrue(hasil.teks.startsWith("Aku siapkan 2 usulan perubahan."))
        assertEquals(2, hasil.disimpan.size)
    }

    @Test
    fun usulanTidakSahDilaporkanDenganAlasanDanTidakDisimpan() {
        val hasil = BalasanAksi.susun("", listOf(panggil(NamaAksi.HAPUS_TUGAS, "id" to 99.0)), data)
        assertTrue(hasil.disimpan.isEmpty())
        assertTrue(hasil.teks.contains("tidak bisa kubuat"))
        assertTrue(hasil.teks.contains("id 99 tidak ditemukan"))
        assertFalse(hasil.teks.contains("Belum ada yang diubah"))
    }

    @Test
    fun campuranSahDanTidakSah() {
        val hasil = BalasanAksi.susun(
            "Oke.",
            listOf(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a"), panggil(NamaAksi.HAPUS_KEGIATAN, "id" to 999.0)),
            data,
        )
        assertEquals(1, hasil.disimpan.size)
        assertTrue(hasil.teks.contains("Belum ada yang diubah"))
        assertTrue(hasil.teks.contains("tidak bisa kubuat"))
    }

    @Test
    fun usulanDibatasiLimaDanPenggunaDiberiTahu() {
        val banyak = (1..8).map { panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "Tugas $it") }
        val hasil = BalasanAksi.susun("", banyak, data)
        assertEquals(5, hasil.disimpan.size)
        assertEquals((1..5).map { "Tugas $it" }, hasil.disimpan.map { it.args["judul"] })
        assertTrue(hasil.teks.contains("hanya 5 usulan pertama"))
    }

    // ---------- pesan chat jenis aksi ----------

    @Test
    fun pesanAksiTersimanDanTerbacaKembali() {
        val daftar = listOf(
            PesanChat(PeranChat.PENGGUNA, "tambah tugas", waktu = 1),
            PesanChat(PeranChat.HANAKO, "Aku usulkan.", EkspresiHanako.SENYUM, 2),
            PesanChat(PeranChat.HANAKO, "Sudah dilakukan: Tambah tugas \"a\"", null, 3, aksi = true),
            PesanChat(PeranChat.HANAKO, "Halo (lokal)", EkspresiHanako.NORMAL, 4, lokal = true),
        )
        val balik = RiwayatChatCodec.decode(RiwayatChatCodec.encode(daftar))
        assertEquals(daftar, balik)
        assertTrue(balik[2].aksi)
        assertFalse(balik[2].lokal)
        assertFalse(balik[1].aksi)
        assertTrue(balik[3].lokal)
        assertFalse(balik[3].aksi)
    }

    @Test
    fun riwayatLamaYangMemakaiFlag0Dan1TetapTerbaca() {
        val lama = "H|SENYUM|5|1|halo lokal\nH|-|6|0|halo online"
        val hasil = RiwayatChatCodec.decode(lama)
        assertTrue(hasil[0].lokal)
        assertFalse(hasil[0].aksi)
        assertFalse(hasil[1].lokal)
        assertFalse(hasil[1].aksi)
    }

    @Test
    fun pesanAksiIkutDikirimKeModelSebagaiRiwayat() {
        val riwayat = listOf(
            PesanChat(PeranChat.PENGGUNA, "tambah tugas"),
            PesanChat(PeranChat.HANAKO, "Aku usulkan."),
            PesanChat(PeranChat.HANAKO, "Sudah dilakukan: x", aksi = true),
        )
        val untukModel = RiwayatChat.untukModel(riwayat)
        assertEquals(2, untukModel.size)
        assertTrue(untukModel[1].isi.contains("Sudah dilakukan: x"))
        assertNotNull(untukModel[1])
        assertNull(untukModel[0].ekspresi)
    }
}
