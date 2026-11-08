package id.asisten.app.domain.suara

/**
 * Turns a chat reply into text a speech engine reads well. Replies contain things that sound wrong when read
 * aloud: bullets, markdown marks, emoji, long dashes, "07:00–08:30", web addresses.
 * Pure and unit-tested; the engine itself (TextToSpeech) is only reached from the Android layer.
 */
object PenyiapTeksUcap {
    /** TextToSpeech refuses very long input (its limit is about 4000 characters), so longer replies are cut. */
    const val MAKS_PANJANG = 3500

    private val ALAMAT_WEB = Regex("""https?://\S+""")
    private val RENTANG_JAM = Regex("""\b(\d{1,2}):(\d{2})\s*[–-]\s*(\d{1,2}):(\d{2})\b""")
    private val JAM = Regex("""\b(\d{1,2}):(\d{2})\b""")
    private val PENANDA_BARIS = Regex("""^\s*(?:[•\-*]|\d+[.)])\s+""")
    private val PENEKANAN = Regex("""[*`#]+""")
    private val SPASI = Regex("""\s+""")

    fun siapkan(teks: String): String {
        var t = teks
        t = ALAMAT_WEB.replace(t, "tautan")
        t = RENTANG_JAM.replace(t) { m ->
            "${ucapJam(m.groupValues[1], m.groupValues[2])} sampai ${ucapJam(m.groupValues[3], m.groupValues[4])}"
        }
        t = JAM.replace(t) { m -> ucapJam(m.groupValues[1], m.groupValues[2]) }

        // Emoji and markdown marks go first: otherwise a line that ends in an emoji would get its full stop after it.
        t = hapusEmoji(t)
        t = PENEKANAN.replace(t, "").replace("_", " ")

        // One spoken sentence per line, so list items are separated by a pause.
        t = t.lines().map { PENANDA_BARIS.replace(it, "").trim() }.filter { it.isNotEmpty() }.joinToString(" ") { baris ->
            if (baris.last() in ".!?:;,") baris else "$baris."
        }

        t = t.replace("—", ", ").replace("–", ", ").replace("%", " persen").replace("…", "...")
        t = SPASI.replace(t, " ").replace(" ,", ",").trim()
        return potong(t)
    }

    /** "16", "20" -> "16 lewat 20"; "07", "00" -> "7". */
    private fun ucapJam(jam: String, menit: String): String {
        val j = jam.toInt()
        val m = menit.toInt()
        return if (m == 0) "$j" else "$j lewat $m"
    }

    private fun hapusEmoji(t: String): String {
        val sb = StringBuilder(t.length)
        var i = 0
        while (i < t.length) {
            val kode = t.codePointAt(i)
            val panjang = Character.charCount(kode)
            val emoji = kode >= 0x1F000 || kode in 0x2600..0x27BF || kode == 0xFE0F || kode == 0x200D || kode in 0x2B00..0x2BFF
            if (!emoji) sb.append(t, i, i + panjang)
            i += panjang
        }
        return sb.toString()
    }

    private fun potong(t: String): String {
        if (t.length <= MAKS_PANJANG) return t
        val batas = t.lastIndexOf(". ", MAKS_PANJANG)
        return if (batas > MAKS_PANJANG / 2) t.substring(0, batas + 1) else t.substring(0, MAKS_PANJANG)
    }
}
