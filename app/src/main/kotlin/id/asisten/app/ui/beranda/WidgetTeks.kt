package id.asisten.app.ui.beranda

import androidx.annotation.StringRes
import id.asisten.app.R
import id.asisten.app.domain.dasbor.WidgetBeranda

@StringRes
fun WidgetBeranda.judul(): Int = when (this) {
    WidgetBeranda.JADWAL_HARI_INI -> R.string.widget_jadwal_judul
    WidgetBeranda.TUGAS -> R.string.widget_tugas_judul
    WidgetBeranda.SEKOLAH -> R.string.widget_sekolah_judul
    WidgetBeranda.SHOLAT -> R.string.widget_sholat_judul
    WidgetBeranda.KEBIASAAN -> R.string.widget_kebiasaan_judul
    WidgetBeranda.KEUANGAN -> R.string.widget_keuangan_judul
    WidgetBeranda.TUJUAN -> R.string.widget_tujuan_judul
}

/** Honest empty-state text: the data features behind each card arrive in later phases. */
@StringRes
fun WidgetBeranda.keterangan(): Int = when (this) {
    WidgetBeranda.JADWAL_HARI_INI -> R.string.widget_jadwal_ket
    WidgetBeranda.TUGAS -> R.string.widget_tugas_ket
    WidgetBeranda.SEKOLAH -> R.string.widget_sekolah_ket
    WidgetBeranda.SHOLAT -> R.string.widget_sholat_ket
    WidgetBeranda.KEBIASAAN -> R.string.widget_kebiasaan_ket
    WidgetBeranda.KEUANGAN -> R.string.widget_keuangan_ket
    WidgetBeranda.TUJUAN -> R.string.widget_tujuan_ket
}
