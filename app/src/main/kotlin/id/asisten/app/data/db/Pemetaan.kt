package id.asisten.app.data.db

import id.asisten.app.domain.Prioritas
import id.asisten.app.domain.aktivitas.JangkaTujuan
import id.asisten.app.domain.aktivitas.JenisTugas
import id.asisten.app.domain.aktivitas.Tonggak
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.aktivitas.Tujuan
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.Piket
import id.asisten.app.domain.keEnum
import id.asisten.app.domain.memori.ItemMemori
import id.asisten.app.domain.memori.KategoriMemori
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime

// ---- primitive conversions (defensive: bad stored values are clamped, never thrown) ----

internal fun LocalTime.keMenit(): Int = hour * 60 + minute

internal fun Int.keJam(): LocalTime = LocalTime.ofSecondOfDay(coerceIn(0, 24 * 60 - 1) * 60L)

internal fun DayOfWeek.keAngka(): Int = value

internal fun Int.keHari(): DayOfWeek = DayOfWeek.of(coerceIn(1, 7))

// ---- memori ----

fun MemoriEntitas.keDomain() = ItemMemori(
    id = id,
    kategori = kategori.keEnum(KategoriMemori.KONTEKS),
    isi = isi,
    sensitif = sensitif,
    dibuat = dibuat,
)

fun ItemMemori.keEntitas() = MemoriEntitas(
    id = id,
    kategori = kategori.name,
    isi = isi,
    sensitif = sensitif,
    dibuat = dibuat,
)

// ---- kegiatan ----

fun KegiatanEntitas.keDomain() = Kegiatan(
    id = id,
    judul = judul,
    tanggal = tanggalHari?.let { LocalDate.ofEpochDay(it) },
    hariMingguan = hariMingguan?.keHari(),
    mulai = mulaiMenit.keJam(),
    selesai = selesaiMenit.keJam(),
    prioritas = prioritas.keEnum(Prioritas.SEDANG),
    catatan = catatan,
)

fun Kegiatan.keEntitas() = KegiatanEntitas(
    id = id,
    judul = judul,
    tanggalHari = tanggal?.toEpochDay(),
    hariMingguan = hariMingguan?.keAngka(),
    mulaiMenit = mulai.keMenit(),
    selesaiMenit = selesai.keMenit(),
    prioritas = prioritas.name,
    catatan = catatan,
)

// ---- sekolah ----

fun JamSekolahEntitas.keDomain() = JamSekolah(
    hari = hari.keHari(),
    masuk = masukMenit.keJam(),
    pulang = pulangMenit.keJam(),
)

fun JamSekolah.keEntitas() = JamSekolahEntitas(
    hari = hari.keAngka(),
    masukMenit = masuk.keMenit(),
    pulangMenit = pulang.keMenit(),
)

fun PelajaranEntitas.keDomain() = Pelajaran(
    id = id,
    hari = hari.keHari(),
    mulai = mulaiMenit.keJam(),
    selesai = selesaiMenit.keJam(),
    nama = nama,
    guru = guru,
    ruang = ruang,
    istirahat = istirahat,
)

fun Pelajaran.keEntitas() = PelajaranEntitas(
    id = id,
    hari = hari.keAngka(),
    mulaiMenit = mulai.keMenit(),
    selesaiMenit = selesai.keMenit(),
    nama = nama,
    guru = guru,
    ruang = ruang,
    istirahat = istirahat,
)

fun PiketEntitas.keDomain() = Piket(id = id, hari = hari.keHari(), keterangan = keterangan)

fun Piket.keEntitas() = PiketEntitas(id = id, hari = hari.keAngka(), keterangan = keterangan)

// ---- tugas dan tujuan ----

fun TugasEntitas.keDomain() = Tugas(
    id = id,
    judul = judul,
    jenis = jenis.keEnum(JenisTugas.TUGAS),
    mapel = mapel,
    tenggat = tenggatHari?.let { LocalDate.ofEpochDay(it) },
    prioritas = prioritas.keEnum(Prioritas.SEDANG),
    selesai = selesai,
    selesaiPada = selesaiPada,
    catatan = catatan,
)

fun Tugas.keEntitas() = TugasEntitas(
    id = id,
    judul = judul,
    jenis = jenis.name,
    mapel = mapel,
    tenggatHari = tenggat?.toEpochDay(),
    prioritas = prioritas.name,
    selesai = selesai,
    selesaiPada = selesaiPada,
    catatan = catatan,
)

fun TonggakEntitas.keDomain() = Tonggak(id = id, tujuanId = tujuanId, judul = judul, selesai = selesai)

fun Tonggak.keEntitas() = TonggakEntitas(id = id, tujuanId = tujuanId, judul = judul, selesai = selesai)

/** Builds a goal with only the milestones that belong to it, in the order given. */
fun TujuanEntitas.keDomain(semuaTonggak: List<TonggakEntitas>) = Tujuan(
    id = id,
    judul = judul,
    jangka = jangka.keEnum(JangkaTujuan.PENDEK),
    tenggat = tenggatHari?.let { LocalDate.ofEpochDay(it) },
    selesai = selesai,
    selesaiPada = selesaiPada,
    tonggak = semuaTonggak.filter { it.tujuanId == id }.map { it.keDomain() },
)

/** Milestones are stored in their own table, so only the goal row is produced here. */
fun Tujuan.keEntitas() = TujuanEntitas(
    id = id,
    judul = judul,
    jangka = jangka.name,
    tenggatHari = tenggat?.toEpochDay(),
    selesai = selesai,
    selesaiPada = selesaiPada,
)
