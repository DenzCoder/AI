package id.asisten.app.domain.pengingat

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.StatusTenggat
import id.asisten.app.domain.aktivitas.statusTenggat
import id.asisten.app.domain.label
import id.asisten.app.domain.waktu.Epoch
import id.asisten.app.domain.waktu.NamaHari
import id.asisten.app.domain.waktu.Waktu
import java.time.DayOfWeek
import java.time.Instant
import java.time.ZoneId

/** An upcoming reminder with its text, for the "Pengingat berikutnya" list. */
data class PengingatAkanDatang(val pengingat: Pengingat, val judul: String, val isi: String)

/**
 * Builds the Indonesian notification text from the CURRENT data. It returns null when the reminder no longer
 * applies: the item was deleted, completed, or moved, so an old alarm or snooze can never announce something stale.
 */
object PenyusunNotifikasi {

    fun susun(p: Pengingat, data: DataPengingat, sekarang: Long, zona: ZoneId): IsiNotifikasi? = when (p.jenis) {
        JenisPengingat.KEGIATAN -> kegiatan(p, data, sekarang, zona)
        JenisPengingat.TUGAS -> tugas(p, data, sekarang, zona)
        JenisPengingat.SEKOLAH -> sekolah(p, data, sekarang, zona)
    }

    private fun kegiatan(p: Pengingat, data: DataPengingat, sekarang: Long, zona: ZoneId): IsiNotifikasi? {
        val k = data.kegiatan.firstOrNull { it.id == p.idSumber } ?: return null
        val tanggal = Epoch.tanggal(p.acara, zona)
        if (!k.muncul(tanggal) || tanggal.millis(k.mulai, zona) != p.acara) return null
        val sisa = Math.floorDiv(p.acara - sekarang, 60_000L)
        val kapan = when {
            sisa > 0 -> "Mulai ${menitLagi(sisa)}"
            sisa == 0L -> "Mulai sekarang"
            else -> "Sudah dimulai"
        }
        val bagian = listOf(kapan, Waktu.rentang(k.mulai, k.selesai), k.catatan.trim().ifEmpty { null })
        return IsiNotifikasi(k.judul, bagian.filterNotNull().joinToString(" · "), k.prioritas)
    }

    private fun tugas(p: Pengingat, data: DataPengingat, sekarang: Long, zona: ZoneId): IsiNotifikasi? {
        val t = data.tugas.firstOrNull { it.id == p.idSumber } ?: return null
        if (t.selesai) return null
        val tenggat = t.tenggat ?: return null
        if (tenggat.awalHariMillis(zona) != p.acara) return null
        val hariIni = Epoch.tanggal(sekarang, zona)
        val status = when (t.statusTenggat(hariIni)) {
            StatusTenggat.HARI_INI -> "Tenggat hari ini"
            StatusTenggat.BESOK -> "Tenggat besok"
            StatusTenggat.MENDATANG -> "Tenggat ${NamaHari.untuk(tenggat.dayOfWeek)}, ${Waktu.formatTanggal(tenggat)}"
            StatusTenggat.LEWAT -> "Tenggat sudah lewat (${Waktu.formatTanggal(tenggat)})"
            StatusTenggat.TANPA -> return null
        }
        val isi = listOf(t.mapel.trim().ifEmpty { null }, status).filterNotNull().joinToString(" · ")
        return IsiNotifikasi("${t.jenis.label()}: ${t.judul}", isi, t.prioritas)
    }

    private fun sekolah(p: Pengingat, data: DataPengingat, sekarang: Long, zona: ZoneId): IsiNotifikasi? {
        if (p.idSumber !in 1L..7L) return null
        val hari = DayOfWeek.of(p.idSumber.toInt())
        val tanggal = Epoch.tanggal(p.acara, zona)
        if (tanggal.dayOfWeek != hari) return null
        val jam = data.jamSekolah.firstOrNull { it.hari == hari } ?: return null
        if (tanggal.millis(jam.masuk, zona) != p.acara) return null
        val sisa = Math.floorDiv(p.acara - sekarang, 60_000L)
        val piket = data.piket.filter { it.hari == hari }.map { it.keterangan.trim() }.filter { it.isNotEmpty() }
        val bagian = listOf(
            "Masuk pukul ${Waktu.formatJam(jam.masuk)}",
            if (sisa > 0) menitLagi(sisa) else null,
            if (piket.isNotEmpty()) "Piket: ${piket.joinToString(", ")}" else null,
        )
        return IsiNotifikasi("Persiapan sekolah", bagian.filterNotNull().joinToString(" · "), Prioritas.SEDANG)
    }

    /** "10 menit lagi", "2 jam lagi", "1 jam 5 menit lagi". */
    fun menitLagi(menit: Long): String = when {
        menit < 60 -> "$menit menit lagi"
        menit % 60 == 0L -> "${menit / 60} jam lagi"
        else -> "${menit / 60} jam ${menit % 60} menit lagi"
    }

    /** "Sen, 04-11-2026 14:50" in [zona]. */
    fun labelWaktu(millis: Long, zona: ZoneId): String {
        val w = Instant.ofEpochMilli(millis).atZone(zona)
        return "${NamaHari.singkat(w.dayOfWeek)}, ${Waktu.formatTanggal(w.toLocalDate())} ${Waktu.formatJam(w.toLocalTime())}"
    }

    /** The next [maks] reminders within [hari] days, with the text they will show. Used by the settings screen. */
    fun akanDatang(
        data: DataPengingat,
        tunda: List<Tunda>,
        p: PengaturanPengingat,
        sekarang: Long,
        zona: ZoneId,
        maks: Int = 5,
        hari: Int = ProsesPengingat.JANGKAUAN_HARI,
    ): List<PengingatAkanDatang> =
        PenghitungPengingat.semua(data, tunda, p, sekarang, sekarang + hari * 86_400_000L, zona)
            .asSequence()
            .mapNotNull { r -> susun(r, data, r.waktu, zona)?.let { PengingatAkanDatang(r, it.judul, it.isi) } }
            .take(maks)
            .toList()
}
