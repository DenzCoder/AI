package id.asisten.app.notifikasi

/** Intent actions and extra keys shared by the notification, the alarm and the receivers. */
internal object KunciIntent {
    const val AKSI_ALARM = "id.asisten.app.aksi.ALARM"
    const val AKSI_SELESAI = "id.asisten.app.aksi.SELESAI"
    const val AKSI_TUNDA = "id.asisten.app.aksi.TUNDA"

    const val EXTRA_JENIS = "id.asisten.app.extra.JENIS"
    const val EXTRA_ID_SUMBER = "id.asisten.app.extra.ID_SUMBER"
    const val EXTRA_ACARA = "id.asisten.app.extra.ACARA"
    const val EXTRA_ID_NOTIFIKASI = "id.asisten.app.extra.ID_NOTIFIKASI"

    /** Which tab MainActivity should show when opened from a notification. */
    const val EXTRA_BUKA = "id.asisten.app.extra.BUKA"
    const val TAB_JADWAL = "jadwal"
    const val TAB_AKTIVITAS = "aktivitas"
}
