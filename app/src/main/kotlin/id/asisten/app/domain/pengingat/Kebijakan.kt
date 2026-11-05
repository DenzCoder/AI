package id.asisten.app.domain.pengingat

import id.asisten.app.domain.Prioritas
import java.time.Instant
import java.time.ZoneId

enum class Nada { BERSUARA, SENYAP }

enum class AlasanSenyap { MODE_ISTIRAHAT, MODE_FOKUS, JAM_TENANG }

data class Keputusan(val nada: Nada, val alasan: AlasanSenyap? = null)

object JamTenang {
    /**
     * True when [menitHari] lies in the window [mulai, selesai). A window that crosses midnight
     * (for example 22:00 to 06:00) is handled. An empty window (mulai == selesai) never matches.
     */
    fun dalam(menitHari: Int, mulai: Int, selesai: Int): Boolean = when {
        mulai == selesai -> false
        mulai < selesai -> menitHari >= mulai && menitHari < selesai
        else -> menitHari >= mulai || menitHari < selesai
    }

    fun menitHari(millis: Long, zona: ZoneId): Int {
        val waktu = Instant.ofEpochMilli(millis).atZone(zona).toLocalTime()
        return waktu.hour * 60 + waktu.minute
    }
}

/**
 * Decides whether a reminder makes a sound. A reminder is never dropped: when it must be quiet it is
 * still shown, just without sound or vibration, so nothing is lost while the user is focused or resting.
 *
 * Order of rules: Istirahat silences everything; Fokus silences everything except Tinggi priority;
 * quiet hours silence everything; otherwise the reminder makes a sound.
 * [sekarang] is the delivery time, not the scheduled time, so late reminders follow the current situation.
 */
object KebijakanNotifikasi {
    fun putuskan(
        prioritas: Prioritas,
        pengaturan: PengaturanPengingat,
        sekarang: Long,
        zona: ZoneId,
    ): Keputusan {
        when (pengaturan.modeEfektif(sekarang)) {
            ModePengingat.ISTIRAHAT -> return Keputusan(Nada.SENYAP, AlasanSenyap.MODE_ISTIRAHAT)
            ModePengingat.FOKUS ->
                if (prioritas != Prioritas.TINGGI) return Keputusan(Nada.SENYAP, AlasanSenyap.MODE_FOKUS)
            ModePengingat.NORMAL -> Unit
        }
        if (pengaturan.jamTenangAktif &&
            JamTenang.dalam(
                JamTenang.menitHari(sekarang, zona),
                pengaturan.jamTenangMulai,
                pengaturan.jamTenangSelesai,
            )
        ) {
            return Keputusan(Nada.SENYAP, AlasanSenyap.JAM_TENANG)
        }
        return Keputusan(Nada.BERSUARA)
    }
}
