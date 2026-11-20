package id.asisten.app

import android.app.Application
import android.content.Context
import android.util.Log
import id.asisten.app.data.ai.KlienAi
import id.asisten.app.data.db.AsistenDatabase
import id.asisten.app.data.hanako.HanakoRepository
import id.asisten.app.data.hanako.PelaksanaAksi
import id.asisten.app.data.hanako.PengacakKeystore
import id.asisten.app.data.pengaturan.PengaturanRepository
import id.asisten.app.data.pengaturan.penyimpananPengaturan
import id.asisten.app.data.pengingat.SuaraPengingatRepository
import id.asisten.app.data.repo.AktivitasRepository
import id.asisten.app.data.repo.JadwalRepository
import id.asisten.app.data.repo.MemoriRepository
import id.asisten.app.data.suara.SuaraRepository
import id.asisten.app.notifikasi.PekerjaPengaman
import id.asisten.app.notifikasi.PengelolaSuara
import id.asisten.app.notifikasi.PenampilNotifikasi
import id.asisten.app.notifikasi.PenjadwalPengingat
import id.asisten.app.suara.PembacaSuara
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** Hand-written dependency container (no DI framework, to keep the app small). Everything is lazy. */
class WadahAplikasi(context: Context) {
    private val app = context.applicationContext

    val database: AsistenDatabase by lazy { AsistenDatabase.buat(app) }
    val pengaturan: PengaturanRepository by lazy { PengaturanRepository(app.penyimpananPengaturan) }
    val memori: MemoriRepository by lazy { MemoriRepository(database.memoriDao()) }
    val jadwal: JadwalRepository by lazy { JadwalRepository(database.kegiatanDao(), database.sekolahDao()) }
    val aktivitas: AktivitasRepository by lazy { AktivitasRepository(database.tugasDao(), database.tujuanDao()) }
    val hanako: HanakoRepository by lazy { HanakoRepository(app.penyimpananPengaturan, PengacakKeystore()) }
    val pelaksanaAksi: PelaksanaAksi by lazy { PelaksanaAksi(jadwal, aktivitas, pengaturan) }
    val klienAi: KlienAi by lazy { KlienAi() }
    val suara: SuaraRepository by lazy { SuaraRepository(app.penyimpananPengaturan) }
    val pembaca: PembacaSuara by lazy { PembacaSuara(app) }

    val suaraPengingat: SuaraPengingatRepository by lazy { SuaraPengingatRepository(app.penyimpananPengaturan) }
    val pengelolaSuara: PengelolaSuara by lazy { PengelolaSuara(app) }
    val penampil: PenampilNotifikasi by lazy { PenampilNotifikasi(app, pengelolaSuara) }
    val penjadwal: PenjadwalPengingat by lazy {
        PenjadwalPengingat(app, pengaturan, jadwal, aktivitas, penampil, suaraPengingat)
    }

    private val lingkup = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Starts the reminder system. Called once per process (also when an alarm or the restart receiver wakes the app).
     * 1. the WorkManager safety net and the notification channels (for the chosen sound) are set up;
     * 2. reminders that came due while the app was not running are delivered (if still recent);
     * 3. from then on every change to activities, tasks, school data or reminder settings re-arms the alarm.
     */
    fun mulaiPengingat() {
        PekerjaPengaman.jadwalkan(app)
        lingkup.launch {
            try {
                penampil.siapkanSaluran(suaraPengingat.pengaturan.first())
                penjadwal.proses(kirimTerlewat = true)
                // collectLatest + delay acts as a debounce: a burst of edits triggers one re-arm.
                // Edits never deliver stale reminders (kirimTerlewat = false).
                combine(penjadwal.dataPengingat, pengaturan.pengingat) { _, _ -> Unit }
                    .collectLatest {
                        delay(400)
                        penjadwal.proses(kirimTerlewat = false)
                    }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("WadahAplikasi", "Sistem pengingat berhenti karena galat", e)
            }
        }
    }
}

class AsistenAplikasi : Application() {
    val wadah: WadahAplikasi by lazy { WadahAplikasi(this) }

    override fun onCreate() {
        super.onCreate()
        wadah.mulaiPengingat()
    }
}
