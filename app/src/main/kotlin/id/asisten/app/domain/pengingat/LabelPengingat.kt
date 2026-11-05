package id.asisten.app.domain.pengingat

fun ModePengingat.namaMode(): String = when (this) {
    ModePengingat.NORMAL -> "Normal"
    ModePengingat.FOKUS -> "Fokus"
    ModePengingat.ISTIRAHAT -> "Istirahat"
}

/** Why a reminder came without sound, in a phrase that follows "karena". */
fun AlasanSenyap.keterangan(): String = when (this) {
    AlasanSenyap.MODE_ISTIRAHAT -> "mode Istirahat sedang aktif"
    AlasanSenyap.MODE_FOKUS -> "mode Fokus sedang aktif"
    AlasanSenyap.JAM_TENANG -> "sekarang jam tenang"
}

/** Indonesian texts for the reminder settings screen, kept out of the UI so they can be unit-tested. */
object LabelPengingat {

    /** "Tepat waktu" or "10 menit sebelum". */
    fun selang(menit: Int): String = if (menit <= 0) "Tepat waktu" else "${durasi(menit)} sebelum"

    /** "30 menit", "1 jam", "1 jam 30 menit". */
    fun durasi(menit: Int): String = when {
        menit < 60 -> "$menit menit"
        menit % 60 == 0 -> "${menit / 60} jam"
        else -> "${menit / 60} jam ${menit % 60} menit"
    }

    /** Status line for the mode card. */
    fun statusMode(p: PengaturanPengingat, sekarang: Long): String {
        val aktif = p.modeEfektif(sekarang)
        if (aktif == ModePengingat.NORMAL) return "Tidak ada mode yang aktif."
        return "Mode ${aktif.namaMode()} aktif, ${durasi(p.sisaModeMenit(sekarang).toInt())} lagi."
    }

    const val UJI_DITOLAK =
        "Notifikasi dimatikan untuk ASISTEN, jadi notifikasi uji tidak bisa muncul. Izinkan dulu di bagian Izin."

    fun hasilUji(keputusan: Keputusan): String = when (keputusan.nada) {
        Nada.BERSUARA -> "Notifikasi uji dikirim. Kamu seharusnya mendengar suaranya."
        Nada.SENYAP -> "Notifikasi uji dikirim tanpa suara karena ${keputusan.alasan?.keterangan() ?: "pengaturan senyap"}."
    }

    /** Status line of the Hanako card at the top of the reminder screen. */
    fun sapaanHanako(p: PengaturanPengingat, sekarang: Long, notifikasiDiizinkan: Boolean): String = when {
        !notifikasiDiizinkan -> "Notifikasi sedang dimatikan, jadi pengingatku tidak akan terlihat. Izinkan dulu ya."
        !p.aktif -> "Pengingat sedang dimatikan."
        p.modeEfektif(sekarang) == ModePengingat.FOKUS -> "Fokus ya. " + statusMode(p, sekarang)
        p.modeEfektif(sekarang) == ModePengingat.ISTIRAHAT -> "Istirahat dulu. " + statusMode(p, sekarang)
        else -> "Aku akan mengingatkanmu sesuai jadwal."
    }
}
