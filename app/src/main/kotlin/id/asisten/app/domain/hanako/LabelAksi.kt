package id.asisten.app.domain.hanako

import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.PenyusunHarian
import id.asisten.app.domain.label
import id.asisten.app.domain.pengingat.GeserMenit
import id.asisten.app.domain.pengingat.LabelPengingat
import id.asisten.app.domain.pengingat.ModePengingat
import id.asisten.app.domain.pengingat.namaMode
import id.asisten.app.domain.waktu.NamaHari
import id.asisten.app.domain.waktu.Waktu

/**
 * Indonesian text that tells the user, in plain words, what an approved proposal will do. For edits it shows the
 * old and the new value, so the user sees exactly what changes before tapping Setujui.
 */
object LabelAksi {

    const val CATATAN_TIDAK_BISA_BATAL = "Tidak bisa dibatalkan dari aplikasi."

    // The app's own reports in the chat after the user decided. They say what really happened.
    fun laporSetuju(ringkas: String) = "Sudah dilakukan: $ringkas"

    fun laporGagal(ringkas: String, alasan: String) = "Tidak jadi: $ringkas. $alasan"

    fun laporTolak(ringkas: String) = "Usulan dibatalkan, tidak ada yang diubah: $ringkas"

    fun laporTidakBerlaku(alasan: String) = "Usulan itu sudah tidak berlaku dan tidak dijalankan. $alasan"

    fun laporError(ringkas: String) =
        "Gagal menjalankan: $ringkas. Terjadi galat di ponsel, jadi mungkin belum berubah. Cek di tab yang bersangkutan, lalu coba lagi."

    fun ringkas(aksi: AksiHanako, d: DataSaatIni): String = when (aksi) {
        is AksiHanako.TambahTugas -> "Tambah ${aksi.tugas.jenis.label().lowercase()} \"${aksi.tugas.judul}\"" +
            rincian(
                aksi.tugas.mapel.takeIf { it.isNotEmpty() },
                aksi.tugas.tenggat?.let { "tenggat ${Waktu.tanggalPanjang(it)}" } ?: "tanpa tenggat",
                "prioritas ${aksi.tugas.prioritas.label().lowercase()}",
                aksi.tugas.catatan.takeIf { it.isNotEmpty() }?.let { "catatan: $it" },
            )
        is AksiHanako.UbahTugas -> {
            val lama = d.tugas.firstOrNull { it.id == aksi.id }
            if (lama == null) "Ubah tugas (id ${aksi.id})" else "Ubah tugas \"${lama.judul}\": " + bedaTugas(lama, aksi.perubahan)
        }
        is AksiHanako.TandaiTugas -> {
            val judul = d.tugas.firstOrNull { it.id == aksi.id }?.judul ?: "id ${aksi.id}"
            "Tandai tugas \"$judul\" ${if (aksi.selesai) "selesai" else "belum selesai"}"
        }
        is AksiHanako.HapusTugas -> "Hapus tugas \"${aksi.judul}\""

        is AksiHanako.TambahKegiatan -> "Tambah kegiatan \"${aksi.kegiatan.judul}\"" +
            rincian(
                waktuKegiatan(aksi.kegiatan),
                "prioritas ${aksi.kegiatan.prioritas.label().lowercase()}",
                aksi.kegiatan.catatan.takeIf { it.isNotEmpty() }?.let { "catatan: $it" },
            )
        is AksiHanako.UbahKegiatan -> {
            val lama = d.kegiatan.firstOrNull { it.id == aksi.id }
            if (lama == null) "Ubah kegiatan (id ${aksi.id})" else "Ubah kegiatan \"${lama.judul}\": " + bedaKegiatan(lama, aksi.perubahan)
        }
        is AksiHanako.HapusKegiatan -> "Hapus kegiatan \"${aksi.judul}\""

        is AksiHanako.TambahPelajaran -> "Tambah pelajaran \"${aksi.pelajaran.nama}\"" +
            rincian(
                "setiap ${NamaHari.untuk(aksi.pelajaran.hari)} ${Waktu.rentang(aksi.pelajaran.mulai, aksi.pelajaran.selesai)}",
                aksi.pelajaran.guru.takeIf { it.isNotEmpty() }?.let { "guru $it" },
                aksi.pelajaran.ruang.takeIf { it.isNotEmpty() }?.let { "ruang $it" },
            )
        is AksiHanako.UbahPelajaran -> {
            val lama = d.pelajaran.firstOrNull { it.id == aksi.id }
            if (lama == null) "Ubah pelajaran (id ${aksi.id})" else "Ubah pelajaran \"${lama.nama}\": " + bedaPelajaran(lama, aksi.perubahan)
        }
        is AksiHanako.HapusPelajaran -> "Hapus pelajaran \"${aksi.judul}\" dari jadwal sekolah"

        is AksiHanako.AturPengingat -> "Ubah pengaturan pengingat: " + bedaPengingat(d, aksi.perubahan)
        is AksiHanako.MulaiMode -> "Mulai mode ${aksi.mode.namaMode()} selama ${LabelPengingat.durasi(aksi.menit)}"
        AksiHanako.AkhiriMode -> "Akhiri mode ${d.pengingat.modeEfektif(d.sekarangMillis).namaMode()} dan kembali ke pengingat normal"
    }

