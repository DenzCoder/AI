package id.asisten.app.domain

import java.time.LocalTime

/** Time-of-day greeting category. The UI maps each value to an Indonesian string resource. */
enum class Sapaan { PAGI, SIANG, SORE, MALAM }

object SapaanProvider {
    /**
     * 04:00-10:59 pagi, 11:00-14:59 siang, 15:00-17:59 sore, otherwise malam.
     */
    fun untuk(waktu: LocalTime): Sapaan = when (waktu.hour) {
        in 4..10 -> Sapaan.PAGI
        in 11..14 -> Sapaan.SIANG
        in 15..17 -> Sapaan.SORE
        else -> Sapaan.MALAM
    }
}
