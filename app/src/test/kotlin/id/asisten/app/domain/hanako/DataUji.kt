package id.asisten.app.domain.hanako

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.pengingat.ModePengingat
import id.asisten.app.domain.pengingat.PengaturanPengingat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

/** Shared fixture for the Hanako action tests. 16 November 2026 is a Monday. */
object DataUji {
    val hariIni: LocalDate = LocalDate.of(2026, 11, 16)
    const val SEKARANG = 1_800_000_000_000L

    val tugasPr = Tugas(
        id = 1, judul = "PR Matematika", jenis = JenisTugas.PR, mapel = "Matematika",
        tenggat = hariIni.plusDays(2), prioritas = Prioritas.TINGGI, catatan = "bab 3",
    )
    val tugasSelesai = Tugas(id = 2, judul = "Laporan IPA", selesai = true, selesaiPada = 5L)
    val lesInggris = Kegiatan(
        id = 10, judul = "Les Inggris", hariMingguan = DayOfWeek.WEDNESDAY,
        mulai = LocalTime.of(16, 0), selesai = LocalTime.of(17, 30),
    )
    val rapatOsis = Kegiatan(
        id = 11, judul = "Rapat OSIS", tanggal = hariIni.plusDays(7),
        mulai = LocalTime.of(15, 0), selesai = LocalTime.of(16, 0), prioritas = Prioritas.TINGGI,
    )
    val kegiatanLampau = Kegiatan(
        id = 12, judul = "Kerja kelompok", tanggal = hariIni.minusDays(3),
        mulai = LocalTime.of(10, 0), selesai = LocalTime.of(11, 0),
    )
    val matematika = Pelajaran(
        id = 20, hari = DayOfWeek.MONDAY, mulai = LocalTime.of(7, 0), selesai = LocalTime.of(8, 30),
        nama = "Matematika", guru = "Bu Ani", ruang = "3",
    )
    val istirahat = Pelajaran(
        id = 21, hari = DayOfWeek.MONDAY, mulai = LocalTime.of(9, 0), selesai = LocalTime.of(9, 30),
        nama = "Istirahat", istirahat = true,
    )

    fun data(
        tugas: List<Tugas> = listOf(tugasPr, tugasSelesai),
        kegiatan: List<Kegiatan> = listOf(lesInggris, rapatOsis, kegiatanLampau),
        pelajaran: List<Pelajaran> = listOf(matematika, istirahat),
        pengingat: PengaturanPengingat = PengaturanPengingat(),
    ) = DataSaatIni(hariIni, tugas, kegiatan, pelajaran, pengingat, SEKARANG)

    fun dataModeAktif() = data(
        pengingat = PengaturanPengingat().mulaiMode(ModePengingat.FOKUS, 30, SEKARANG),
    )

    fun panggil(nama: String, vararg args: Pair<String, Any?>) = PanggilanFungsi(nama, mapOf(*args))
}
