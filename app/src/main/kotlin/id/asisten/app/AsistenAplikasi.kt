package id.asisten.app

import android.app.Application
import android.content.Context
import id.asisten.app.data.db.AsistenDatabase
import id.asisten.app.data.pengaturan.PengaturanRepository
import id.asisten.app.data.pengaturan.penyimpananPengaturan
import id.asisten.app.data.repo.AktivitasRepository
import id.asisten.app.data.repo.JadwalRepository
import id.asisten.app.data.repo.MemoriRepository

/** Hand-written dependency container (no DI framework, to keep the app small). Everything is lazy. */
class WadahAplikasi(context: Context) {
    private val app = context.applicationContext

    val database: AsistenDatabase by lazy { AsistenDatabase.buat(app) }
    val pengaturan: PengaturanRepository by lazy { PengaturanRepository(app.penyimpananPengaturan) }
    val memori: MemoriRepository by lazy { MemoriRepository(database.memoriDao()) }
    val jadwal: JadwalRepository by lazy { JadwalRepository(database.kegiatanDao(), database.sekolahDao()) }
    val aktivitas: AktivitasRepository by lazy { AktivitasRepository(database.tugasDao(), database.tujuanDao()) }
}

class AsistenAplikasi : Application() {
    val wadah: WadahAplikasi by lazy { WadahAplikasi(this) }
}
