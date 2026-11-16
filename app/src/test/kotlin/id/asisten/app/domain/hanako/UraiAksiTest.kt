package id.asisten.app.domain.hanako

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.hanako.DataUji.hariIni
import id.asisten.app.domain.hanako.DataUji.panggil
import id.asisten.app.domain.pengingat.ModePengingat
import java.time.DayOfWeek
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UraiAksiTest {

    private val data = DataUji.data()

    private fun urai(p: PanggilanFungsi, d: DataSaatIni = data) = UraiAksi.urai(p, d)

    private inline fun <reified T : AksiHanako> sah(p: PanggilanFungsi, d: DataSaatIni = data): T {
        val hasil = urai(p, d)
        assertTrue("diharapkan sah, tetapi: $hasil", hasil is HasilUrai.Sah)
        val aksi = (hasil as HasilUrai.Sah).aksi
        assertTrue("tipe aksi salah: $aksi", aksi is T)
        return aksi as T
    }

    private fun ditolak(p: PanggilanFungsi, d: DataSaatIni = data, mengandung: String? = null) {
        val hasil = urai(p, d)
        assertTrue("diharapkan ditolak, tetapi: $hasil", hasil is HasilUrai.Tidak)
        if (mengandung != null) {
            val alasan = (hasil as HasilUrai.Tidak).alasan
            assertTrue("alasan \"$alasan\" tidak memuat \"$mengandung\"", alasan.contains(mengandung))
        }
    }

    // ---------- tugas ----------

    @Test
    fun tambahTugasLengkap() {
        val a = sah<AksiHanako.TambahTugas>(
            panggil(
                NamaAksi.TAMBAH_TUGAS, "judul" to "Laporan Biologi", "jenis" to "pr", "mapel" to "Biologi",
                "tenggat" to "2026-11-20", "prioritas" to "TINGGI", "catatan" to "pakai kertas A4",
            ),
        )
        assertEquals("Laporan Biologi", a.tugas.judul)
        assertEquals(JenisTugas.PR, a.tugas.jenis)
        assertEquals("Biologi", a.tugas.mapel)
        assertEquals(java.time.LocalDate.of(2026, 11, 20), a.tugas.tenggat)
        assertEquals(Prioritas.TINGGI, a.tugas.prioritas)
        assertEquals("pakai kertas A4", a.tugas.catatan)
        assertEquals(0L, a.tugas.id)
        assertFalse(a.tugas.selesai)
    }

    @Test
    fun tambahTugasHanyaJudulMemakaiNilaiBawaan() {
        val a = sah<AksiHanako.TambahTugas>(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "Beli buku"))
        assertEquals(JenisTugas.TUGAS, a.tugas.jenis)
        assertEquals(Prioritas.SEDANG, a.tugas.prioritas)
        assertNull(a.tugas.tenggat)
        assertEquals("", a.tugas.mapel)
    }

    @Test
    fun judulWajibDanDibersihkan() {
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS), mengandung = "judul")
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "   "), mengandung = "tidak boleh kosong")
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to 5.0), mengandung = "teks")
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "x".repeat(101)), mengandung = "terlalu panjang")
        val a = sah<AksiHanako.TambahTugas>(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "  PR\nMatematika\t halaman 5 "))
        assertEquals("PR Matematika halaman 5", a.tugas.judul)
        val b = sah<AksiHanako.TambahTugas>(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "PR​‮X"))
        assertEquals("PRX", b.tugas.judul)
        sah<AksiHanako.TambahTugas>(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "x".repeat(100)))
    }

    @Test
    fun tenggatDiperiksa() {
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "tenggat" to "besok"), mengandung = "tenggat")
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "tenggat" to "2026-02-31"), mengandung = "tenggat")
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "tenggat" to "2040-01-01"), mengandung = "terlalu jauh")
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "tenggat" to "2020-01-01"), mengandung = "terlalu jauh")
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "tenggat" to 20261120.0), mengandung = "tanggal")
        // Setahun ke belakang masih boleh (tugas yang sudah lewat tenggat), format d-M-yyyy juga.
        sah<AksiHanako.TambahTugas>(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "tenggat" to "2026-11-10"))
        val a = sah<AksiHanako.TambahTugas>(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "tenggat" to "20-11-2026"))
        assertEquals(java.time.LocalDate.of(2026, 11, 20), a.tugas.tenggat)
    }

    @Test
    fun jenisDanPrioritasTidakDikenalDitolak() {
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "jenis" to "KUIS"), mengandung = "Pilihan")
        ditolak(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "prioritas" to "PENTING"), mengandung = "Pilihan")
    }

    @Test
    fun ubahTugasMemakaiIdYangAda() {
        val a = sah<AksiHanako.UbahTugas>(panggil(NamaAksi.UBAH_TUGAS, "id" to 1.0, "tenggat" to "2026-11-25", "prioritas" to "RENDAH"))
        assertEquals(1L, a.id)
        assertEquals(java.time.LocalDate.of(2026, 11, 25), a.perubahan.tenggat)
        assertEquals(Prioritas.RENDAH, a.perubahan.prioritas)
        assertNull(a.perubahan.judul)
    }

    @Test
    fun idHarusAdaDanSah() {
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "id" to 99.0, "judul" to "x"), mengandung = "id 99 tidak ditemukan")
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "judul" to "x"), mengandung = "'id'")
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "id" to 1.5, "judul" to "x"), mengandung = "'id'")
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "id" to 0.0, "judul" to "x"), mengandung = "'id'")
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "id" to -1.0, "judul" to "x"), mengandung = "'id'")
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "id" to true, "judul" to "x"), mengandung = "'id'")
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "id" to "abc", "judul" to "x"), mengandung = "'id'")
        sah<AksiHanako.UbahTugas>(panggil(NamaAksi.UBAH_TUGAS, "id" to "1", "judul" to "x"))
        sah<AksiHanako.UbahTugas>(panggil(NamaAksi.UBAH_TUGAS, "id" to 1, "judul" to "x"))
    }

    @Test
    fun ubahTugasTanpaPerubahanAtauSamaDitolak() {
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "id" to 1.0), mengandung = "Tidak ada yang diubah")
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "id" to 1.0, "prioritas" to "TINGGI"), mengandung = "sudah seperti itu")
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "id" to 1.0, "judul" to "PR Matematika"), mengandung = "sudah seperti itu")
    }

    @Test
    fun tenggatDanHapusTenggatTidakBolehBersamaan() {
        ditolak(
            panggil(NamaAksi.UBAH_TUGAS, "id" to 1.0, "tenggat" to "2026-11-25", "hapus_tenggat" to true),
            mengandung = "salah satu",
        )
        val a = sah<AksiHanako.UbahTugas>(panggil(NamaAksi.UBAH_TUGAS, "id" to 1.0, "hapus_tenggat" to true))
        assertTrue(a.perubahan.hapusTenggat)
    }

    @Test
    fun teksKosongPadaUbahMenghapusCatatan() {
        val a = sah<AksiHanako.UbahTugas>(panggil(NamaAksi.UBAH_TUGAS, "id" to 1.0, "catatan" to ""))
        assertEquals("", a.perubahan.catatan)
        assertEquals("", DataUji.tugasPr.terapkan(a.perubahan).catatan)
        ditolak(panggil(NamaAksi.UBAH_TUGAS, "id" to 1.0, "judul" to ""), mengandung = "tidak boleh kosong")
    }

    @Test
    fun tandaiTugas() {
        val a = sah<AksiHanako.TandaiTugas>(panggil(NamaAksi.TANDAI_TUGAS, "id" to 1.0, "selesai" to true))
        assertTrue(a.selesai)
        sah<AksiHanako.TandaiTugas>(panggil(NamaAksi.TANDAI_TUGAS, "id" to 1.0, "selesai" to "true"))
        sah<AksiHanako.TandaiTugas>(panggil(NamaAksi.TANDAI_TUGAS, "id" to 2.0, "selesai" to false))
        ditolak(panggil(NamaAksi.TANDAI_TUGAS, "id" to 2.0, "selesai" to true), mengandung = "sudah ditandai selesai")
        ditolak(panggil(NamaAksi.TANDAI_TUGAS, "id" to 1.0, "selesai" to false), mengandung = "belum selesai")
        ditolak(panggil(NamaAksi.TANDAI_TUGAS, "id" to 1.0), mengandung = "selesai")
        ditolak(panggil(NamaAksi.TANDAI_TUGAS, "id" to 1.0, "selesai" to "mungkin"), mengandung = "true atau false")
    }

    @Test
    fun hapusTugasMembawaJudulDanDitandaiDestruktif() {
        val a = sah<AksiHanako.HapusTugas>(panggil(NamaAksi.HAPUS_TUGAS, "id" to 1.0))
        assertEquals("PR Matematika", a.judul)
        assertTrue(a.destruktif)
        ditolak(panggil(NamaAksi.HAPUS_TUGAS, "id" to 5.0), mengandung = "tidak ditemukan")
    }

    // ---------- kegiatan ----------

    @Test
    fun tambahKegiatanMingguanDanSekali() {
        val m = sah<AksiHanako.TambahKegiatan>(
            panggil(NamaAksi.TAMBAH_KEGIATAN, "judul" to "Futsal", "hari" to "Jumat", "mulai" to "16:00", "selesai" to "17:00"),
        )
        assertEquals(DayOfWeek.FRIDAY, m.kegiatan.hariMingguan)
        assertNull(m.kegiatan.tanggal)
        assertEquals(LocalTime.of(16, 0), m.kegiatan.mulai)
        val s = sah<AksiHanako.TambahKegiatan>(
            panggil(NamaAksi.TAMBAH_KEGIATAN, "judul" to "Dokter gigi", "tanggal" to "2026-11-17", "mulai" to "9.30", "selesai" to "10:15"),
        )
        assertEquals(java.time.LocalDate.of(2026, 11, 17), s.kegiatan.tanggal)
        assertNull(s.kegiatan.hariMingguan)
        assertEquals(LocalTime.of(9, 30), s.kegiatan.mulai)
        assertEquals(Prioritas.SEDANG, s.kegiatan.prioritas)
    }

    @Test
    fun kegiatanHarusPunyaSalahSatuTanggalAtauHari() {
        val dasar = arrayOf("judul" to "x", "mulai" to "10:00", "selesai" to "11:00")
        ditolak(panggil(NamaAksi.TAMBAH_KEGIATAN, *dasar), mengandung = "'tanggal'")
        ditolak(panggil(NamaAksi.TAMBAH_KEGIATAN, *dasar, "tanggal" to "2026-11-17", "hari" to "SENIN"), mengandung = "salah satu")
    }

    @Test
    fun kegiatanSekaliTidakBolehDiMasaLalu() {
        ditolak(
            panggil(NamaAksi.TAMBAH_KEGIATAN, "judul" to "x", "tanggal" to "2026-11-15", "mulai" to "10:00", "selesai" to "11:00"),
            mengandung = "sudah lewat",
        )
        sah<AksiHanako.TambahKegiatan>(
            panggil(NamaAksi.TAMBAH_KEGIATAN, "judul" to "x", "tanggal" to "2026-11-16", "mulai" to "10:00", "selesai" to "11:00"),
        )
    }

    @Test
    fun jamDiperiksa() {
        val dasar = arrayOf("judul" to "x", "hari" to "SENIN")
        ditolak(panggil(NamaAksi.TAMBAH_KEGIATAN, *dasar, "mulai" to "11:00", "selesai" to "11:00"), mengandung = "setelah jam mulai")
        ditolak(panggil(NamaAksi.TAMBAH_KEGIATAN, *dasar, "mulai" to "12:00", "selesai" to "11:00"), mengandung = "setelah jam mulai")
        ditolak(panggil(NamaAksi.TAMBAH_KEGIATAN, *dasar, "mulai" to "25:00", "selesai" to "26:00"), mengandung = "'mulai'")
        ditolak(panggil(NamaAksi.TAMBAH_KEGIATAN, *dasar, "mulai" to "10:00"), mengandung = "'selesai'")
        ditolak(panggil(NamaAksi.TAMBAH_KEGIATAN, *dasar, "selesai" to "10:00"), mengandung = "'mulai'")
        ditolak(panggil(NamaAksi.TAMBAH_KEGIATAN, *dasar, "mulai" to 10.0, "selesai" to "11:00"), mengandung = "jam")
    }

    @Test
    fun namaHariDalamBahasaIndonesiaDanInggris() {
        fun hari(nama: String) = sah<AksiHanako.TambahKegiatan>(
            panggil(NamaAksi.TAMBAH_KEGIATAN, "judul" to "x", "hari" to nama, "mulai" to "10:00", "selesai" to "11:00"),
        ).kegiatan.hariMingguan
        assertEquals(DayOfWeek.MONDAY, hari("SENIN"))
        assertEquals(DayOfWeek.TUESDAY, hari("selasa"))
        assertEquals(DayOfWeek.FRIDAY, hari("Jum'at"))
        assertEquals(DayOfWeek.SUNDAY, hari("ahad"))
        assertEquals(DayOfWeek.THURSDAY, hari("Thursday"))
        ditolak(
            panggil(NamaAksi.TAMBAH_KEGIATAN, "judul" to "x", "hari" to "kemarin", "mulai" to "10:00", "selesai" to "11:00"),
            mengandung = "nama hari",
        )
    }

    @Test
    fun isianKosongPadaFieldBukanTeksDianggapTidakDiisi() {
        val a = sah<AksiHanako.TambahKegiatan>(
            panggil(
                NamaAksi.TAMBAH_KEGIATAN, "judul" to "x", "tanggal" to "", "hari" to "RABU",
                "mulai" to "10:00", "selesai" to "11:00", "prioritas" to "",
            ),
        )
        assertEquals(DayOfWeek.WEDNESDAY, a.kegiatan.hariMingguan)
        assertEquals(Prioritas.SEDANG, a.kegiatan.prioritas)
    }

    @Test
    fun ubahKegiatan() {
        val a = sah<AksiHanako.UbahKegiatan>(panggil(NamaAksi.UBAH_KEGIATAN, "id" to 10.0, "mulai" to "15:00"))
        assertEquals(LocalTime.of(15, 0), a.perubahan.mulai)
        // Memberi tanggal membuat kegiatan mingguan menjadi sekali.
        val sekali = sah<AksiHanako.UbahKegiatan>(panggil(NamaAksi.UBAH_KEGIATAN, "id" to 10.0, "tanggal" to "2026-11-18"))
        val baru = DataUji.lesInggris.terapkan(sekali.perubahan)
        assertEquals(java.time.LocalDate.of(2026, 11, 18), baru.tanggal)
        assertNull(baru.hariMingguan)
        // Memberi hari membuat kegiatan sekali menjadi mingguan.
        val mingguan = sah<AksiHanako.UbahKegiatan>(panggil(NamaAksi.UBAH_KEGIATAN, "id" to 11.0, "hari" to "KAMIS"))
        val baru2 = DataUji.rapatOsis.terapkan(mingguan.perubahan)
        assertEquals(DayOfWeek.THURSDAY, baru2.hariMingguan)
        assertNull(baru2.tanggal)
    }

    @Test
    fun ubahKegiatanYangMembuatJamTidakMasukAkalDitolak() {
        // Les berakhir 17:30; mulai 18:00 akan lebih akhir dari selesai.
        ditolak(panggil(NamaAksi.UBAH_KEGIATAN, "id" to 10.0, "mulai" to "18:00"), mengandung = "setelah jam mulai")
        ditolak(panggil(NamaAksi.UBAH_KEGIATAN, "id" to 10.0, "tanggal" to "2026-11-18", "hari" to "RABU"), mengandung = "salah satu")
        ditolak(panggil(NamaAksi.UBAH_KEGIATAN, "id" to 10.0), mengandung = "Tidak ada yang diubah")
        ditolak(panggil(NamaAksi.UBAH_KEGIATAN, "id" to 10.0, "hari" to "RABU"), mengandung = "sudah seperti itu")
        ditolak(panggil(NamaAksi.UBAH_KEGIATAN, "id" to 10.0, "tanggal" to "2026-11-10"), mengandung = "sudah lewat")
        ditolak(panggil(NamaAksi.UBAH_KEGIATAN, "id" to 77.0, "judul" to "x"), mengandung = "tidak ditemukan")
    }

    @Test
    fun hapusKegiatan() {
        val a = sah<AksiHanako.HapusKegiatan>(panggil(NamaAksi.HAPUS_KEGIATAN, "id" to 10.0))
        assertEquals("Les Inggris", a.judul)
        assertTrue(a.destruktif)
        ditolak(panggil(NamaAksi.HAPUS_KEGIATAN, "id" to 1.0), mengandung = "Kegiatan dengan id 1 tidak ditemukan")
    }

    // ---------- pelajaran ----------

    @Test
    fun tambahUbahHapusPelajaran() {
        val t = sah<AksiHanako.TambahPelajaran>(
            panggil(
                NamaAksi.TAMBAH_PELAJARAN, "hari" to "SELASA", "mulai" to "10:00", "selesai" to "11:30",
                "nama" to "IPA", "guru" to "Pak Budi", "ruang" to "Lab",
            ),
        )
        assertEquals(DayOfWeek.TUESDAY, t.pelajaran.hari)
        assertEquals("Pak Budi", t.pelajaran.guru)
        assertFalse(t.pelajaran.istirahat)
        ditolak(panggil(NamaAksi.TAMBAH_PELAJARAN, "mulai" to "10:00", "selesai" to "11:00", "nama" to "IPA"), mengandung = "'hari'")
        ditolak(panggil(NamaAksi.TAMBAH_PELAJARAN, "hari" to "SENIN", "mulai" to "11:00", "selesai" to "10:00", "nama" to "IPA"), mengandung = "setelah")

        val u = sah<AksiHanako.UbahPelajaran>(panggil(NamaAksi.UBAH_PELAJARAN, "id" to 20.0, "ruang" to "5", "guru" to ""))
        val baru = DataUji.matematika.terapkan(u.perubahan)
        assertEquals("5", baru.ruang)
        assertEquals("", baru.guru)
        ditolak(panggil(NamaAksi.UBAH_PELAJARAN, "id" to 20.0, "mulai" to "09:00"), mengandung = "setelah jam mulai")

        val h = sah<AksiHanako.HapusPelajaran>(panggil(NamaAksi.HAPUS_PELAJARAN, "id" to 20.0))
        assertEquals("Matematika", h.judul)
        assertTrue(h.destruktif)
    }

    @Test
    fun slotIstirahatTidakBisaDiubahAtauDihapus() {
        ditolak(panggil(NamaAksi.UBAH_PELAJARAN, "id" to 21.0, "nama" to "x"), mengandung = "istirahat")
        ditolak(panggil(NamaAksi.HAPUS_PELAJARAN, "id" to 21.0), mengandung = "istirahat")
    }

    // ---------- pengingat dan mode ----------

    @Test
    fun aturPengingat() {
        val a = sah<AksiHanako.AturPengingat>(
            panggil(
                NamaAksi.ATUR_PENGINGAT, "menit_sebelum_kegiatan" to 30.0, "tunda_menit" to 5.0,
                "jam_tenang_aktif" to true, "jam_tenang_mulai" to "21:30", "jam_tenang_selesai" to "05:00",
            ),
        )
        assertEquals(30, a.perubahan.menitSebelumKegiatan)
        assertEquals(5, a.perubahan.tundaMenit)
        assertEquals(true, a.perubahan.jamTenangAktif)
        assertEquals(21 * 60 + 30, a.perubahan.jamTenangMulai)
        assertEquals(5 * 60, a.perubahan.jamTenangSelesai)
        assertNull(a.perubahan.aktif)
    }

    @Test
    fun aturPengingatMenolakNilaiDiLuarBatas() {
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "menit_sebelum_kegiatan" to 721.0), mengandung = "antara 0 dan 720")
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "menit_sebelum_kegiatan" to -5.0), mengandung = "antara 0 dan 720")
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "menit_sebelum_sekolah" to 1000.0), mengandung = "antara 0 dan 720")
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "tunda_menit" to 0.0), mengandung = "antara 1 dan 120")
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "tunda_menit" to 121.0), mengandung = "antara 1 dan 120")
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "tunda_menit" to 7.5), mengandung = "bilangan bulat")
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "tunda_menit" to "cepat"), mengandung = "angka")
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "jam_tenang_mulai" to "24:00"), mengandung = "jam")
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "aktif" to 1.0), mengandung = "true atau false")
    }

    @Test
    fun aturPengingatTanpaPerubahanAtauSamaDitolak() {
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT), mengandung = "Tidak ada pengaturan")
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "menit_sebelum_kegiatan" to 10.0), mengandung = "sudah seperti itu")
        ditolak(panggil(NamaAksi.ATUR_PENGINGAT, "aktif" to true), mengandung = "sudah seperti itu")
    }

    @Test
    fun mulaiMode() {
        val f = sah<AksiHanako.MulaiMode>(panggil(NamaAksi.MULAI_MODE, "mode" to "FOKUS"))
        assertEquals(ModePengingat.FOKUS, f.mode)
        assertEquals(25, f.menit)
        val i = sah<AksiHanako.MulaiMode>(panggil(NamaAksi.MULAI_MODE, "mode" to "istirahat"))
        assertEquals(15, i.menit)
        val k = sah<AksiHanako.MulaiMode>(panggil(NamaAksi.MULAI_MODE, "mode" to "FOKUS", "menit" to 90.0))
        assertEquals(90, k.menit)
        sah<AksiHanako.MulaiMode>(panggil(NamaAksi.MULAI_MODE, "mode" to "FOKUS", "menit" to 480.0))
        ditolak(panggil(NamaAksi.MULAI_MODE, "mode" to "FOKUS", "menit" to 481.0), mengandung = "antara 1 dan 480")
        ditolak(panggil(NamaAksi.MULAI_MODE, "mode" to "FOKUS", "menit" to 0.0), mengandung = "antara 1 dan 480")
        ditolak(panggil(NamaAksi.MULAI_MODE, "mode" to "NORMAL"), mengandung = "akhiri_mode")
        ditolak(panggil(NamaAksi.MULAI_MODE, "mode" to "SANTAI"), mengandung = "Pilihan")
        ditolak(panggil(NamaAksi.MULAI_MODE), mengandung = "'mode'")
    }

    @Test
    fun akhiriModeHanyaBilaAdaModeAktif() {
        ditolak(panggil(NamaAksi.AKHIRI_MODE), mengandung = "Tidak ada mode")
        assertEquals(AksiHanako.AkhiriMode, sah<AksiHanako.AkhiriMode>(panggil(NamaAksi.AKHIRI_MODE), DataUji.dataModeAktif()))
        // Mode yang sudah kedaluwarsa dianggap tidak aktif.
        val lewat = DataUji.dataModeAktif().copy(sekarangMillis = DataUji.SEKARANG + 31 * 60_000L)
        ditolak(panggil(NamaAksi.AKHIRI_MODE), lewat, mengandung = "Tidak ada mode")
    }

    // ---------- umum ----------

    @Test
    fun perintahTidakDikenalDitolak() {
        ditolak(panggil("hapus_semua_data"), mengandung = "tidak mengenal")
        ditolak(panggil("DROP TABLE tugas"), mengandung = "tidak mengenal")
        ditolak(panggil(""), mengandung = "tidak mengenal")
    }

    @Test
    fun namaPerintahPanjangDipotongDiPesanGalat() {
        val hasil = urai(panggil("x".repeat(500))) as HasilUrai.Tidak
        assertTrue(hasil.alasan.length < 120)
    }

    @Test
    fun isianTambahanDiabaikan() {
        val a = sah<AksiHanako.HapusTugas>(panggil(NamaAksi.HAPUS_TUGAS, "id" to 1.0, "semua" to true, "sql" to "x"))
        assertEquals(1L, a.id)
    }

    @Test
    fun nilaiJsonNullDianggapTidakDiisi() {
        val a = sah<AksiHanako.TambahTugas>(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "tenggat" to null, "mapel" to null))
        assertNull(a.tugas.tenggat)
        assertEquals("", a.tugas.mapel)
    }

    @Test
    fun hanyaHapusDanBerisikoYangDestruktif() {
        assertFalse(AksiHanako.AkhiriMode.destruktif)
        assertFalse(AksiHanako.TandaiTugas(1, true).destruktif)
        assertFalse(AksiHanako.UbahTugas(1, PerubahanTugas(judul = "x")).destruktif)
        assertTrue(AksiHanako.HapusTugas(1, "x").destruktif)
        assertTrue(AksiHanako.HapusKegiatan(1, "x").destruktif)
        assertTrue(AksiHanako.HapusPelajaran(1, "x").destruktif)
    }
}
