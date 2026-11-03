package id.asisten.app.domain.waktu

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Epoch {
    /** Calendar date of an epoch-millisecond timestamp in [zona]. */
    fun tanggal(millis: Long, zona: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zona).toLocalDate()
}