    /** Warnings shown on the card: clashes with the schedule, things that cannot be undone, side effects. */
    fun peringatan(aksi: AksiHanako, d: DataSaatIni): List<String> = buildList {
        if (aksi.destruktif) add(CATATAN_TIDAK_BISA_BATAL)
        when (aksi) {
            is AksiHanako.TambahKegiatan -> bentrok(aksi.kegiatan, d, d.pelajaran, d.kegiatan)?.let { add(it) }
            is AksiHanako.UbahKegiatan -> d.kegiatan.firstOrNull { it.id == aksi.id }?.let { lama ->
                bentrok(lama.terapkan(aksi.perubahan), d, d.pelajaran, d.kegiatan)?.let { add(it) }
            }
            is AksiHanako.TambahPelajaran -> bentrok(
                Kegiatan(judul = aksi.pelajaran.nama, mulai = aksi.pelajaran.mulai, selesai = aksi.pelajaran.selesai, hariMingguan = aksi.pelajaran.hari),
                d, d.pelajaran, d.kegiatan,
            )?.let { add(it) }
            is AksiHanako.UbahPelajaran -> d.pelajaran.firstOrNull { it.id == aksi.id }?.let { lama ->
                val baru = lama.terapkan(aksi.perubahan)
                bentrok(
                    Kegiatan(judul = baru.nama, mulai = baru.mulai, selesai = baru.selesai, hariMingguan = baru.hari),
                    d, d.pelajaran.filter { it.id != lama.id }, d.kegiatan,
                )?.let { add(it) }
            }
            is AksiHanako.TambahTugas -> {
                if (d.tugas.any { !it.selesai && it.judul.equals(aksi.tugas.judul, ignoreCase = true) }) {
                    add("Sudah ada tugas yang belum selesai dengan judul yang sama.")
                }
            }
            is AksiHanako.AturPengingat -> {
                if (aksi.perubahan.aktif == false) add("Semua pengingat jadwal dan tugas akan berhenti sampai kamu menyalakannya lagi.")
            }
            is AksiHanako.MulaiMode -> add(
                if (aksi.mode == ModePengingat.ISTIRAHAT) "Selama mode Istirahat, semua pengingat datang tanpa suara."
                else "Selama mode Fokus, pengingat berprioritas rendah dan sedang datang tanpa suara.",
            )
            else -> Unit
        }
    }

    private fun bentrok(draf: Kegiatan, d: DataSaatIni, pelajaran: List<Pelajaran>, kegiatan: List<Kegiatan>): String? {
        val acuan = draf.tanggalAcuan(d.hariIni)
        val lain = PenyusunHarian.bentrokDengan(draf, acuan, kegiatan, pelajaran)
        if (lain.isEmpty()) return null
        val daftar = lain.take(3).joinToString(", ") { "${it.judul} ${Waktu.rentang(it.mulai, it.selesai)}" }
        val sisa = if (lain.size > 3) ", dan ${lain.size - 3} lagi" else ""
        return "Bentrok dengan $daftar$sisa."
    }

    private fun rincian(vararg bagian: String?): String {
        val isi = bagian.filterNotNull()
        return if (isi.isEmpty()) "" else " (" + isi.joinToString(", ") + ")"
    }

    /** "Senin, 17 November 2026" for a one-time activity, "setiap Rabu" for a weekly one. */
    private fun harinya(k: Kegiatan): String = when {
        k.tanggal != null -> Waktu.tanggalPanjang(k.tanggal)
        k.hariMingguan != null -> "setiap ${NamaHari.untuk(k.hariMingguan)}"
        else -> ""
    }

    private fun waktuKegiatan(k: Kegiatan): String =
        listOf(harinya(k), Waktu.rentang(k.mulai, k.selesai)).filter { it.isNotEmpty() }.joinToString(", ")

