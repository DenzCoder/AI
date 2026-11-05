package id.asisten.app.notifikasi

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.net.Uri
import android.util.Log
import id.asisten.app.MainActivity
import id.asisten.app.R
import id.asisten.app.domain.pengingat.IsiNotifikasi
import id.asisten.app.domain.pengingat.JenisPengingat
import id.asisten.app.domain.pengingat.Nada
import id.asisten.app.domain.pengingat.Pengingat

/**
 * Creates the notification channels and posts reminders.
 *
 * Two channels exist because Android fixes a channel's sound when it is created: "Pengingat" (sound and
 * vibration) and "Pengingat senyap" (shown without sound, used for quiet hours, Fokus and Istirahat).
 * Every notification offers its actions as real buttons: Selesai (tasks only), Tunda and Buka.
 */
class PenampilNotifikasi(private val context: Context) {

    private val manajer: NotificationManager = context.getSystemService(NotificationManager::class.java)

    /** Safe to call on every start: creating an existing channel changes nothing. */
    fun buatSaluran() {
        val bersuara = NotificationChannel(SALURAN_SUARA, "Pengingat", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Pengingat kegiatan, tugas, dan persiapan sekolah, dengan suara dan getar."
            enableVibration(true)
        }
        val senyap = NotificationChannel(SALURAN_SENYAP, "Pengingat senyap", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Pengingat tanpa suara: saat jam tenang, mode Fokus, atau mode Istirahat."
        }
        manajer.createNotificationChannels(listOf(bersuara, senyap))
    }

    /** Posts the reminder; does nothing when the user has switched notifications off for the app. */
    fun tampilkan(pengingat: Pengingat, isi: IsiNotifikasi, nada: Nada) {
        if (!manajer.areNotificationsEnabled()) return
        val id = pengingat.idNotifikasi
        val tab = if (pengingat.jenis == JenisPengingat.TUGAS) KunciIntent.TAB_AKTIVITAS else KunciIntent.TAB_JADWAL
        val builder = dasar(nada)
            .setContentTitle(isi.judul)
            .setContentText(isi.isi)
            .setStyle(Notification.BigTextStyle().bigText(isi.isi))
            .setContentIntent(intentBuka(id, tab, KODE_ISI))
        if (pengingat.jenis == JenisPengingat.TUGAS) {
            builder.addAction(buatAksi("Selesai", intentTindakan(KunciIntent.AKSI_SELESAI, pengingat, id, KODE_SELESAI)))
        }
        builder.addAction(buatAksi("Tunda", intentTindakan(KunciIntent.AKSI_TUNDA, pengingat, id, KODE_TUNDA)))
        builder.addAction(buatAksi("Buka", intentBuka(id, tab, KODE_BUKA)))
        kirim(id, builder.build())
    }

    /** A test notification through the same channels, so the user can check sound, silence and permissions. */
    fun tampilkanUji(nada: Nada) {
        if (!manajer.areNotificationsEnabled()) return
        val builder = dasar(nada)
            .setContentTitle("Pengingat uji ASISTEN")
            .setContentText("Jika kamu membaca ini, notifikasi berfungsi.")
            .setContentIntent(intentBuka(ID_UJI, null, KODE_ISI))
        kirim(ID_UJI, builder.build())
    }

    fun batalkan(id: Int) {
        manajer.cancel(id)
    }

    private fun dasar(nada: Nada): Notification.Builder {
        val saluran = if (nada == Nada.BERSUARA) SALURAN_SUARA else SALURAN_SENYAP
        return Notification.Builder(context, saluran)
            .setSmallIcon(R.drawable.ic_notifikasi)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setAutoCancel(true)
    }

    private fun kirim(id: Int, notifikasi: Notification) {
        try {
            manajer.notify(id, notifikasi)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notifikasi ditolak sistem", e)
        }
    }

    private fun buatAksi(judul: String, aksi: PendingIntent): Notification.Action =
        Notification.Action.Builder(Icon.createWithResource(context, R.drawable.ic_notifikasi), judul, aksi).build()

    /**
     * Opens the app, on the tab that holds the item. The data URI makes each intent unique: PendingIntents are
     * matched without looking at extras, so without it two notifications would share (and overwrite) one intent.
     */
    private fun intentBuka(id: Int, tab: String?, kode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            data = Uri.parse("asisten://pengingat/$id/buka/$kode")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            if (tab != null) putExtra(KunciIntent.EXTRA_BUKA, tab)
        }
        return PendingIntent.getActivity(context, kodePermintaan(id, kode), intent, BENDERA)
    }

    private fun intentTindakan(aksi: String, pengingat: Pengingat, id: Int, kode: Int): PendingIntent {
        val intent = Intent(context, PenerimaTindakan::class.java).apply {
            action = aksi
            data = Uri.parse("asisten://pengingat/$id/tindakan/$kode")
            putExtra(KunciIntent.EXTRA_JENIS, pengingat.jenis.name)
            putExtra(KunciIntent.EXTRA_ID_SUMBER, pengingat.idSumber)
            putExtra(KunciIntent.EXTRA_ACARA, pengingat.acara)
            putExtra(KunciIntent.EXTRA_ID_NOTIFIKASI, id)
        }
        return PendingIntent.getBroadcast(context, kodePermintaan(id, kode), intent, BENDERA)
    }

    private fun kodePermintaan(id: Int, kode: Int): Int = id * 8 + kode

    companion object {
        const val SALURAN_SUARA = "pengingat"
        const val SALURAN_SENYAP = "pengingat_senyap"
        private const val TAG = "PenampilNotifikasi"
        private const val ID_UJI = 1
        private const val KODE_ISI = 0
        private const val KODE_SELESAI = 1
        private const val KODE_TUNDA = 2
        private const val KODE_BUKA = 3
        private const val BENDERA = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    }
}
