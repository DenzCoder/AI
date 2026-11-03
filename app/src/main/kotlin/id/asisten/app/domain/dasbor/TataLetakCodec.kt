package id.asisten.app.domain.dasbor

/**
 * Text format for persisting the dashboard arrangement.
 *
 * Layout:   "URUTAN,URUTAN,...|TERSEMBUNYI,TERSEMBUNYI"
 * History:  "<current>;<oldest>;...;<newest-previous>"  (current first, then undo history oldest to newest)
 *
 * Decoding is lenient so data written by another app version never crashes the app:
 * unknown widget names are dropped, duplicates are removed, and widgets missing from the
 * stored order are appended at the end.
 */
object TataLetakCodec {
    private const val PEMISAH_BAGIAN = "|"
    private const val PEMISAH_ITEM = ","
    private const val PEMISAH_RIWAYAT = ";"

    fun encode(tataLetak: TataLetakWidget): String {
        val urutan = tataLetak.urutan.joinToString(PEMISAH_ITEM) { it.name }
        val sembunyi = WidgetBeranda.entries
            .filter { it in tataLetak.tersembunyi }
            .joinToString(PEMISAH_ITEM) { it.name }
        return urutan + PEMISAH_BAGIAN + sembunyi
    }

    fun decode(teks: String?): TataLetakWidget {
        if (teks.isNullOrBlank()) return TataLetakWidget.STANDAR
        val bagian = teks.split(PEMISAH_BAGIAN)
        val urutanTersimpan = parseDaftar(bagian.getOrElse(0) { "" }).distinct()
        val lengkap = urutanTersimpan + WidgetBeranda.entries.filter { it !in urutanTersimpan }
        val sembunyi = parseDaftar(bagian.getOrElse(1) { "" }).toSet()
        return TataLetakWidget(urutan = lengkap, tersembunyi = sembunyi)
    }

    fun encodeRiwayat(riwayat: RiwayatTataLetak): String =
        (listOf(riwayat.sekarang) + riwayat.riwayat).joinToString(PEMISAH_RIWAYAT) { encode(it) }

    fun decodeRiwayat(teks: String?): RiwayatTataLetak {
        if (teks.isNullOrBlank()) return RiwayatTataLetak()
        val bagian = teks.split(PEMISAH_RIWAYAT).filter { it.isNotBlank() }
        if (bagian.isEmpty()) return RiwayatTataLetak()
        return RiwayatTataLetak(
            sekarang = decode(bagian.first()),
            riwayat = bagian.drop(1).map { decode(it) }.takeLast(RiwayatTataLetak.BATAS_RIWAYAT),
        )
    }

    private fun parseDaftar(teks: String): List<WidgetBeranda> =
        teks.split(PEMISAH_ITEM).mapNotNull { nama ->
            WidgetBeranda.entries.firstOrNull { it.name == nama.trim() }
        }
}
