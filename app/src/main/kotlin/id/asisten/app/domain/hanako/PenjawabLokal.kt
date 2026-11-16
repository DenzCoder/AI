package id.asisten.app.domain.hanako

import id.asisten.app.domain.Sapaan
import id.asisten.app.domain.SapaanProvider

data class BalasanLokal(val teks: String, val ekspresi: EkspresiHanako)

/**
 * The offline fallback: when no Gemini key is set up (or online mode is off) Hanako still answers a few questions
 * from the user's real data, by simple keyword matching. It never invents anything: if it does not understand,
 * it says so and explains how to turn on the AI.
 */
object PenjawabLokal {

    private val KATA_JADWAL = listOf("jadwal", "agenda", "kegiatan", "pelajaran", "piket")
    private val KATA_TUGAS = listOf("tugas", "pr", "deadline", "tenggat", "ujian", "mendesak", "dikumpul")
    private val KATA_TUJUAN = listOf("tujuan", "target", "cita", "goal")
    private val KATA_SAPA = listOf("halo", "hai", "hi", "hei", "hello", "selamat")
    private val KATA_TERIMA_KASIH = listOf("terima", "makasih", "thanks", "thx")
    private val KATA_UBAH = listOf(
        "tambah", "tambahkan", "ubah", "ganti", "hapus", "hapuskan", "atur", "aturkan", "ingatkan",
        "pindah", "pindahkan", "geser", "batalkan", "buatkan", "bikin", "masukkan", "catatkan",
        "tambahin", "hapusin", "ubahin", "gantiin", "aturin", "pindahin", "ingetin", "ingatin",
    )
    private val KATA_SEMANGAT = listOf("semangat", "capek", "lelah", "malas", "males", "bosan", "stres", "stress")

    fun jawab(pertanyaan: String, latar: LatarHanako): BalasanLokal {
        val kata = pertanyaan.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        fun cocok(daftar: List<String>) = kata.any { k -> daftar.any { d -> if (d.length <= 3) k == d else k.startsWith(d) } }

        // A request to change something gets an honest explanation, not a list that ignores what was asked.
        // Whole words only: "aturan" or "tambahan" are not requests to change anything.
        if (kata.any { it in KATA_UBAH }) return BalasanLokal(PESAN_UBAH, EkspresiHanako.BINGUNG)

        val hariIni = latar.sekarang.toLocalDate()
        val bagian = ArrayList<String>()
        var ekspresi = EkspresiHanako.SENYUM

        if (cocok(KATA_SAPA) && !cocok(KATA_JADWAL) && !cocok(KATA_TUGAS)) {
            bagian.add(sapaan(latar))
        }
        if (cocok(KATA_JADWAL)) {
            val baris = RingkasData.jadwal(latar.jadwalHariIni)
            bagian.add(
                if (baris.isEmpty()) "Hari ini belum ada jadwal yang tercatat. Kamu bisa menambahkannya di tab Jadwal."
                else "Jadwalmu hari ini:\n" + baris.joinToString("\n") { "• $it" },
            )
        }
        if (cocok(KATA_TUGAS)) {
            val baris = RingkasData.tugas(latar.tugas, hariIni)
            bagian.add(
                if (baris.isEmpty()) "Tidak ada tugas yang belum selesai. Mantap!"
                else "Tugas yang belum selesai:\n" + baris.joinToString("\n") { "• $it" },
            )
            if (baris.isEmpty()) ekspresi = EkspresiHanako.TERTAWA
        }
        if (cocok(KATA_TUJUAN)) {
            val baris = RingkasData.tujuan(latar.tujuanAktif)
            bagian.add(
                if (baris.isEmpty()) "Belum ada tujuan aktif. Kamu bisa menambahkannya di tab Aktivitas."
                else "Tujuan yang sedang berjalan:\n" + baris.joinToString("\n") { "• $it" },
            )
        }
        if (cocok(KATA_TERIMA_KASIH)) {
            bagian.add("Sama-sama! Senang bisa membantu.")
            ekspresi = EkspresiHanako.MALU
        }
        if (cocok(KATA_SEMANGAT)) {
            bagian.add("Pelan-pelan saja ya. Pilih satu hal kecil dulu, selesaikan, lalu istirahat sebentar. Aku di sini.")
        }

        if (bagian.isEmpty()) {
            return BalasanLokal(
                "Aku sedang dalam mode lokal, jadi baru bisa menjawab soal jadwal, tugas, dan tujuanmu. " +
                    "Untuk ngobrol bebas, hubungkan kunci Gemini lewat ikon gerigi di atas.",
                EkspresiHanako.BINGUNG,
            )
        }
        // Offline answers use data on the phone only and nothing leaves the device, so the "share with AI" flag
        // (LatarHanako.dataDibagikan) does not limit them: the caller passes the real data here.
        return BalasanLokal(bagian.joinToString("\n\n"), ekspresi)
    }

    const val PESAN_UBAH =
        "Di mode lokal aku belum bisa mengubah apa pun di aplikasi. Hubungkan kunci Gemini lewat ikon gerigi di atas " +
            "dan nyalakan mode online, lalu aku bisa mengusulkan perubahannya untuk kamu setujui. " +
            "Sementara itu kamu bisa mengubahnya sendiri di tab Jadwal atau Aktivitas, atau lewat Profil > Pengingat dan mode."

    private fun sapaan(latar: LatarHanako): String {
        val waktu = when (SapaanProvider.untuk(latar.sekarang.toLocalTime())) {
            Sapaan.PAGI -> "Selamat pagi"
            Sapaan.SIANG -> "Selamat siang"
            Sapaan.SORE -> "Selamat sore"
            Sapaan.MALAM -> "Selamat malam"
        }
        val nama = RingkasData.bersih(latar.namaPengguna, 40)
        return if (nama.isEmpty()) "$waktu! Ada yang bisa kubantu?" else "$waktu, $nama! Ada yang bisa kubantu?"
    }
}
