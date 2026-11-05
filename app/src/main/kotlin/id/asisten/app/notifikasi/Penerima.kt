package id.asisten.app.notifikasi

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import id.asisten.app.AsistenAplikasi
import id.asisten.app.domain.pengingat.JenisPengingat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

private const val TAG = "PenerimaPengingat"

private fun Context.wadah() = (applicationContext as AsistenAplikasi).wadah

/**
 * Runs [blok] off the main thread while keeping the receiver alive (goAsync), and always finishes the broadcast.
 * An error is logged instead of crashing the app from a background broadcast.
 */
private fun BroadcastReceiver.kerjakan(blok: suspend () -> Unit) {
    val hasil = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
        try {
            blok()
        } catch (e: Exception) {
            Log.e(TAG, "Gagal memproses pengingat", e)
        } finally {
            hasil.finish()
        }
    }
}

/** The exact alarm for the next reminder fired. */
class PenerimaAlarm : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val penjadwal = context.wadah().penjadwal
        kerjakan { penjadwal.proses(kirimTerlewat = true) }
    }
}

/**
 * Notification buttons. The notification is dismissed immediately; the work (marking a task done, saving a snooze)
 * follows. "Buka" is a plain activity intent and does not come through here.
 */
class PenerimaTindakan : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val jenis = JenisPengingat.entries.firstOrNull { it.name == intent.getStringExtra(KunciIntent.EXTRA_JENIS) } ?: return
        val idSumber = intent.getLongExtra(KunciIntent.EXTRA_ID_SUMBER, -1L)
        val acara = intent.getLongExtra(KunciIntent.EXTRA_ACARA, -1L)
        val idNotifikasi = intent.getIntExtra(KunciIntent.EXTRA_ID_NOTIFIKASI, -1)
        if (idNotifikasi >= 0) context.getSystemService(NotificationManager::class.java).cancel(idNotifikasi)
        val penjadwal = context.wadah().penjadwal
        when (intent.action) {
            KunciIntent.AKSI_SELESAI -> {
                if (jenis == JenisPengingat.TUGAS) kerjakan { penjadwal.tandaiTugasSelesai(idSumber) }
            }
            KunciIntent.AKSI_TUNDA -> kerjakan { penjadwal.tunda(jenis, idSumber, acara) }
        }
    }
}

/**
 * Alarms do not survive a restart, a time change or an app update, so they are re-armed here.
 * After a restart, an update or a permission change the reminders that came due meanwhile are delivered
 * (when still recent). After a clock or time-zone change nothing is delivered, only re-armed.
 */
class PenerimaSistem : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val kirimTerlewat = when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
            -> true
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            -> false
            else -> return
        }
        val penjadwal = context.wadah().penjadwal
        kerjakan { penjadwal.proses(kirimTerlewat) }
    }
}
