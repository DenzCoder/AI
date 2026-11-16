package id.asisten.app.domain.hanako

/**
 * Proposals waiting for the user's decision, stored as text in DataStore so they survive leaving the screen or
 * closing the app. A proposal is stored as the raw function call and is checked again against the current data
 * whenever it is shown or approved. Unreadable lines are skipped, never fatal.
 *
 * Line format: `id|waktu|nama|argsJson`. The JSON never contains a raw line break (see [MiniJson.kutip]).
 */
object RiwayatUsulan {
    const val MAKS = 10
    const val UMUR_MS = 24L * 60 * 60 * 1000

    fun encode(daftar: List<UsulanMentah>): String =
        daftar.joinToString("\n") { "${it.id}|${it.waktu}|${it.nama}|${it.argsJson}" }

    fun decode(teks: String?): List<UsulanMentah> {
        if (teks.isNullOrEmpty()) return emptyList()
        val hasil = ArrayList<UsulanMentah>()
        for (baris in teks.split("\n")) {
            val bagian = baris.split("|", limit = 4)
            if (bagian.size != 4) continue
            val id = bagian[0].toLongOrNull() ?: continue
            val waktu = bagian[1].toLongOrNull() ?: continue
            if (bagian[2] !in NamaAksi.SEMUA) continue
            if (MiniJson.uraiAtauNull(bagian[3]) !is Map<*, *>) continue
            hasil.add(UsulanMentah(id, waktu, bagian[2], bagian[3]))
        }
        return hasil.takeLast(MAKS)
    }

    /** Drops proposals older than [UMUR_MS]: an old proposal is probably not what the user wants any more. */
    fun segar(daftar: List<UsulanMentah>, sekarang: Long): List<UsulanMentah> =
        daftar.filter { sekarang - it.waktu in 0..UMUR_MS }

    /** The call's arguments as JSON, or null when they cannot be written (never happens for parsed JSON). */
    fun mentah(id: Long, waktu: Long, panggilan: PanggilanFungsi): UsulanMentah? =
        try {
            UsulanMentah(id, waktu, panggilan.nama, MiniJson.tulis(panggilan.args))
        } catch (e: IllegalArgumentException) {
            null
        }

    /** Adds [baru], skipping any proposal that is identical to one already waiting (same function, same arguments). */
    fun tambah(daftar: List<UsulanMentah>, baru: List<UsulanMentah>): List<UsulanMentah> {
        val hasil = ArrayList(daftar)
        for (b in baru) {
            if (hasil.none { it.nama == b.nama && it.argsJson == b.argsJson }) hasil.add(b)
        }
        return hasil.takeLast(MAKS)
    }

    fun hapus(daftar: List<UsulanMentah>, id: Long): List<UsulanMentah> = daftar.filter { it.id != id }

    /** An id that is not used by any proposal in [daftar] (ids are the creation time, bumped on a clash). */
    fun idBaru(daftar: List<UsulanMentah>, sekarang: Long): Long {
        var id = sekarang
        val dipakai = daftar.map { it.id }.toSet()
        while (id in dipakai) id++
        return id
    }

    fun panggilan(u: UsulanMentah): PanggilanFungsi? {
        val peta = MiniJson.uraiAtauNull(u.argsJson) as? Map<*, *> ?: return null
        val args = peta.entries.filter { it.key is String }.associate { it.key as String to it.value }
        return PanggilanFungsi(u.nama, args)
    }

    /**
     * What the screen shows: every stored proposal that is still valid against [data], with its description and
     * warnings. Proposals that no longer make sense (the task was deleted, the date has passed...) are left out.
     */
    fun tampil(daftar: List<UsulanMentah>, data: DataSaatIni): List<UsulanTampil> = daftar.mapNotNull { u ->
        val panggil = panggilan(u) ?: return@mapNotNull null
        val hasil = UraiAksi.urai(panggil, data) as? HasilUrai.Sah ?: return@mapNotNull null
        UsulanTampil(u.id, hasil.aksi, LabelAksi.ringkas(hasil.aksi, data), LabelAksi.peringatan(hasil.aksi, data))
    }
}

/** Turns the model's function calls into the chat message and the proposals to store. Pure, so it is unit-tested. */
object BalasanAksi {
    const val MAKS_USULAN = 5
    private const val MAKS_ALASAN_DITAMPILKAN = 3

    data class Hasil(val teks: String, val disimpan: List<PanggilanFungsi>)

    /**
     * [teksModel] is the model's text with the mood marker already removed (may be empty). The text always says
     * plainly that nothing has been done yet, whatever the model wrote.
     */
    fun susun(teksModel: String, panggilan: List<PanggilanFungsi>, data: DataSaatIni): Hasil {
        val dipakai = panggilan.take(MAKS_USULAN)
        val hasil = dipakai.map { it to UraiAksi.urai(it, data) }
        val disimpan = hasil.filter { it.second is HasilUrai.Sah }.map { it.first }
        val alasan = hasil.mapNotNull { (it.second as? HasilUrai.Tidak)?.alasan }
        val dipotong = panggilan.size - dipakai.size

        val sb = StringBuilder(teksModel.trim())
        if (disimpan.isNotEmpty()) {
            if (sb.isEmpty()) {
                sb.append("Aku siapkan ").append(disimpan.size).append(" usulan perubahan.")
            }
            sb.append("\n\nBelum ada yang diubah. Cek kartu usulan di bawah, lalu tekan Setujui atau Tolak.")
        }
        if (alasan.isNotEmpty()) {
            if (sb.isNotEmpty()) sb.append("\n\n")
            sb.append(if (alasan.size == 1) "Ada usulan yang tidak bisa kubuat: " else "Ada usulan yang tidak bisa kubuat:\n")
            if (alasan.size == 1) sb.append(alasan[0]) else {
                sb.append(alasan.take(MAKS_ALASAN_DITAMPILKAN).joinToString("\n") { "• $it" })
            }
        }
        if (dipotong > 0) {
            sb.append("\n\nUntuk keamanan, hanya $MAKS_USULAN usulan pertama yang kutampilkan.")
        }
        return Hasil(sb.toString().trim(), disimpan)
    }
}
