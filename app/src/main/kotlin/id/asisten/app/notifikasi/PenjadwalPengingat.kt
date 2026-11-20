package id.asisten.app.notifikasi

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import id.asisten.app.data.pengaturan.PengaturanRepository
import id.asisten.app.data.pengingat.SuaraPengingatRepository
import id.asisten.app.data.repo.AktivitasRepository
import id.asisten.app.data.repo.JadwalRepository
import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.pengingat.DataPengingat
import id.asisten.app.domain.pengingat.JenisPengingat
import id.asisten.app.domain.pengingat.KebijakanNotifikasi
import id.asisten.app.domain.pengingat.Keputusan
import id.asisten.app.domain.pengingat.PengaturanPengingat
import id.asisten.app.domain.pengingat.PengaturanSuaraPengingat
import id.asisten.app.domain.pengingat.Pengingat
import id.asisten.app.domain.pengingat.PenyusunNotifikasi
import id.asisten.app.domain.pengingat.ProsesPengingat
import id.asisten.app.domain.pengingat.Tunda
import id.asisten.app.domain.pengingat.TundaCodec
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Connects the pure reminder logic to Android.
 *
 * One exact alarm is kept for the NEXT reminder only. When it fires (or the device restarts, the clock
 * changes, the app starts, the periodic safety-net worker runs) [proses] delivers whatever became due,
 * moves the "handled up to" marker forward, and sets the alarm for the next reminder.
 * All work is serialized by [kunci], so alarm, receivers and observer can never deliver the same reminder twice.
 */
class PenjadwalPengingat(
    private val context: Context,
    private val pengaturan: PengaturanRepository,
    private val jadwal: JadwalRepository,
    private val aktivitas: AktivitasRepository,
    private val penampil: PenampilNotifikasi,
    private val suaraPengingat: SuaraPengingatRepository,
) {
    private val kunci = Mutex()
    private val alarmManager: AlarmManager = context.getSystemService(AlarmManager::class.java)

    /** Everything the reminders are computed from, as one flow. */
    val dataPengingat: Flow<DataPengingat> = combine(
        jadwal.kegiatan,
        jadwal.jamSekolah,
        jadwal.piket,
        aktivitas.tugas,
    ) { kegiatan, jamSekolah, piket, tugas ->
        DataPengingat(kegiatan = kegiatan, tugas = tugas, jamSekolah = jamSekolah, piket = piket)
    }

    /**
     * Delivers what is due (when [kirimTerlewat]) and re-arms the alarm.
     * Pass false after the user edits data or settings: a reminder whose time already passed because of the
     * edit (for example an activity created 5 minutes before it starts) must not pop up as a stale notification.
     */
    suspend fun proses(kirimTerlewat: Boolean) {
        kunci.withLock { prosesTerkunci(kirimTerlewat) }
    }

    /** Snooze: remind again after the configured number of minutes, then re-arm the alarm. */
    suspend fun tunda(jenis: JenisPengingat, idSumber: Long, acara: Long) {
        kunci.withLock {
            val p = pengaturan.pengingat.first()
            val sekarang = System.currentTimeMillis()
            val tundaBaru = Tunda(jenis, idSumber, acara, sekarang + p.tundaMenit * 60_000L)
            pengaturan.ubahKeadaanPengingat { it.copy(tunda = TundaCodec.tambah(it.tunda, tundaBaru)) }
            prosesTerkunci(kirimTerlewat = false)
        }
    }

    /** The "Selesai" notification action for a task. */
    suspend fun tandaiTugasSelesai(id: Long) {
        kunci.withLock {
            val tugas = aktivitas.tugas.first().firstOrNull { it.id == id }
            if (tugas != null && !tugas.selesai) aktivitas.setTugasSelesai(tugas, true)
            prosesTerkunci(kirimTerlewat = false)
        }
    }

    /** Posts a test notification now, following the current mode and quiet-hours rules, and reports what happened. */
    suspend fun kirimUji(): Keputusan {
        val p = pengaturan.pengingat.first()
        val keputusan = KebijakanNotifikasi.putuskan(Prioritas.SEDANG, p, System.currentTimeMillis(), ZoneId.systemDefault())
        penampil.tampilkanUji(keputusan.nada, suaraPengingat.pengaturan.first())
        return keputusan
    }

    fun bisaAlarmPresisi(): Boolean = CekIzin.alarmPresisiDiizinkan(context)

    private suspend fun prosesTerkunci(kirimTerlewat: Boolean) {
        val sekarang = System.currentTimeMillis()
        val zona = ZoneId.systemDefault()
        val p = pengaturan.pengingat.first()
        val data = dataPengingat.first()
        val awal = pengaturan.keadaanPengingat.first()

        if (kirimTerlewat) {
            val suara = suaraPengingat.pengaturan.first()
            ProsesPengingat.jatuhTempo(data, awal.tunda, p, awal.prosesTerakhir, sekarang, zona)
                .forEach { kirim(it, data, p, sekarang, zona, suara) }
        }

        // Everything up to now counts as handled; snoozes that have passed are removed.
        val baru = pengaturan.ubahKeadaanPengingat { keadaan ->
            keadaan.copy(prosesTerakhir = sekarang, tunda = keadaan.tunda.filter { it.waktu > sekarang })
        }
        pasangBerikutnya(data, baru.tunda, p, sekarang, zona)
    }

    private fun kirim(
        pengingat: Pengingat,
        data: DataPengingat,
        p: PengaturanPengingat,
        sekarang: Long,
        zona: ZoneId,
        suara: PengaturanSuaraPengingat,
    ) {
        // The text is built from current data: a deleted, completed or moved item yields null and is skipped.
        val isi = PenyusunNotifikasi.susun(pengingat, data, sekarang, zona) ?: return
        val keputusan = KebijakanNotifikasi.putuskan(isi.prioritas, p, sekarang, zona)
        penampil.tampilkan(pengingat, isi, keputusan.nada, suara)
    }

    private fun pasangBerikutnya(data: DataPengingat, tunda: List<Tunda>, p: PengaturanPengingat, sekarang: Long, zona: ZoneId) {
        val tujuan = intentAlarm()
        val waktu = ProsesPengingat.berikutnya(data, tunda, p, sekarang, zona)
        if (waktu == null) {
            alarmManager.cancel(tujuan)
            return
        }
        if (bisaAlarmPresisi()) {
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, waktu, tujuan)
                return
            } catch (e: SecurityException) {
                // The permission was revoked between the check and the call: fall back to the inexact alarm.
                Log.w(TAG, "Alarm presisi ditolak, memakai alarm biasa", e)
            }
        }
        // Without the exact-alarm permission the system may delay this by several minutes, never drop it.
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, waktu, tujuan)
    }

    private fun intentAlarm(): PendingIntent {
        val intent = Intent(context, PenerimaAlarm::class.java).setAction(KunciIntent.AKSI_ALARM)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val TAG = "PenjadwalPengingat"
    }
}
