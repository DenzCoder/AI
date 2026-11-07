package id.asisten.app.domain.hanako

/** Indonesian texts for the Hanako chat that depend on logic (errors, status). Fixed texts live in strings.xml. */
object LabelHanako {

    fun galat(jenis: JenisGalat): String = when (jenis) {
        JenisGalat.TANPA_KUNCI ->
            "Kunci Gemini belum diisi atau tidak bisa dibaca. Buka pengaturan Hanako (ikon gerigi) lalu masukkan kuncinya."
        JenisGalat.KUNCI_TIDAK_VALID ->
            "Google menolak kunci Gemini ini (tidak valid atau sudah kedaluwarsa). Periksa kuncinya di pengaturan Hanako."
        JenisGalat.AKSES_DITOLAK ->
            "Google menolak akses (kode 401/403). Kunci mungkin dibatasi, layanannya belum aktif untuk kunci ini, " +
                "atau tidak tersedia di wilayahmu."
        JenisGalat.KUOTA ->
            "Jatah pemakaian Gemini sedang habis atau terlalu banyak permintaan. Tunggu sebentar lalu coba lagi."
        JenisGalat.MODEL_TIDAK_ADA ->
            "Model Gemini dengan nama itu tidak ditemukan. Ganti nama model di pengaturan Hanako."
        JenisGalat.PERMINTAAN_DITOLAK ->
            "Google menolak permintaan ini. Coba kalimat lain, atau periksa nama model di pengaturan Hanako."
        JenisGalat.DIBLOKIR ->
            "Google memblokir balasan untuk pesan ini. Coba ubah kalimatnya."
        JenisGalat.KOSONG ->
            "Gemini tidak mengirim jawaban (mungkin terpotong). Coba tanyakan dengan lebih singkat."
        JenisGalat.SERVER ->
            "Server Gemini sedang bermasalah. Coba lagi beberapa saat lagi."
        JenisGalat.JARINGAN ->
            "Tidak bisa terhubung ke internet. Periksa koneksimu lalu coba lagi."
        JenisGalat.WAKTU_HABIS ->
            "Gemini terlalu lama menjawab. Coba lagi."
        JenisGalat.TIDAK_DIKENAL ->
            "Terjadi masalah yang tidak dikenal saat menghubungi Gemini."
    }

    /** Errors the user fixes in the Hanako settings (key or model) rather than by retrying. */
    fun perluPengaturan(jenis: JenisGalat): Boolean = when (jenis) {
        JenisGalat.TANPA_KUNCI,
        JenisGalat.KUNCI_TIDAK_VALID,
        JenisGalat.AKSES_DITOLAK,
        JenisGalat.MODEL_TIDAK_ADA,
        JenisGalat.PERMINTAAN_DITOLAK,
        -> true
        else -> false
    }

    /** Errors where sending the same message again can succeed. */
    fun bisaDiulang(jenis: JenisGalat): Boolean = !perluPengaturan(jenis) && jenis != JenisGalat.DIBLOKIR

    fun status(online: Boolean): String =
        if (online) "Terhubung ke Gemini" else "Mode lokal: tanpa internet, hanya jadwal dan tugasmu"
}
