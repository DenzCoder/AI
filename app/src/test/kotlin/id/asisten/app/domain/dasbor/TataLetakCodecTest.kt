package id.asisten.app.domain.dasbor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TataLetakCodecTest {

    @Test
    fun encodeDecodeBolakBalikSamaDenganAsli() {
        val asli = TataLetakWidget.STANDAR
            .pindah(WidgetBeranda.TUGAS, +1)
            .pindah(WidgetBeranda.TUJUAN, -2)
            .sembunyikan(WidgetBeranda.KEUANGAN)
            .sembunyikan(WidgetBeranda.SHOLAT)
        assertEquals(asli, TataLetakCodec.decode(TataLetakCodec.encode(asli)))
    }

    @Test
    fun decodeNullAtauKosongMemberiTataLetakStandar() {
        assertEquals(TataLetakWidget.STANDAR, TataLetakCodec.decode(null))
        assertEquals(TataLetakWidget.STANDAR, TataLetakCodec.decode(""))
        assertEquals(TataLetakWidget.STANDAR, TataLetakCodec.decode("   "))
    }

    @Test
    fun decodeMembuangNamaTidakDikenalDanDuplikat() {
        val hasil = TataLetakCodec.decode("TUGAS,HANTU,TUGAS,SEKOLAH|HANTU,KEUANGAN")
        assertEquals(WidgetBeranda.TUGAS, hasil.urutan[0])
        assertEquals(WidgetBeranda.SEKOLAH, hasil.urutan[1])
        assertEquals(setOf(WidgetBeranda.KEUANGAN), hasil.tersembunyi)
        assertEquals(WidgetBeranda.entries.size, hasil.urutan.size)
        assertEquals(hasil.urutan.size, hasil.urutan.toSet().size)
    }

    @Test
    fun widgetBaruDariVersiLaterDitambahkanDiAkhir() {
        val hasil = TataLetakCodec.decode("SEKOLAH,TUGAS|")
        assertEquals(listOf(WidgetBeranda.SEKOLAH, WidgetBeranda.TUGAS), hasil.urutan.take(2))
        assertEquals(WidgetBeranda.entries.size, hasil.urutan.size)
        assertTrue(hasil.tersembunyi.isEmpty())
    }

    @Test
    fun riwayatBolakBalikMempertahankanUrutanTerbaruDiAkhir() {
        var r = RiwayatTataLetak()
        r = r.terapkan(r.sekarang.pindah(WidgetBeranda.TUGAS, +1))
        r = r.terapkan(r.sekarang.sembunyikan(WidgetBeranda.KEUANGAN))
        r = r.terapkan(r.sekarang.pindah(WidgetBeranda.TUJUAN, -1))
        val dipulihkan = TataLetakCodec.decodeRiwayat(TataLetakCodec.encodeRiwayat(r))
        assertEquals(r, dipulihkan)
        // Undo must walk back through the same states after a restore.
        assertEquals(r.urungkan(), dipulihkan.urungkan())
        assertEquals(r.urungkan().urungkan(), dipulihkan.urungkan().urungkan())
    }

    @Test
    fun decodeRiwayatKosongMemberiRiwayatBaru() {
        assertEquals(RiwayatTataLetak(), TataLetakCodec.decodeRiwayat(null))
        assertEquals(RiwayatTataLetak(), TataLetakCodec.decodeRiwayat(""))
        assertEquals(RiwayatTataLetak(), TataLetakCodec.decodeRiwayat(";;"))
    }

    @Test
    fun decodeRiwayatMembatasiJumlahRiwayat() {
        val banyak = (0 until RiwayatTataLetak.BATAS_RIWAYAT + 15).joinToString(";") {
            TataLetakCodec.encode(TataLetakWidget.STANDAR)
        }
        val r = TataLetakCodec.decodeRiwayat(banyak)
        assertEquals(RiwayatTataLetak.BATAS_RIWAYAT, r.riwayat.size)
    }

    @Test
    fun riwayatTanpaPerubahanTidakDiubahOlehEncode() {
        val r = RiwayatTataLetak()
        assertEquals(r, TataLetakCodec.decodeRiwayat(TataLetakCodec.encodeRiwayat(r)))
    }
}
