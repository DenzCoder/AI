package id.asisten.app.data.hanako

import id.asisten.app.data.pengaturan.PengaturanRepository
import id.asisten.app.data.repo.AktivitasRepository
import id.asisten.app.data.repo.JadwalRepository
import id.asisten.app.domain.hanako.AksiHanako
import id.asisten.app.domain.hanako.DataSaatIni
import id.asisten.app.domain.hanako.HasilRencana
import id.asisten.app.domain.hanako.LangkahAksi
import id.asisten.app.domain.hanako.PerencanaAksi
import id.asisten.app.domain.hanako.terapkan
import java.time.LocalDate
import kotlinx.coroutines.flow.first

sealed interface HasilPelaksanaan {
    data object Berhasil : HasilPelaksanaan

    /** Nothing was changed. [alasan] is an Indonesian sentence for the user. */
    data class Gagal(val alasan: String) : HasilPelaksanaan
}

/**
 * Carries out an action the user approved, through the same repositories the screens use. Because of that the reminder
 * system (which watches the data and the reminder settings) re-arms the alarms by itself after every change.
 * Which step an action becomes is decided by the pure [PerencanaAksi]; this class only reads and writes.
 */
class PelaksanaAksi(
    private val jadwal: JadwalRepository,
    private val aktivitas: AktivitasRepository,
    private val pengaturan: PengaturanRepository,
) {
    /** A snapshot of everything an action is checked against. */
    suspend fun dataSaatIni(): DataSaatIni = DataSaatIni(
        hariIni = LocalDate.now(),
        tugas = aktivitas.tugas.first(),
        kegiatan = jadwal.kegiatan.first(),
        pelajaran = jadwal.pelajaran.first(),
        pengingat = pengaturan.pengingat.first(),
        sekarangMillis = System.currentTimeMillis(),
    )

    /** [data] must be a snapshot taken just before (see [dataSaatIni]). */
    suspend fun jalankan(aksi: AksiHanako, data: DataSaatIni): HasilPelaksanaan {
        val langkah = when (val rencana = PerencanaAksi.rencanakan(aksi, data)) {
            is HasilRencana.Gagal -> return HasilPelaksanaan.Gagal(rencana.alasan)
            is HasilRencana.Siap -> rencana.langkah
        }
        when (langkah) {
            is LangkahAksi.SimpanTugas -> aktivitas.simpanTugas(langkah.tugas)
            is LangkahAksi.TandaiTugas -> aktivitas.setTugasSelesai(langkah.tugas, langkah.selesai)
            is LangkahAksi.HapusTugas -> aktivitas.hapusTugas(langkah.id)
            is LangkahAksi.SimpanKegiatan -> jadwal.simpanKegiatan(langkah.kegiatan)
            is LangkahAksi.HapusKegiatan -> jadwal.hapusKegiatan(langkah.id)
            is LangkahAksi.SimpanPelajaran -> jadwal.simpanPelajaran(langkah.pelajaran)
            is LangkahAksi.HapusPelajaran -> jadwal.hapusPelajaran(langkah.id)
            is LangkahAksi.UbahPengingat -> pengaturan.ubahPengingat { it.terapkan(langkah.perubahan) }
            is LangkahAksi.MulaiMode ->
                pengaturan.ubahPengingat { it.mulaiMode(langkah.aksi.mode, langkah.aksi.menit, System.currentTimeMillis()) }
            LangkahAksi.AkhiriMode -> pengaturan.ubahPengingat { it.akhiriMode() }
        }
        return HasilPelaksanaan.Berhasil
    }
}
