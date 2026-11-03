package id.asisten.app.domain.aktivitas

import id.asisten.app.domain.Prioritas
import java.time.LocalDate

enum class JenisTugas { TUGAS, PR, UJIAN }

data class Tugas(
    val id: Long = 0,
    val judul: String,
    val jenis: JenisTugas = JenisTugas.TUGAS,
    val mapel: String = "",
    val tenggat: LocalDate? = null,
    val prioritas: Prioritas = Prioritas.SEDANG,
    val selesai: Boolean = false,
    /** Epoch millis when the user marked it done; null while open. */
    val selesaiPada: Long? = null,
    val catatan: String = "",
)

enum class StatusTenggat { LEWAT, HARI_INI, BESOK, MENDATANG, TANPA }

fun Tugas.statusTenggat(hariIni: LocalDate): StatusTenggat {
    val batas = tenggat ?: return StatusTenggat.TANPA
    return when {
        batas.isBefore(hariIni) -> StatusTenggat.LEWAT
        batas == hariIni -> StatusTenggat.HARI_INI
        batas == hariIni.plusDays(1) -> StatusTenggat.BESOK
        else -> StatusTenggat.MENDATANG
    }
}

enum class JangkaTujuan { PENDEK, PANJANG }

data class Tonggak(val id: Long = 0, val tujuanId: Long = 0, val judul: String, val selesai: Boolean = false)

data class Tujuan(
    val id: Long = 0,
    val judul: String,
    val jangka: JangkaTujuan = JangkaTujuan.PENDEK,
    val tenggat: LocalDate? = null,
    val selesai: Boolean = false,
    val selesaiPada: Long? = null,
    val tonggak: List<Tonggak> = emptyList(),
) {
    /** 0f..1f. Without milestones a goal is 0 until the user marks it achieved. */
    val kemajuan: Float
        get() = when {
            selesai -> 1f
            tonggak.isEmpty() -> 0f
            else -> tonggak.count { it.selesai }.toFloat() / tonggak.size
        }
}

object Aktivitas {
    /** Open items first, then by due date (no date last), then higher priority first. */
    fun urutkanTugas(daftar: List<Tugas>): List<Tugas> = daftar.sortedWith(
        compareBy<Tugas>({ it.selesai }, { it.tenggat == null }, { it.tenggat }, { -it.prioritas.ordinal }),
    )

    /** Achievement history: achieved goals, most recent first. */
    fun riwayatPencapaian(daftar: List<Tujuan>): List<Tujuan> =
        daftar.filter { it.selesai }.sortedByDescending { it.selesaiPada ?: 0L }
}
