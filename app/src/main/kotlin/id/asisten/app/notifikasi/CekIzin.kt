package id.asisten.app.notifikasi

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/** What the device currently allows. Read fresh each time: the user can change these in system settings at any moment. */
object CekIzin {

    /** True when notifications are allowed for the app (Android 13+: runtime permission; older: system switch). */
    fun notifikasiDiizinkan(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    /** Only Android 13 (API 33) and newer ask for the notification permission at runtime. Android 11 never does. */
    fun perluMintaIzinNotifikasi(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /** Android 11 needs no permission for exact alarms; Android 12+ can have it denied (always denied by default on 14+). */
    fun alarmPresisiDiizinkan(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** Only Android 12+ has a settings page for the exact-alarm permission. */
    fun adaHalamanIzinAlarm(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
