package id.asisten.app.notifikasi

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import id.asisten.app.AsistenAplikasi
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking

/**
 * WorkManager safety net: every few hours it runs the same step as the alarm. If an alarm was lost
 * (some vendors kill background apps aggressively), the next run delivers recent reminders and re-arms the alarm.
 * It is deliberately a plain [Worker]: doWork already runs on a background thread, so no coroutine
 * extension library is needed.
 */
class PekerjaPengaman(context: Context, parameter: WorkerParameters) : Worker(context, parameter) {

    override fun doWork(): Result {
        val penjadwal = (applicationContext as AsistenAplikasi).wadah.penjadwal
        return try {
            runBlocking { penjadwal.proses(kirimTerlewat = true) }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val NAMA_KERJA = "pengingat_pengaman"
        private const val ULANG_JAM = 3L

        /** KEEP: starting the app again does not reset the 3-hour cycle. */
        fun jadwalkan(context: Context) {
            val permintaan = PeriodicWorkRequest.Builder(PekerjaPengaman::class.java, ULANG_JAM, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAMA_KERJA,
                ExistingPeriodicWorkPolicy.KEEP,
                permintaan,
            )
        }
    }
}
