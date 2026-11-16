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

/** A function call the model asked for: a name and its arguments, exactly as received. */
data class PanggilanFungsi(val nama: String, val args: Map<String, Any?> = emptyMap())

/** The app data an action is checked against and described with. */
data class DataSaatIni(
    val hariIni: LocalDate,
    val tugas: List<Tugas> = emptyList(),
    val kegiatan: List<Kegiatan> = emptyList(),
    val pelajaran: List<Pelajaran> = emptyList(),
    val pengingat: PengaturanPengingat = PengaturanPengingat(),
    /** Epoch millis now; decides whether a Fokus/Istirahat mode is still running. */
    val sekarangMillis: Long = 0L,
)

// --- partial changes: only the fields that are not null are changed ---

data class PerubahanTugas(
    val judul: String? = null,
    val jenis: JenisTugas? = null,
    val mapel: String? = null,
    val tenggat: LocalDate? = null,
    val hapusTenggat: Boolean = false,
    val prioritas: Prioritas? = null,
    val catatan: String? = null,
) {
    val kosong: Boolean
        get() = judul == null && jenis == null && mapel == null && tenggat == null && !hapusTenggat &&
            prioritas == null && catatan == null
}

fun Tugas.terapkan(p: PerubahanTugas): Tugas = copy(
    judul = p.judul ?: judul,
    jenis = p.jenis ?: jenis,
    mapel = p.mapel ?: mapel,
    tenggat = if (p.hapusTenggat) null else (p.tenggat ?: tenggat),
    prioritas = p.prioritas ?: prioritas,
    catatan = p.catatan ?: catatan,
)

/** Setting [tanggal] makes the activity one-time; setting [hariMingguan] makes it weekly. Never both. */
data class PerubahanKegiatan(
    val judul: String? = null,
    val tanggal: LocalDate? = null,
    val hariMingguan: DayOfWeek? = null,
    val mulai: LocalTime? = null,
    val selesai: LocalTime? = null,
    val prioritas: Prioritas? = null,
    val catatan: String? = null,
) {
    val kosong: Boolean
        get() = judul == null && tanggal == null && hariMingguan == null && mulai == null && selesai == null &&
            prioritas == null && catatan == null
}

fun Kegiatan.terapkan(p: PerubahanKegiatan): Kegiatan {
    val (tgl, hari) = when {
        p.tanggal != null -> p.tanggal to null
        p.hariMingguan != null -> null to p.hariMingguan
        else -> tanggal to hariMingguan
    }
    return copy(
        judul = p.judul ?: judul,
        tanggal = tgl,
        hariMingguan = hari,
        mulai = p.mulai ?: mulai,
        selesai = p.selesai ?: selesai,
        prioritas = p.prioritas ?: prioritas,
        catatan = p.catatan ?: catatan,
    )
}

data class PerubahanPelajaran(
    val hari: DayOfWeek? = null,
    val mulai: LocalTime? = null,
    val selesai: LocalTime? = null,
    val nama: String? = null,
    val guru: String? = null,
    val ruang: String? = null,
) {
    val kosong: Boolean
        get() = hari == null && mulai == null && selesai == null && nama == null && guru == null && ruang == null
}

fun Pelajaran.terapkan(p: PerubahanPelajaran): Pelajaran = copy(
    hari = p.hari ?: hari,
    mulai = p.mulai ?: mulai,
    selesai = p.selesai ?: selesai,
    nama = p.nama ?: nama,
    guru = p.guru ?: guru,
    ruang = p.ruang ?: ruang,
)

/** Times of day are minutes since midnight, like in [PengaturanPengingat]. */
data class PerubahanPengingat(
    val aktif: Boolean? = null,
    val menitSebelumKegiatan: Int? = null,
    val menitSebelumSekolah: Int? = null,
    val tundaMenit: Int? = null,
    val jamTenangAktif: Boolean? = null,
    val jamTenangMulai: Int? = null,
    val jamTenangSelesai: Int? = null,
) {
    val kosong: Boolean
        get() = aktif == null && menitSebelumKegiatan == null && menitSebelumSekolah == null && tundaMenit == null &&
            jamTenangAktif == null && jamTenangMulai == null && jamTenangSelesai == null
}

fun PengaturanPengingat.terapkan(p: PerubahanPengingat): PengaturanPengingat = copy(
    aktif = p.aktif ?: aktif,
    menitSebelumKegiatan = p.menitSebelumKegiatan ?: menitSebelumKegiatan,
    menitSebelumSekolah = p.menitSebelumSekolah ?: menitSebelumSekolah,
    tundaMenit = p.tundaMenit ?: tundaMenit,
    jamTenangAktif = p.jamTenangAktif ?: jamTenangAktif,
    jamTenangMulai = p.jamTenangMulai ?: jamTenangMulai,
    jamTenangSelesai = p.jamTenangSelesai ?: jamTenangSelesai,
).normalisasi()

/**
 * A change to the app that Hanako proposes. Nothing here is executed by itself: the user must approve it first.
 * Delete actions carry the title as it was when proposed, so the confirmation can name what will disappear.
 */
sealed interface AksiHanako {
    /** True when the change cannot be undone from the app. */
    val destruktif: Boolean get() = false

    data class TambahTugas(val tugas: Tugas) : AksiHanako
    data class UbahTugas(val id: Long, val perubahan: PerubahanTugas) : AksiHanako
    data class TandaiTugas(val id: Long, val selesai: Boolean) : AksiHanako
    data class HapusTugas(val id: Long, val judul: String) : AksiHanako {
        override val destruktif get() = true
    }

    data class TambahKegiatan(val kegiatan: Kegiatan) : AksiHanako
    data class UbahKegiatan(val id: Long, val perubahan: PerubahanKegiatan) : AksiHanako
    data class HapusKegiatan(val id: Long, val judul: String) : AksiHanako {
        override val destruktif get() = true
    }

    data class TambahPelajaran(val pelajaran: Pelajaran) : AksiHanako
    data class UbahPelajaran(val id: Long, val perubahan: PerubahanPelajaran) : AksiHanako
    data class HapusPelajaran(val id: Long, val judul: String) : AksiHanako {
        override val destruktif get() = true
    }

    data class AturPengingat(val perubahan: PerubahanPengingat) : AksiHanako
    data class MulaiMode(val mode: ModePengingat, val menit: Int) : AksiHanako
    data object AkhiriMode : AksiHanako
}

/** A proposal ready to show: the action, its one-line description and warnings (clashes, "cannot be undone"). */
data class UsulanTampil(
    val id: Long,
    val aksi: AksiHanako,
    val ringkas: String,
    val peringatan: List<String>,
)

/** A proposal as it is stored: the raw function call, re-checked against the current data whenever it is shown. */
data class UsulanMentah(val id: Long, val waktu: Long, val nama: String, val argsJson: String)
