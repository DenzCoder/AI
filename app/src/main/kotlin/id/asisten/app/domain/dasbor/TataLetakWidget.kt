package id.asisten.app.domain.dasbor

/** Cards that can appear on the Beranda dashboard. */
enum class WidgetBeranda {
    JADWAL_HARI_INI,
    TUGAS,
    SEKOLAH,
    SHOLAT,
    KEBIASAAN,
    KEUANGAN,
    TUJUAN,
}

/** Immutable dashboard arrangement: order of all widgets plus which ones are hidden. */
data class TataLetakWidget(
    val urutan: List<WidgetBeranda>,
    val tersembunyi: Set<WidgetBeranda> = emptySet(),
) {
    val terlihat: List<WidgetBeranda> get() = urutan.filter { it !in tersembunyi }

    /** Swaps [widget] with its neighbour. [arah] is -1 (up) or +1 (down); out-of-range is a no-op. */
    fun pindah(widget: WidgetBeranda, arah: Int): TataLetakWidget {
        val asal = urutan.indexOf(widget)
        if (asal < 0) return this
        val tujuan = asal + arah
        if (tujuan !in urutan.indices) return this
        val baru = urutan.toMutableList()
        baru[asal] = baru[tujuan]
        baru[tujuan] = widget
        return copy(urutan = baru)
    }

    fun sembunyikan(widget: WidgetBeranda): TataLetakWidget = copy(tersembunyi = tersembunyi + widget)

    fun tampilkan(widget: WidgetBeranda): TataLetakWidget = copy(tersembunyi = tersembunyi - widget)

    companion object {
        val STANDAR = TataLetakWidget(urutan = WidgetBeranda.entries.toList())
    }
}

/** Arrangement plus a bounded undo history. Changes that do not alter the layout are not recorded. */
data class RiwayatTataLetak(
    val sekarang: TataLetakWidget = TataLetakWidget.STANDAR,
    val riwayat: List<TataLetakWidget> = emptyList(),
) {
    val dapatDiurungkan: Boolean get() = riwayat.isNotEmpty()

    fun terapkan(baru: TataLetakWidget): RiwayatTataLetak =
        if (baru == sekarang) this
        else copy(sekarang = baru, riwayat = (riwayat + sekarang).takeLast(BATAS_RIWAYAT))

    fun urungkan(): RiwayatTataLetak =
        if (riwayat.isEmpty()) this
        else copy(sekarang = riwayat.last(), riwayat = riwayat.dropLast(1))

    fun kembalikanStandar(): RiwayatTataLetak = terapkan(TataLetakWidget.STANDAR)

    companion object {
        const val BATAS_RIWAYAT = 20
    }
}