    private fun bedaTugas(lama: Tugas, p: PerubahanTugas): String {
        val baru = lama.terapkan(p)
        val beda = ArrayList<String>()
        if (baru.judul != lama.judul) beda.add("judul \"${lama.judul}\" → \"${baru.judul}\"")
        if (baru.jenis != lama.jenis) beda.add("jenis ${lama.jenis.label()} → ${baru.jenis.label()}")
        if (baru.mapel != lama.mapel) beda.add("mapel ${kosong(lama.mapel)} → ${kosong(baru.mapel)}")
        if (baru.tenggat != lama.tenggat) {
            beda.add("tenggat ${lama.tenggat?.let { Waktu.tanggalPanjang(it) } ?: "tanpa tenggat"} → ${baru.tenggat?.let { Waktu.tanggalPanjang(it) } ?: "tanpa tenggat"}")
        }
        if (baru.prioritas != lama.prioritas) beda.add("prioritas ${lama.prioritas.label()} → ${baru.prioritas.label()}")
        if (baru.catatan != lama.catatan) beda.add("catatan ${kosong(lama.catatan)} → ${kosong(baru.catatan)}")
        return beda.joinToString("; ")
    }

    private fun bedaKegiatan(lama: Kegiatan, p: PerubahanKegiatan): String {
        val baru = lama.terapkan(p)
        val beda = ArrayList<String>()
        if (baru.judul != lama.judul) beda.add("judul \"${lama.judul}\" → \"${baru.judul}\"")
        if (baru.tanggal != lama.tanggal || baru.hariMingguan != lama.hariMingguan) {
            beda.add("hari ${harinya(lama)} → ${harinya(baru)}")
        }
        if (baru.mulai != lama.mulai || baru.selesai != lama.selesai) {
            beda.add("jam ${Waktu.rentang(lama.mulai, lama.selesai)} → ${Waktu.rentang(baru.mulai, baru.selesai)}")
        }
        if (baru.prioritas != lama.prioritas) beda.add("prioritas ${lama.prioritas.label()} → ${baru.prioritas.label()}")
        if (baru.catatan != lama.catatan) beda.add("catatan ${kosong(lama.catatan)} → ${kosong(baru.catatan)}")
        return beda.joinToString("; ")
    }

    private fun bedaPelajaran(lama: Pelajaran, p: PerubahanPelajaran): String {
        val baru = lama.terapkan(p)
        val beda = ArrayList<String>()
        if (baru.nama != lama.nama) beda.add("nama \"${lama.nama}\" → \"${baru.nama}\"")
        if (baru.hari != lama.hari) beda.add("hari ${NamaHari.untuk(lama.hari)} → ${NamaHari.untuk(baru.hari)}")
        if (baru.mulai != lama.mulai || baru.selesai != lama.selesai) {
            beda.add("jam ${Waktu.rentang(lama.mulai, lama.selesai)} → ${Waktu.rentang(baru.mulai, baru.selesai)}")
        }
        if (baru.guru != lama.guru) beda.add("guru ${kosong(lama.guru)} → ${kosong(baru.guru)}")
        if (baru.ruang != lama.ruang) beda.add("ruang ${kosong(lama.ruang)} → ${kosong(baru.ruang)}")
        return beda.joinToString("; ")
    }

    private fun bedaPengingat(d: DataSaatIni, p: PerubahanPengingat): String {
        val lama = d.pengingat
        val baru = lama.terapkan(p)
        val beda = ArrayList<String>()
        if (baru.aktif != lama.aktif) beda.add("pengingat ${if (baru.aktif) "dinyalakan" else "dimatikan"}")
        if (baru.menitSebelumKegiatan != lama.menitSebelumKegiatan) {
            beda.add("pengingat kegiatan ${LabelPengingat.selang(lama.menitSebelumKegiatan).lowercase()} → ${LabelPengingat.selang(baru.menitSebelumKegiatan).lowercase()}")
        }
        if (baru.menitSebelumSekolah != lama.menitSebelumSekolah) {
            beda.add("pengingat sekolah ${LabelPengingat.selang(lama.menitSebelumSekolah).lowercase()} → ${LabelPengingat.selang(baru.menitSebelumSekolah).lowercase()}")
        }
        if (baru.tundaMenit != lama.tundaMenit) {
            beda.add("tunda ${LabelPengingat.durasi(lama.tundaMenit)} → ${LabelPengingat.durasi(baru.tundaMenit)}")
        }
        if (baru.jamTenangAktif != lama.jamTenangAktif) beda.add("jam tenang ${if (baru.jamTenangAktif) "dinyalakan" else "dimatikan"}")
        if (baru.jamTenangMulai != lama.jamTenangMulai || baru.jamTenangSelesai != lama.jamTenangSelesai) {
            beda.add(
                "jam tenang ${GeserMenit.format(lama.jamTenangMulai)}–${GeserMenit.format(lama.jamTenangSelesai)} → " +
                    "${GeserMenit.format(baru.jamTenangMulai)}–${GeserMenit.format(baru.jamTenangSelesai)}",
            )
        }
        return beda.joinToString("; ")
    }

    private fun kosong(teks: String) = if (teks.isEmpty()) "(kosong)" else "\"$teks\""
}
