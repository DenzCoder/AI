package id.asisten.app.domain.dasbor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class TataLetakWidgetTest {

    private val standar = TataLetakWidget.STANDAR

    @Test
    fun standarMemuatSemuaWidgetDanTidakAdaYangTersembunyi() {
        assertEquals(WidgetBeranda.entries.toList(), standar.urutan)
        assertTrue(standar.tersembunyi.isEmpty())
        assertEquals(standar.urutan, standar.terlihat)
    }

    @Test
    fun pindahKeBawahMenukarDenganTetangga() {
        val hasil = standar.pindah(WidgetBeranda.JADWAL_HARI_INI, +1)
        assertEquals(WidgetBeranda.TUGAS, hasil.urutan[0])
        assertEquals(WidgetBeranda.JADWAL_HARI_INI, hasil.urutan[1])
    }

    @Test
    fun pindahDiLuarBatasTidakMengubahApaPun() {
        assertSame(standar, standar.pindah(WidgetBeranda.JADWAL_HARI_INI, -1))
        assertSame(standar, standar.pindah(WidgetBeranda.TUJUAN, +1))
    }

    @Test
    fun sembunyikanDanTampilkanMengubahDaftarTerlihat() {
        val tersembunyi = standar.sembunyikan(WidgetBeranda.KEUANGAN)
        assertFalse(WidgetBeranda.KEUANGAN in tersembunyi.terlihat)
        assertEquals(WidgetBeranda.entries.size - 1, tersembunyi.terlihat.size)
        assertEquals(standar, tersembunyi.tampilkan(WidgetBeranda.KEUANGAN))
    }

    @Test
    fun urungkanMengembalikanSusunanSebelumnya() {
        val awal = RiwayatTataLetak()
        val diubah = awal.terapkan(standar.pindah(WidgetBeranda.TUGAS, +1))
        assertTrue(diubah.dapatDiurungkan)
        val kembali = diubah.urungkan()
        assertEquals(standar, kembali.sekarang)
        assertFalse(kembali.dapatDiurungkan)
    }

    @Test
    fun perubahanKosongTidakMasukRiwayat() {
        val awal = RiwayatTataLetak()
        val hasil = awal.terapkan(standar)
        assertSame(awal, hasil)
        assertFalse(hasil.dapatDiurungkan)
    }

    @Test
    fun urungkanTanpaRiwayatTidakMengubahApaPun() {
        val awal = RiwayatTataLetak()
        assertSame(awal, awal.urungkan())
    }

    @Test
    fun kembalikanStandarBisaDiurungkan() {
        val diubah = RiwayatTataLetak().terapkan(standar.sembunyikan(WidgetBeranda.SHOLAT))
        val reset = diubah.kembalikanStandar()
        assertEquals(standar, reset.sekarang)
        assertEquals(diubah.sekarang, reset.urungkan().sekarang)
    }

    @Test
    fun riwayatDibatasi() {
        var r = RiwayatTataLetak()
        var layout = standar
        repeat(RiwayatTataLetak.BATAS_RIWAYAT + 10) {
            layout = layout.pindah(WidgetBeranda.JADWAL_HARI_INI, if (it % 2 == 0) +1 else -1)
            r = r.terapkan(layout)
        }
        assertEquals(RiwayatTataLetak.BATAS_RIWAYAT, r.riwayat.size)
    }
}
