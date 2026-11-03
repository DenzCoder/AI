package id.asisten.app.domain

import id.asisten.app.domain.aktivitas.JangkaTujuan
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.jadwal.ModeKalender
import id.asisten.app.domain.jadwal.SumberItem
import id.asisten.app.domain.memori.KategoriMemori

/** Indonesian display labels, independent of the device locale. */
fun Prioritas.label(): String = when (this) {
    Prioritas.RENDAH -> "Rendah"
    Prioritas.SEDANG -> "Sedang"
    Prioritas.TINGGI -> "Tinggi"
}

fun JenisTugas.label(): String = when (this) {
    JenisTugas.TUGAS -> "Tugas"
    JenisTugas.PR -> "PR"
    JenisTugas.UJIAN -> "Ujian"
}

fun JangkaTujuan.label(): String = when (this) {
    JangkaTujuan.PENDEK -> "Jangka pendek"
    JangkaTujuan.PANJANG -> "Jangka panjang"
}

fun KategoriMemori.label(): String = when (this) {
    KategoriMemori.PREFERENSI -> "Preferensi pribadi"
    KategoriMemori.KEBIASAAN -> "Kebiasaan dan rutinitas"
    KategoriMemori.TUJUAN -> "Tujuan dan rencana"
    KategoriMemori.KONTEKS -> "Konteks percakapan penting"
}

fun SumberItem.label(): String = when (this) {
    SumberItem.KEGIATAN -> "Kegiatan"
    SumberItem.PELAJARAN -> "Pelajaran"
    SumberItem.ISTIRAHAT -> "Istirahat"
}

fun ModeKalender.label(): String = when (this) {
    ModeKalender.HARI -> "Hari"
    ModeKalender.MINGGU -> "Minggu"
    ModeKalender.BULAN -> "Bulan"
}
