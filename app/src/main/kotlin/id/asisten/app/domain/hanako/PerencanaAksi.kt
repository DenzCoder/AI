package id.asisten.app.domain.hanako

import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.ValidasiJadwal

/** The concrete database or settings step an approved action becomes. */
sealed interface LangkahAksi {
    data class SimpanTugas(val tugas: Tugas) : LangkahAksi
    data class TandaiTugas(val tugas: Tugas, val selesai: Boolean) : LangkahAksi
    data class HapusTugas(val id: Long) : LangkahAksi
    data class SimpanKegiatan(val kegiatan: Kegiatan) : LangkahAksi
    data class HapusKegiatan(val id: Long) : LangkahAksi
    data class SimpanPelajaran(val pelajaran: Pelajaran) : LangkahAksi
    data class HapusPelajaran(val id: Long) : LangkahAksi

    /** Reminder settings steps are applied to the settings as they are at that moment, not to a copy. */
    data class UbahPengingat(val perubahan: PerubahanPengingat) : LangkahAksi
    data class MulaiMode(val aksi: AksiHanako.MulaiMode) : LangkahAksi
    data object AkhiriMode : LangkahAksi
}

sealed interface HasilRencana {
    data class Siap(val langkah: LangkahAksi) : HasilRencana

    /** The data changed since the proposal was made (for example the item was deleted by the user). */
    data class Gagal(val alasan: String) : HasilRencana
}

/**
 * Called when the user taps Setujui. The data may have changed since Hanako proposed the action (the user can edit
 * or delete things while the card is on screen), so everything is looked up and checked again. Edits are applied
 * to the item as it is NOW, so changes the user made in the meantime are kept.
 */
object PerencanaAksi {

    fun rencanakan(aksi: AksiHanako, d: DataSaatIni): HasilRencana = when (aksi) {
        is AksiHanako.TambahTugas -> HasilRencana.Siap(LangkahAksi.SimpanTugas(aksi.tugas.copy(id = 0)))
        is AksiHanako.UbahTugas -> d.tugas.firstOrNull { it.id == aksi.id }
            ?.let { HasilRencana.Siap(LangkahAksi.SimpanTugas(it.terapkan(aksi.perubahan))) }
            ?: hilang("Tugas")
        is AksiHanako.TandaiTugas -> d.tugas.firstOrNull { it.id == aksi.id }
            ?.let { HasilRencana.Siap(LangkahAksi.TandaiTugas(it, aksi.selesai)) }
            ?: hilang("Tugas")
        is AksiHanako.HapusTugas -> if (d.tugas.any { it.id == aksi.id }) HasilRencana.Siap(LangkahAksi.HapusTugas(aksi.id)) else hilang("Tugas")

        is AksiHanako.TambahKegiatan -> when {
            aksi.kegiatan.tanggal != null && aksi.kegiatan.tanggal.isBefore(d.hariIni) ->
                HasilRencana.Gagal("Tanggal kegiatan itu sudah lewat.")
            else -> HasilRencana.Siap(LangkahAksi.SimpanKegiatan(aksi.kegiatan.copy(id = 0)))
        }
        is AksiHanako.UbahKegiatan -> {
            val lama = d.kegiatan.firstOrNull { it.id == aksi.id }
            if (lama == null) hilang("Kegiatan") else {
                val baru = lama.terapkan(aksi.perubahan)
                if (!ValidasiJadwal.waktuValid(baru.mulai, baru.selesai)) {
                    HasilRencana.Gagal("Jam selesai harus setelah jam mulai.")
                } else {
                    HasilRencana.Siap(LangkahAksi.SimpanKegiatan(baru))
                }
            }
        }
        is AksiHanako.HapusKegiatan -> if (d.kegiatan.any { it.id == aksi.id }) HasilRencana.Siap(LangkahAksi.HapusKegiatan(aksi.id)) else hilang("Kegiatan")

        is AksiHanako.TambahPelajaran -> HasilRencana.Siap(LangkahAksi.SimpanPelajaran(aksi.pelajaran.copy(id = 0, istirahat = false)))
        is AksiHanako.UbahPelajaran -> {
            val lama = d.pelajaran.firstOrNull { it.id == aksi.id && !it.istirahat }
            if (lama == null) hilang("Pelajaran") else {
                val baru = lama.terapkan(aksi.perubahan)
                if (!ValidasiJadwal.waktuValid(baru.mulai, baru.selesai)) {
                    HasilRencana.Gagal("Jam selesai harus setelah jam mulai.")
                } else {
                    HasilRencana.Siap(LangkahAksi.SimpanPelajaran(baru))
                }
            }
        }
        is AksiHanako.HapusPelajaran ->
            if (d.pelajaran.any { it.id == aksi.id && !it.istirahat }) HasilRencana.Siap(LangkahAksi.HapusPelajaran(aksi.id)) else hilang("Pelajaran")

        is AksiHanako.AturPengingat -> HasilRencana.Siap(LangkahAksi.UbahPengingat(aksi.perubahan))
        is AksiHanako.MulaiMode -> HasilRencana.Siap(LangkahAksi.MulaiMode(aksi))
        AksiHanako.AkhiriMode -> HasilRencana.Siap(LangkahAksi.AkhiriMode)
    }

    private fun hilang(apa: String) = HasilRencana.Gagal("$apa itu sudah tidak ada, mungkin sudah kamu hapus.")
}
