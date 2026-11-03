package id.asisten.app.domain

import id.asisten.app.domain.aktivitas.FormatAktivitas
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.aktivitas.Tujuan
import id.asisten.app.domain.aktivitas.denganSelesai
import id.asisten.app.domain.jadwal.ModeKalender
import id.asisten.app.domain.jadwal.NavigasiKalender
import id.asisten.app.domain.pengaturan.Aksen
import id.asisten.app.domain.pengaturan.ModeTema
import id.asisten.app.domain.waktu.Epoch
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LainnyaTest {

    // ---- keEnum ----

    @Test
    fun keEnumMembacaNamaYangValid() {
        assertEquals(ModeTema.GELAP, "GELAP".keEnum(ModeTema.SISTEM))
        assertEquals(Aksen.BIRU, "BIRU".keEnum(Aksen.JINGGA))
    }

    @Test
    fun keEnumJatuhKeBawaanBilaNullAtauTidakDikenal() {
        assertEquals(ModeTema.SISTEM, null.keEnum(ModeTema.SISTEM))
        assertEquals(ModeTema.SISTEM, "".keEnum(ModeTema.SISTEM))
        assertEquals(ModeTema.SISTEM, "UNGU_MUDA".keEnum(ModeTema.SISTEM))
        assertEquals(ModeTema.SISTEM, "gelap".keEnum(ModeTema.SISTEM))
    }

    // ---- NavigasiKalender ----

    private val rabu = LocalDate.of(2026, 11, 4)

    @Test
    fun geserSesuaiMode() {
        assertEquals(rabu.plusDays(1), NavigasiKalender.geser(rabu, ModeKalender.HARI, +1))
        assertEquals(rabu.minusWeeks(1), NavigasiKalender.geser(rabu, ModeKalender.MINGGU, -1))
        assertEquals(LocalDate.of(2026, 12, 4), NavigasiKalender.geser(rabu, ModeKalender.BULAN, +1))
    }

    @Test
    fun geserBulanMenjepitHariTerakhir() {
        assertEquals(
            LocalDate.of(2026, 2, 28),
            NavigasiKalender.geser(LocalDate.of(2026, 1, 31), ModeKalender.BULAN, +1),
        )
    }

    @Test
    fun labelSesuaiMode() {
        assertEquals("Rabu, 4 November 2026", NavigasiKalender.label(rabu, ModeKalender.HARI))
        assertEquals("2 November – 8 November 2026", NavigasiKalender.label(rabu, ModeKalender.MINGGU))
        assertEquals("November 2026", NavigasiKalender.label(rabu, ModeKalender.BULAN))
    }

    @Test
    fun labelMingguLintasBulan() {
        assertEquals(
            "26 Oktober – 1 November 2026",
            NavigasiKalender.label(LocalDate.of(2026, 10, 29), ModeKalender.MINGGU),
        )
    }

    // ---- Penyelesaian ----

    @Test
    fun menyelesaikanTugasMencatatWaktuDanMembukaLagiMenghapusnya() {
        val t = Tugas(judul = "A")
        val selesai = t.denganSelesai(true, 500L)
        assertEquals(true, selesai.selesai)
        assertEquals(500L, selesai.selesaiPada)
        val buka = selesai.denganSelesai(false, 900L)
        assertEquals(false, buka.selesai)
        assertNull(buka.selesaiPada)
    }

    @Test
    fun menyelesaikanTujuanMencatatWaktu() {
        val s = Tujuan(judul = "A").denganSelesai(true, 77L)
        assertEquals(77L, s.selesaiPada)
        assertNull(s.denganSelesai(false, 88L).selesaiPada)
    }

    // ---- FormatAktivitas ----

    private val hariIni = LocalDate.of(2026, 11, 2)

    @Test
    fun teksTenggat() {
        assertEquals("Tanpa tenggat", FormatAktivitas.tenggat(Tugas(judul = "A"), hariIni))
        assertEquals("Hari ini", FormatAktivitas.tenggat(Tugas(judul = "A", tenggat = hariIni), hariIni))
        assertEquals("Besok (03-11-2026)", FormatAktivitas.tenggat(Tugas(judul = "A", tenggat = hariIni.plusDays(1)), hariIni))
        assertEquals("09-11-2026", FormatAktivitas.tenggat(Tugas(judul = "A", tenggat = hariIni.plusDays(7)), hariIni))
        assertEquals("Terlambat (01-11-2026)", FormatAktivitas.tenggat(Tugas(judul = "A", tenggat = hariIni.minusDays(1)), hariIni))
    }

    @Test
    fun persenDibulatkanDanDijepit() {
        assertEquals(0, FormatAktivitas.persen(0f))
        assertEquals(50, FormatAktivitas.persen(0.5f))
        assertEquals(33, FormatAktivitas.persen(1f / 3f))
        assertEquals(67, FormatAktivitas.persen(2f / 3f))
        assertEquals(100, FormatAktivitas.persen(1f))
        assertEquals(100, FormatAktivitas.persen(3f))
        assertEquals(0, FormatAktivitas.persen(-1f))
    }

    // ---- Epoch ----

    @Test
    fun epochDiubahKeTanggalMenurutZona() {
        // 2026-11-02T20:00:00Z is already 3 Nov in UTC+7.
        val millis = LocalDate.of(2026, 11, 2).atTime(20, 0).toInstant(ZoneOffset.UTC).toEpochMilli()
        assertEquals(LocalDate.of(2026, 11, 2), Epoch.tanggal(millis, ZoneOffset.UTC))
        assertEquals(LocalDate.of(2026, 11, 3), Epoch.tanggal(millis, ZoneOffset.ofHours(7)))
    }

    // ---- Label ----

    @Test
    fun labelIndonesia() {
        assertEquals("Tinggi", Prioritas.TINGGI.label())
        assertEquals("Jangka panjang", id.asisten.app.domain.aktivitas.JangkaTujuan.PANJANG.label())
        assertEquals("Bulan", ModeKalender.BULAN.label())
    }
}
