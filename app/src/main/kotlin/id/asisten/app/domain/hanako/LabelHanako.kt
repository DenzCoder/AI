package id.asisten.app.domain.hanako

/** Indonesian texts for the Hanako chat that depend on logic (errors, status). Fixed texts live in strings.xml. */
object LabelHanako {

    /** The message for a failed request, worded for the provider that was used ([PenyediaAi.nama]). */
    fun galat(jenis: JenisGalat, penyedia: PenyediaAi = PenyediaAi.GEMINI): String {
        val n = penyedia.nama
        return when (jenis) {
            JenisGalat.TANPA_KUNCI ->
                "Kunci $n belum diisi atau tidak bisa dibaca. Buka pengaturan Hanako (ikon gerigi) lalu masukkan kuncinya."
            JenisGalat.KUNCI_TIDAK_VALID ->
                "$n menolak kunci ini (tidak valid atau sudah kedaluwarsa). Periksa kuncinya di pengaturan Hanako."
            JenisGalat.AKSES_DITOLAK ->
                "$n menolak akses. Kunci mungkin dibatasi, layanannya belum aktif untuk kunci ini, " +
                    "atau tidak tersedia di wilayahmu."
            JenisGalat.KUOTA ->
                "Jatah pemakaian $n sedang habis atau terlalu banyak permintaan. Tunggu sebentar lalu coba lagi."
            JenisGalat.SALDO_HABIS ->
                "Saldo akun $n habis. Isi saldo di situs $n, atau pilih layanan AI lain di pengaturan Hanako."
            JenisGalat.MODEL_TIDAK_ADA ->
                "Model $n dengan nama itu tidak ditemukan. Ganti nama model di pengaturan Hanako."
            JenisGalat.PERMINTAAN_DITOLAK ->
                "$n menolak permintaan ini. Coba kalimat lain, atau periksa nama model di pengaturan Hanako."
            JenisGalat.DIBLOKIR ->
                "$n memblokir balasan untuk pesan ini. Coba ubah kalimatnya."
            JenisGalat.KOSONG ->
                "$n tidak mengirim jawaban (mungkin terpotong). Coba tanyakan dengan lebih singkat."
            JenisGalat.SERVER ->
                "Server $n sedang bermasalah. Coba lagi beberapa saat lagi."
            JenisGalat.JARINGAN ->
                "Tidak bisa terhubung ke internet. Periksa koneksimu lalu coba lagi."
            JenisGalat.WAKTU_HABIS ->
                "$n terlalu lama menjawab. Coba lagi."
            JenisGalat.TIDAK_DIKENAL ->
                "Terjadi masalah yang tidak dikenal saat menghubungi $n."
        }
    }

    /** Errors the user fixes in the Hanako settings (key or model) rather than by retrying. */
    fun perluPengaturan(jenis: JenisGalat): Boolean = when (jenis) {
        JenisGalat.TANPA_KUNCI,
        JenisGalat.KUNCI_TIDAK_VALID,
        JenisGalat.AKSES_DITOLAK,
        JenisGalat.SALDO_HABIS,
        JenisGalat.MODEL_TIDAK_ADA,
        JenisGalat.PERMINTAAN_DITOLAK,
        -> true
        else -> false
    }

    /** Errors where sending the same message again can succeed. */
    fun bisaDiulang(jenis: JenisGalat): Boolean = !perluPengaturan(jenis) && jenis != JenisGalat.DIBLOKIR

    fun status(online: Boolean, penyedia: PenyediaAi = PenyediaAi.GEMINI): String =
        if (online) "Terhubung ke ${penyedia.nama}" else "Mode lokal: tanpa internet, hanya jadwal dan tugasmu"
}
