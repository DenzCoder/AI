package id.asisten.app.domain.pengingat

import id.asisten.app.domain.waktu.Epoch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

internal fun LocalDate.millis(jam: LocalTime, zona: ZoneId): Long =
    LocalDateTime.of(this, jam).atZone(zona).toInstant().toEpochMilli()

internal fun LocalDate.millisMenit(menit: Int, zona: ZoneId): Long =
    millis(LocalTime.of(menit / 60, menit % 60), zona)

internal fun LocalDate.awalHariMillis(zona: ZoneId): Long = atStartOfDay(zona).toInstant().toEpochMilli()

/** Pure schedule math: which reminders fire in a time window. No Android, no clock, no storage. */
object PenghitungPengingat {
    private const val MENIT = 60_000L

    /**
     * Regular reminders with `dari < waktu <= sampai`, sorted by time.
     * Activities (one-time and weekly), open tasks with a due date (day before and due day),
     * and school entry (with the duty roster of that weekday) are covered.
     * Returns nothing when reminders are switched off.
     */
    fun hitung(
        data: DataPengingat,
        p: PengaturanPengingat,
        dari: Long,
        sampai: Long,
        zona: ZoneId,
    ): List<Pengingat> {
        if (!p.aktif || sampai <= dari) return emptyList()
        val hasil = ArrayList<Pengingat>()

        // A reminder is never later than its occurrence (at most a day earlier), so scanning the dates
        // from the window start to one day past its end finds every occurrence that can matter.
        val awal = Epoch.tanggal(dari, zona)
        val akhir = Epoch.tanggal(sampai, zona).plusDays(1)

        for (k in data.kegiatan) {
            val tanggalan: List<LocalDate> = when {
                k.tanggal != null -> listOf(k.tanggal)
                k.hariMingguan != null -> tanggalDenganHari(awal, akhir, k.hariMingguan)
                else -> emptyList()
            }
            for (tgl in tanggalan) {
                val acara = tgl.millis(k.mulai, zona)
                hasil.add(Pengingat(JenisPengingat.KEGIATAN, k.id, acara - p.menitSebelumKegiatan * MENIT, acara))
            }
        }

        if (p.tugasAktif) {
            for (t in data.tugas) {
                if (t.selesai) continue
                val tenggat = t.tenggat ?: continue
                val acara = tenggat.awalHariMillis(zona)
                hasil.add(
                    Pengingat(JenisPengingat.TUGAS, t.id, tenggat.minusDays(1).millisMenit(p.menitTugasSebelum, zona), acara),
                )
                hasil.add(Pengingat(JenisPengingat.TUGAS, t.id, tenggat.millisMenit(p.menitTugasHariH, zona), acara))
            }
        }

        if (p.sekolahAktif) {
            var tgl = awal
            while (!tgl.isAfter(akhir)) {
                val jam = data.jamSekolah.firstOrNull { it.hari == tgl.dayOfWeek }
                if (jam != null) {
                    val acara = tgl.millis(jam.masuk, zona)
                    hasil.add(
                        Pengingat(JenisPengingat.SEKOLAH, jam.hari.value.toLong(), acara - p.menitSebelumSekolah * MENIT, acara),
                    )
                }
                tgl = tgl.plusDays(1)
            }
        }

        return hasil
            .filter { it.waktu > dari && it.waktu <= sampai }
            .sortedWith(compareBy({ it.waktu }, { it.jenis.ordinal }, { it.idSumber }))
    }

    /** Snoozed reminders due in `dari < waktu <= sampai`. */
    fun hitungTunda(tunda: List<Tunda>, dari: Long, sampai: Long): List<Pengingat> =
        tunda.filter { it.waktu > dari && it.waktu <= sampai }
            .map { Pengingat(it.jenis, it.idSumber, it.waktu, it.acara) }

    /** Regular and snoozed reminders together, sorted by time. Snoozes are ignored while reminders are off. */
    fun semua(
        data: DataPengingat,
        tunda: List<Tunda>,
        p: PengaturanPengingat,
        dari: Long,
        sampai: Long,
        zona: ZoneId,
    ): List<Pengingat> {
        if (!p.aktif) return emptyList()
        return (hitung(data, p, dari, sampai, zona) + hitungTunda(tunda, dari, sampai))
            .sortedWith(compareBy({ it.waktu }, { it.jenis.ordinal }, { it.idSumber }))
    }

    private fun tanggalDenganHari(dari: LocalDate, sampai: LocalDate, hari: DayOfWeek): List<LocalDate> {
        val hasil = ArrayList<LocalDate>()
        var tgl = dari
        while (!tgl.isAfter(sampai)) {
            if (tgl.dayOfWeek == hari) hasil.add(tgl)
            tgl = tgl.plusDays(1)
        }
        return hasil
    }
}

/** What to deliver now, and when to wake up next. */
object ProsesPengingat {
    /** Reminders older than this are dropped instead of delivered late (the device was off, the app was stopped). */
    const val JENDELA_TERLEWAT_MS = 120L * 60_000L

    const val JANGKAUAN_HARI = 14

    /**
     * Reminders that became due in `(penanda, sekarang]` and are still recent enough to be useful.
     * [penanda] is the instant up to which everything was handled before (0 = the scheduler never ran:
     * nothing is delivered, so a fresh install or cleared data cannot flood the user).
     * If the system clock moved backwards (`sekarang <= penanda`) nothing is delivered.
     * When one item has several due reminders (same notification id), only the latest is kept.
     */
    fun jatuhTempo(
        data: DataPengingat,
        tunda: List<Tunda>,
        p: PengaturanPengingat,
        penanda: Long,
        sekarang: Long,
        zona: ZoneId,
    ): List<Pengingat> {
        if (penanda <= 0L || sekarang <= penanda) return emptyList()
        val dari = maxOf(penanda, sekarang - JENDELA_TERLEWAT_MS)
        return PenghitungPengingat.semua(data, tunda, p, dari, sekarang, zona)
            .groupBy { it.idNotifikasi }
            .mapNotNull { (_, daftar) -> daftar.maxByOrNull { it.waktu } }
            .sortedBy { it.waktu }
    }

    /** The next reminder strictly after [setelah] within [JANGKAUAN_HARI] days, or null (then no alarm is set). */
    fun berikutnya(
        data: DataPengingat,
        tunda: List<Tunda>,
        p: PengaturanPengingat,
        setelah: Long,
        zona: ZoneId,
    ): Long? {
        val sampai = setelah + JANGKAUAN_HARI * 86_400_000L
        return PenghitungPengingat.semua(data, tunda, p, setelah, sampai, zona).firstOrNull()?.waktu
    }
}
