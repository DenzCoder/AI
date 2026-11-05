package id.asisten.app.domain.pengingat

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Piket

enum class JenisPengingat { KEGIATAN, TUGAS, SEKOLAH }

/**
 * One reminder occurrence.
 *
 * - [waktu]: when the reminder fires (epoch millis).
 * - [acara]: the instant the reminder is about (epoch millis): activity start, the due date at 00:00,
 *   or school entry. It identifies the occurrence, so a changed schedule invalidates old reminders.
 * - [idSumber]: activity id, task id, or the ISO weekday (1..7) for school.
 */
data class Pengingat(val jenis: JenisPengingat, val idSumber: Long, val waktu: Long, val acara: Long) {
    /** Stable notification id: the same item and occurrence always replace their own notification. */
    val idNotifikasi: Int
        get() = ((jenis.ordinal * 31L + idSumber) * 31L + acara).hashCode() and Int.MAX_VALUE
}

/** Everything the reminder logic reads, as plain domain models. */
data class DataPengingat(
    val kegiatan: List<Kegiatan> = emptyList(),
    val tugas: List<Tugas> = emptyList(),
    val jamSekolah: List<JamSekolah> = emptyList(),
    val piket: List<Piket> = emptyList(),
)

/** Text and priority shown in the notification. */
data class IsiNotifikasi(val judul: String, val isi: String, val prioritas: Prioritas)

/** A snoozed reminder: fires again at [waktu] for the same occurrence. */
data class Tunda(val jenis: JenisPengingat, val idSumber: Long, val acara: Long, val waktu: Long)

/**
 * Small piece of scheduler state kept in DataStore.
 * [prosesTerakhir]: every reminder at or before this instant has already been handled (0 = never run).
 */
data class KeadaanPengingat(val prosesTerakhir: Long = 0L, val tunda: List<Tunda> = emptyList())

/** Compact text codec for the snooze list: `JENIS,idSumber,acara,waktu;...` Malformed entries are dropped. */
object TundaCodec {
    const val MAKS = 50

    fun encode(daftar: List<Tunda>): String =
        daftar.takeLast(MAKS).joinToString(";") { "${it.jenis.name},${it.idSumber},${it.acara},${it.waktu}" }

    fun decode(teks: String?): List<Tunda> {
        if (teks.isNullOrBlank()) return emptyList()
        return teks.split(';').mapNotNull { bagian ->
            val kolom = bagian.split(',')
            if (kolom.size != 4) return@mapNotNull null
            val jenis = JenisPengingat.entries.firstOrNull { it.name == kolom[0] } ?: return@mapNotNull null
            val id = kolom[1].toLongOrNull() ?: return@mapNotNull null
            val acara = kolom[2].toLongOrNull() ?: return@mapNotNull null
            val waktu = kolom[3].toLongOrNull() ?: return@mapNotNull null
            Tunda(jenis, id, acara, waktu)
        }.takeLast(MAKS)
    }

    /** Snoozing the same occurrence again replaces the earlier snooze. The list stays bounded. */
    fun tambah(daftar: List<Tunda>, baru: Tunda): List<Tunda> {
        val tanpaSama = daftar.filterNot {
            it.jenis == baru.jenis && it.idSumber == baru.idSumber && it.acara == baru.acara
        }
        return (tanpaSama + baru).takeLast(MAKS)
    }
}
