package id.asisten.app.data.repo

import id.asisten.app.data.db.KegiatanDao
import id.asisten.app.data.db.MemoriDao
import id.asisten.app.data.db.SekolahDao
import id.asisten.app.data.db.TugasDao
import id.asisten.app.data.db.TujuanDao
import id.asisten.app.data.db.keDomain
import id.asisten.app.data.db.keEntitas
import id.asisten.app.domain.aktivitas.Tonggak
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.aktivitas.Tujuan
import id.asisten.app.domain.aktivitas.denganSelesai
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.Piket
import id.asisten.app.domain.memori.ItemMemori
import id.asisten.app.domain.memori.ValidasiMemori
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/*
 * Repositories expose domain models only. Forms validate input first (domain/formulir); these classes
 * treat id == 0 as "new" and anything else as "update".
 */

class MemoriRepository(private val dao: MemoriDao) {
    val semua: Flow<List<ItemMemori>> = dao.amati().map { daftar -> daftar.map { it.keDomain() } }

    /** Blank text is ignored (the form already blocks it). The timestamp is set on first save only. */
    suspend fun simpan(item: ItemMemori) {
        val isi = ValidasiMemori.bersihkan(item.isi)
        if (isi.isEmpty()) return
        val baru = item.copy(isi = isi, dibuat = if (item.dibuat == 0L) System.currentTimeMillis() else item.dibuat)
        if (baru.id == 0L) dao.sisip(baru.keEntitas()) else dao.ubah(baru.keEntitas())
    }

    suspend fun hapus(id: Long) = dao.hapus(id)

    suspend fun hapusSemua() = dao.hapusSemua()
}

class JadwalRepository(private val kegiatanDao: KegiatanDao, private val sekolahDao: SekolahDao) {
    val kegiatan: Flow<List<Kegiatan>> = kegiatanDao.amati().map { d -> d.map { it.keDomain() } }
    val jamSekolah: Flow<List<JamSekolah>> = sekolahDao.amatiJam().map { d -> d.map { it.keDomain() } }
    val pelajaran: Flow<List<Pelajaran>> = sekolahDao.amatiPelajaran().map { d -> d.map { it.keDomain() } }
    val piket: Flow<List<Piket>> = sekolahDao.amatiPiket().map { d -> d.map { it.keDomain() } }

    suspend fun simpanKegiatan(k: Kegiatan) {
        if (k.id == 0L) kegiatanDao.sisip(k.keEntitas()) else kegiatanDao.ubah(k.keEntitas())
    }

    suspend fun hapusKegiatan(id: Long) = kegiatanDao.hapus(id)

    /** One row per weekday: saving replaces that day's hours. */
    suspend fun simpanJamSekolah(jam: JamSekolah) = sekolahDao.simpanJam(jam.keEntitas())

    suspend fun hapusJamSekolah(hari: DayOfWeek) = sekolahDao.hapusJam(hari.value)

    suspend fun simpanPelajaran(p: Pelajaran) {
        if (p.id == 0L) sekolahDao.sisipPelajaran(p.keEntitas()) else sekolahDao.ubahPelajaran(p.keEntitas())
    }

    suspend fun hapusPelajaran(id: Long) = sekolahDao.hapusPelajaran(id)

    suspend fun tambahPiket(p: Piket) {
        sekolahDao.sisipPiket(p.keEntitas())
    }

    suspend fun hapusPiket(id: Long) = sekolahDao.hapusPiket(id)
}

class AktivitasRepository(private val tugasDao: TugasDao, private val tujuanDao: TujuanDao) {
    val tugas: Flow<List<Tugas>> = tugasDao.amati().map { d -> d.map { it.keDomain() } }

    val tujuan: Flow<List<Tujuan>> = combine(tujuanDao.amatiTujuan(), tujuanDao.amatiTonggak()) { daftar, tonggak ->
        daftar.map { it.keDomain(tonggak) }
    }

    suspend fun simpanTugas(t: Tugas) {
        if (t.id == 0L) tugasDao.sisip(t.keEntitas()) else tugasDao.ubah(t.keEntitas())
    }

    suspend fun setTugasSelesai(t: Tugas, selesai: Boolean) {
        tugasDao.ubah(t.denganSelesai(selesai, System.currentTimeMillis()).keEntitas())
    }

    suspend fun hapusTugas(id: Long) = tugasDao.hapus(id)

    suspend fun simpanTujuan(t: Tujuan) {
        if (t.id == 0L) tujuanDao.sisipTujuan(t.keEntitas()) else tujuanDao.ubahTujuan(t.keEntitas())
    }

    suspend fun setTujuanSelesai(t: Tujuan, selesai: Boolean) {
        tujuanDao.ubahTujuan(t.denganSelesai(selesai, System.currentTimeMillis()).keEntitas())
    }

    suspend fun hapusTujuan(id: Long) = tujuanDao.hapusTujuan(id)

    suspend fun tambahTonggak(t: Tonggak) {
        tujuanDao.sisipTonggak(t.keEntitas())
    }

    suspend fun setTonggakSelesai(t: Tonggak, selesai: Boolean) {
        tujuanDao.ubahTonggak(t.copy(selesai = selesai).keEntitas())
    }

    suspend fun hapusTonggak(id: Long) = tujuanDao.hapusTonggak(id)
}
