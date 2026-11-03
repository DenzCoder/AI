package id.asisten.app.domain.jadwal

import id.asisten.app.domain.Prioritas
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * A personal activity. Exactly one of [tanggal] (one-time) or [hariMingguan] (weekly recurring) is set.
 */
data class Kegiatan(
    val id: Long = 0,
    val judul: String,
    val tanggal: LocalDate? = null,
    val hariMingguan: DayOfWeek? = null,
    val mulai: LocalTime,
    val selesai: LocalTime,
    val prioritas: Prioritas = Prioritas.SEDANG,
    val catatan: String = "",
) {
    fun muncul(pada: LocalDate): Boolean = when {
        tanggal != null -> tanggal == pada
        hariMingguan != null -> pada.dayOfWeek == hariMingguan
        else -> false
    }

    /** A concrete date used to check conflicts for this activity, starting from [dari]. */
    fun tanggalAcuan(dari: LocalDate): LocalDate = when {
        tanggal != null -> tanggal
        hariMingguan != null -> dari.with(TemporalAdjusters.nextOrSame(hariMingguan))
        else -> dari
    }
}

/** School entry and dismissal time for one weekday. A weekday without a record has no school configured. */
data class JamSekolah(val hari: DayOfWeek, val masuk: LocalTime, val pulang: LocalTime)

/** A timetable slot. Breaks are slots with [istirahat] = true. */
data class Pelajaran(
    val id: Long = 0,
    val hari: DayOfWeek,
    val mulai: LocalTime,
    val selesai: LocalTime,
    val nama: String,
    val guru: String = "",
    val ruang: String = "",
    val istirahat: Boolean = false,
)

/** Duty roster (piket) entry: which duty falls on which weekday. */
data class Piket(val id: Long = 0, val hari: DayOfWeek, val keterangan: String)

enum class SumberItem { KEGIATAN, PELAJARAN, ISTIRAHAT }

/** One block on the day timeline, merged from activities and the school timetable. */
data class ItemHarian(
    val kunci: String,
    val judul: String,
    val keterangan: String,
    val mulai: LocalTime,
    val selesai: LocalTime,
    val sumber: SumberItem,
    val prioritas: Prioritas?,
    val bentrok: Boolean = false,
)

object ValidasiJadwal {
    /** End must be strictly after start (activities do not cross midnight). */
    fun waktuValid(mulai: LocalTime, selesai: LocalTime): Boolean = selesai.isAfter(mulai)
}

object PenyusunHarian {

    fun saling(a: ItemHarian, b: ItemHarian): Boolean =
        a.sumber != SumberItem.ISTIRAHAT && b.sumber != SumberItem.ISTIRAHAT &&
            a.mulai < b.selesai && b.mulai < a.selesai

    /** Builds the sorted timeline for [tanggal] and flags overlapping blocks (breaks never conflict). */
    fun susun(tanggal: LocalDate, kegiatan: List<Kegiatan>, pelajaran: List<Pelajaran>): List<ItemHarian> {
        val dasar = buildList {
            kegiatan.filter { it.muncul(tanggal) }.forEach {
                add(
                    ItemHarian(
                        kunci = "k${it.id}", judul = it.judul, keterangan = it.catatan,
                        mulai = it.mulai, selesai = it.selesai,
                        sumber = SumberItem.KEGIATAN, prioritas = it.prioritas,
                    ),
                )
            }
            pelajaran.filter { it.hari == tanggal.dayOfWeek }.forEach {
                add(
                    ItemHarian(
                        kunci = "p${it.id}", judul = it.nama,
                        keterangan = listOf(it.guru, it.ruang).filter { s -> s.isNotBlank() }.joinToString(" · "),
                        mulai = it.mulai, selesai = it.selesai,
                        sumber = if (it.istirahat) SumberItem.ISTIRAHAT else SumberItem.PELAJARAN,
                        prioritas = null,
                    ),
                )
            }
        }.sortedWith(compareBy({ it.mulai }, { it.selesai }))
        return dasar.map { a -> a.copy(bentrok = dasar.any { b -> b.kunci != a.kunci && saling(a, b) }) }
    }

    /** Existing blocks that would overlap [draf] on its reference date (excluding [draf] itself when editing). */
    fun bentrokDengan(
        draf: Kegiatan,
        tanggalAcuan: LocalDate,
        kegiatan: List<Kegiatan>,
        pelajaran: List<Pelajaran>,
    ): List<ItemHarian> {
        val lain = susun(tanggalAcuan, kegiatan.filter { it.id == 0L || it.id != draf.id }, pelajaran)
        val blok = ItemHarian(
            kunci = "draf", judul = draf.judul, keterangan = "", mulai = draf.mulai, selesai = draf.selesai,
            sumber = SumberItem.KEGIATAN, prioritas = draf.prioritas,
        )
        return lain.filter { saling(blok, it) }
    }

    /**
     * Earliest free start time of [durasiMenit] minutes between [dari] and [sampai], or null when none fits.
     * Used for adaptive rescheduling suggestions.
     */
    fun saranSlot(
        item: List<ItemHarian>,
        durasiMenit: Int,
        dari: LocalTime = LocalTime.of(6, 0),
        sampai: LocalTime = LocalTime.of(22, 0),
    ): LocalTime? {
        if (durasiMenit <= 0) return null
        var kursor = dari.toSecondOfDay() / 60
        val batas = sampai.toSecondOfDay() / 60
        val sibuk = item.filter { it.sumber != SumberItem.ISTIRAHAT }.sortedBy { it.mulai }
        for (blok in sibuk) {
            val mulai = blok.mulai.toSecondOfDay() / 60
            val akhir = blok.selesai.toSecondOfDay() / 60
            if (mulai - kursor >= durasiMenit && kursor + durasiMenit <= batas) return jam(kursor)
            kursor = maxOf(kursor, akhir)
        }
        return if (batas - kursor >= durasiMenit) jam(kursor) else null
    }

    private fun jam(menit: Int): LocalTime = LocalTime.ofSecondOfDay(menit * 60L)
}

object Kalender {
    /** Monday..Sunday of the ISO week containing [tanggal]. */
    fun hariDalamMinggu(tanggal: LocalDate): List<LocalDate> {
        val senin = tanggal.with(DayOfWeek.MONDAY)
        return (0L..6L).map { senin.plusDays(it) }
    }

    /** Month grid, Monday first. Leading/trailing nulls pad the list to full weeks (size % 7 == 0). */
    fun gridBulan(bulan: YearMonth): List<LocalDate?> {
        val awal = bulan.atDay(1)
        val kosongDepan = awal.dayOfWeek.value - 1
        val sel = MutableList<LocalDate?>(kosongDepan) { null }
        for (hari in 1..bulan.lengthOfMonth()) sel.add(bulan.atDay(hari))
        while (sel.size % 7 != 0) sel.add(null)
        return sel
    }
}
