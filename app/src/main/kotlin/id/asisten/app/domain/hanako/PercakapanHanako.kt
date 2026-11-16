package id.asisten.app.domain.hanako

enum class PeranChat { PENGGUNA, HANAKO }

/**
 * One chat message. [ekspresi] is only set on Hanako's messages (her mood for that reply).
 * [lokal] is true when the offline responder wrote it instead of Gemini, so the screen can say so honestly.
 * [aksi] is true for the app's own report of an approved or rejected change ("Sudah dilakukan: ..."), which the
 * AI did not write and which is not read aloud. A message is never both [lokal] and [aksi].
 */
data class PesanChat(
    val peran: PeranChat,
    val isi: String,
    val ekspresi: EkspresiHanako? = null,
    val waktu: Long = 0L,
    val lokal: Boolean = false,
    val aksi: Boolean = false,
)

object RiwayatChat {
    const val MAKS_PESAN = 40
    const val MAKS_PANJANG = 4000

    /** How many of the latest messages are sent to the model as conversation history. */
    const val PESAN_KE_MODEL = 16

    /** Appends [pesan] (text cut to [MAKS_PANJANG]) and keeps only the newest [MAKS_PESAN] messages. */
    fun tambah(daftar: List<PesanChat>, pesan: PesanChat): List<PesanChat> =
        (daftar + pesan.copy(isi = pesan.isi.take(MAKS_PANJANG))).takeLast(MAKS_PESAN)

    /**
     * Turns the stored history into what Gemini accepts: the newest [maks] messages, starting with a user turn
     * (leading Hanako messages are dropped), with consecutive messages of the same role merged into one.
     */
    fun untukModel(daftar: List<PesanChat>, maks: Int = PESAN_KE_MODEL): List<PesanChat> {
        val terakhir = daftar.takeLast(maks).dropWhile { it.peran != PeranChat.PENGGUNA }
        val hasil = ArrayList<PesanChat>()
        for (p in terakhir) {
            val sebelum = hasil.lastOrNull()
            if (sebelum != null && sebelum.peran == p.peran) {
                hasil[hasil.lastIndex] = sebelum.copy(isi = sebelum.isi + "\n\n" + p.isi)
            } else {
                hasil.add(p)
            }
        }
        return hasil
    }
}

/**
 * Stores the chat as text in DataStore: one message per line, `U|H`, expression name or `-`, time, a kind flag
 * (`0` normal, `1` [PesanChat.lokal], `2` [PesanChat.aksi]), then the text with `\`, line breaks and `|` escaped. Unreadable lines are skipped, never fatal.
 */
object RiwayatChatCodec {

    fun encode(daftar: List<PesanChat>): String = daftar.joinToString("\n") { p ->
        val peran = if (p.peran == PeranChat.PENGGUNA) "U" else "H"
        val ekspresi = p.ekspresi?.name ?: "-"
        val jenis = if (p.aksi) "2" else if (p.lokal) "1" else "0"
        "$peran|$ekspresi|${p.waktu}|$jenis|${lolos(p.isi)}"
    }

    fun decode(teks: String?): List<PesanChat> {
        if (teks.isNullOrEmpty()) return emptyList()
        val hasil = ArrayList<PesanChat>()
        for (baris in teks.split("\n")) {
            val bagian = baris.split("|", limit = 5)
            if (bagian.size != 5) continue
            val peran = when (bagian[0]) {
                "U" -> PeranChat.PENGGUNA
                "H" -> PeranChat.HANAKO
                else -> continue
            }
            val ekspresi = if (bagian[1] == "-") null else EkspresiHanako.entries.firstOrNull { it.name == bagian[1] }
            val waktu = bagian[2].toLongOrNull() ?: continue
            val isi = bukaLolos(bagian[4])
            if (isi.isEmpty()) continue
            hasil.add(PesanChat(peran, isi, ekspresi, waktu, lokal = bagian[3] == "1", aksi = bagian[3] == "2"))
        }
        return hasil.takeLast(RiwayatChat.MAKS_PESAN)
    }

    private fun lolos(s: String): String {
        val sb = StringBuilder(s.length + 8)
        for (c in s) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '|' -> sb.append("\\p")
                else -> sb.append(c)
            }
        }
        return sb.toString()
    }

    private fun bukaLolos(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (s[i + 1]) {
                    '\\' -> sb.append('\\')
                    'n' -> sb.append('\n')
                    'r' -> sb.append('\r')
                    'p' -> sb.append('|')
                    else -> sb.append(c).append(s[i + 1])
                }
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
