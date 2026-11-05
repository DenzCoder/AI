package id.asisten.app.domain.hanako

import id.asisten.app.domain.Sapaan
import id.asisten.app.domain.SapaanProvider
import id.asisten.app.domain.pengingat.ModePengingat
import java.time.LocalTime

/**
 * The facial expressions of Hanako, taken from the character sheet (docs/referensi/hanako-lembar-karakter.png).
 * The enum value names are the file names `hanako_<name in lowercase>.webp` in res/drawable-nodpi.
 */
enum class EkspresiHanako(val label: String) {
    NORMAL("Normal"),
    SENYUM("Senyum"),
    TERTAWA("Tertawa"),
    MALU("Malu"),
    SEDIH("Sedih"),
    MARAH("Marah"),
    BINGUNG("Bingung"),
    TERKEJUT("Terkejut"),
    CEMBERUT("Cemberut"),
    MENGANTUK("Mengantuk"),
    BERPIKIR("Berpikir"),
    PANIK("Panik"),
}

/** Chooses which expression fits the situation. Pure, so the choices are unit-tested. */
object PilihEkspresi {

    /** Sleepy between 22:00 and 03:59; cheerful in the morning and afternoon; calm at midday and early evening. */
    fun untukWaktu(waktu: LocalTime): EkspresiHanako {
        if (waktu.hour >= 22 || waktu.hour < 4) return EkspresiHanako.MENGANTUK
        return when (SapaanProvider.untuk(waktu)) {
            Sapaan.PAGI -> EkspresiHanako.SENYUM
            Sapaan.SIANG -> EkspresiHanako.NORMAL
            Sapaan.SORE -> EkspresiHanako.SENYUM
            Sapaan.MALAM -> EkspresiHanako.NORMAL
        }
    }

    /**
     * Reminder screen: confused when notifications are blocked (the reminders cannot reach the user),
     * thinking in Fokus mode, sleepy in Istirahat mode, otherwise smiling.
     */
    fun untukPengingat(mode: ModePengingat, notifikasiDiizinkan: Boolean): EkspresiHanako = when {
        !notifikasiDiizinkan -> EkspresiHanako.BINGUNG
        mode == ModePengingat.FOKUS -> EkspresiHanako.BERPIKIR
        mode == ModePengingat.ISTIRAHAT -> EkspresiHanako.MENGANTUK
        else -> EkspresiHanako.SENYUM
    }
}
