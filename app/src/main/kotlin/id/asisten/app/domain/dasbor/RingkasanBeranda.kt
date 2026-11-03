package id.asisten.app.domain.dasbor

import id.asisten.app.domain.aktivitas.Aktivitas
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.aktivitas.Tujuan
import id.asisten.app.domain.jadwal.ItemHarian
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.PenyusunHarian
import id.asisten.app.domain.jadwal.Piket
import java.time.LocalDate

/** Everything the Beranda cards show, derived from recorded data only. */
data class RingkasanBeranda(
    val hariIni: LocalDate,
    val jadwalHariIni: List<ItemHarian> = emptyList(),
    val jamSekolahHariIni: JamSekolah? = null,
    val piketHariIni: List<Piket> = emptyList(),
    val tugasTerdekat: List<Tugas> = emptyList(),
    val tujuanAktif: List<Tujuan> = emptyList(),
)

object PenyusunBeranda {
    const val BATAS_TUGAS = 3
    const val BATAS_TUJUAN = 3

    fun susun(
        hariIni: LocalDate,
        kegiatan: List<Kegiatan>,
        pelajaran: List<Pelajaran>,
        jamSekolah: List<JamSekolah>,
        piket: List<Piket>,
        tugas: List<Tugas>,
        tujuan: List<Tujuan>,
    ): RingkasanBeranda = RingkasanBeranda(
        hariIni = hariIni,
        jadwalHariIni = PenyusunHarian.susun(hariIni, kegiatan, pelajaran),
        jamSekolahHariIni = jamSekolah.firstOrNull { it.hari == hariIni.dayOfWeek },
        piketHariIni = piket.filter { it.hari == hariIni.dayOfWeek },
        tugasTerdekat = Aktivitas.urutkanTugas(tugas.filter { !it.selesai && it.jenis != JenisTugas.UJIAN })
            .take(BATAS_TUGAS),
        tujuanAktif = tujuan.filter { !it.selesai }.take(BATAS_TUJUAN),
    )
}
