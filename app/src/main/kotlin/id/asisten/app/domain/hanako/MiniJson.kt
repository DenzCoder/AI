package id.asisten.app.domain.hanako

/**
 * A very small JSON reader and string writer, just enough for the Gemini REST API.
 * It exists so the app needs no JSON library (fewer dependencies, and the code runs in plain JVM unit tests,
 * where Android's `org.json` is only a stub).
 *
 * Parsing yields: Map<String, Any?> (object), List<Any?> (array), String, Double, Boolean or null.
 */
object MiniJson {

    class JsonRusak(pesan: String) : Exception(pesan)

    private const val KEDALAMAN_MAKS = 64

    /** Returns [teks] as a JSON string literal, quotes included. */
    fun kutip(teks: String): String {
        val sb = StringBuilder(teks.length + 2)
        sb.append('"')
        for (c in teks) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c.code < 0x20 -> sb.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> sb.append(c)
            }
        }
        sb.append('"')
        return sb.toString()
    }

    /**
     * Writes a value read by [urai] (or built by hand from Map, List, String, Number, Boolean, null) as JSON text.
     * Whole numbers are written without a decimal point, so an id read as 12.0 is written back as 12.
     * Throws [IllegalArgumentException] for any other type, or for NaN or infinite numbers.
     */
    fun tulis(nilai: Any?): String = StringBuilder().also { tulisKe(it, nilai, 0) }.toString()

    private fun tulisKe(sb: StringBuilder, nilai: Any?, kedalaman: Int) {
        require(kedalaman <= KEDALAMAN_MAKS) { "JSON terlalu dalam" }
        when (nilai) {
            null -> sb.append("null")
            is String -> sb.append(kutip(nilai))
            is Boolean -> sb.append(nilai)
            is Number -> {
                val d = nilai.toDouble()
                require(!d.isNaN() && !d.isInfinite()) { "Angka tidak sah" }
                if (d == Math.rint(d) && Math.abs(d) < 1e15) sb.append(d.toLong()) else sb.append(d)
            }
            is Map<*, *> -> {
                sb.append('{')
                var pertama = true
                for ((k, v) in nilai) {
                    require(k is String) { "Kunci objek harus string" }
                    if (!pertama) sb.append(',')
                    pertama = false
                    sb.append(kutip(k)).append(':')
                    tulisKe(sb, v, kedalaman + 1)
                }
                sb.append('}')
            }
            is List<*> -> {
                sb.append('[')
                nilai.forEachIndexed { i, v ->
                    if (i > 0) sb.append(',')
                    tulisKe(sb, v, kedalaman + 1)
                }
                sb.append(']')
            }
            else -> throw IllegalArgumentException("Tipe tidak didukung: ${nilai.javaClass.simpleName}")
        }
    }

    /** Parses [teks]. Throws [JsonRusak] on malformed input or on nesting deeper than 64 levels. */
    fun urai(teks: String): Any? {
        val pembaca = Pembaca(teks)
        pembaca.lewatiSpasi()
        val nilai = pembaca.nilai(0)
        pembaca.lewatiSpasi()
        if (!pembaca.habis()) throw JsonRusak("Sisa teks setelah nilai JSON di posisi ${pembaca.posisi}")
        return nilai
    }

    /** Like [urai] but returns null instead of throwing. (A JSON `null` also gives null.) */
    fun uraiAtauNull(teks: String): Any? = try {
        urai(teks)
    } catch (e: JsonRusak) {
        null
    }

    /**
     * Walks [jalur] (String = object key, Int = array index) down from [akar].
     * Any missing step, wrong type or out-of-range index gives null.
     */
    fun ambil(akar: Any?, vararg jalur: Any): Any? {
        var sekarang = akar
        for (langkah in jalur) {
            sekarang = when {
                langkah is String && sekarang is Map<*, *> -> sekarang[langkah]
                langkah is Int && sekarang is List<*> -> sekarang.getOrNull(langkah)
                else -> return null
            }
        }
        return sekarang
    }

    private class Pembaca(private val s: String) {
        var posisi = 0
            private set

        fun habis() = posisi >= s.length

        fun lewatiSpasi() {
            while (posisi < s.length && (s[posisi] == ' ' || s[posisi] == '\n' || s[posisi] == '\r' || s[posisi] == '\t')) {
                posisi++
            }
        }

        fun nilai(kedalaman: Int): Any? {
            if (kedalaman > KEDALAMAN_MAKS) throw JsonRusak("JSON terlalu dalam")
            if (habis()) throw JsonRusak("JSON berakhir terlalu cepat")
            return when (val c = s[posisi]) {
                '{' -> objek(kedalaman)
                '[' -> larik(kedalaman)
                '"' -> string()
                't' -> harfiah("true", true)
                'f' -> harfiah("false", false)
                'n' -> harfiah("null", null)
                else -> if (c == '-' || c in '0'..'9') angka() else throw JsonRusak("Karakter tak terduga '$c' di posisi $posisi")
            }
        }

        private fun harfiah(kata: String, hasil: Any?): Any? {
            if (!s.startsWith(kata, posisi)) throw JsonRusak("Nilai tidak dikenal di posisi $posisi")
            posisi += kata.length
            return hasil
        }

        private fun angka(): Double {
            val awal = posisi
            if (s[posisi] == '-') posisi++
            while (posisi < s.length && (s[posisi] in '0'..'9' || s[posisi] in ".eE+-")) posisi++
            return s.substring(awal, posisi).toDoubleOrNull() ?: throw JsonRusak("Angka rusak di posisi $awal")
        }

        private fun string(): String {
            posisi++ // opening quote
            val sb = StringBuilder()
            while (true) {
                if (habis()) throw JsonRusak("String tidak ditutup")
                val c = s[posisi++]
                when (c) {
                    '"' -> return sb.toString()
                    '\\' -> {
                        if (habis()) throw JsonRusak("Escape tidak lengkap")
                        when (val e = s[posisi++]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                if (posisi + 4 > s.length) throw JsonRusak("Escape \\u tidak lengkap")
                                val kode = s.substring(posisi, posisi + 4).toIntOrNull(16)
                                    ?: throw JsonRusak("Escape \\u rusak di posisi $posisi")
                                sb.append(kode.toChar())
                                posisi += 4
                            }
                            else -> throw JsonRusak("Escape tidak dikenal '\\$e'")
                        }
                    }
                    else -> sb.append(c)
                }
            }
        }

        private fun larik(kedalaman: Int): List<Any?> {
            posisi++ // [
            val hasil = ArrayList<Any?>()
            lewatiSpasi()
            if (!habis() && s[posisi] == ']') {
                posisi++
                return hasil
            }
            while (true) {
                lewatiSpasi()
                hasil.add(nilai(kedalaman + 1))
                lewatiSpasi()
                if (habis()) throw JsonRusak("Larik tidak ditutup")
                when (s[posisi++]) {
                    ',' -> continue
                    ']' -> return hasil
                    else -> throw JsonRusak("Larik rusak di posisi ${posisi - 1}")
                }
            }
        }

        private fun objek(kedalaman: Int): Map<String, Any?> {
            posisi++ // {
            val hasil = LinkedHashMap<String, Any?>()
            lewatiSpasi()
            if (!habis() && s[posisi] == '}') {
                posisi++
                return hasil
            }
            while (true) {
                lewatiSpasi()
                if (habis() || s[posisi] != '"') throw JsonRusak("Kunci objek harus berupa string di posisi $posisi")
                val kunci = string()
                lewatiSpasi()
                if (habis() || s[posisi] != ':') throw JsonRusak("Tanda ':' hilang di posisi $posisi")
                posisi++
                lewatiSpasi()
                hasil[kunci] = nilai(kedalaman + 1)
                lewatiSpasi()
                if (habis()) throw JsonRusak("Objek tidak ditutup")
                when (s[posisi++]) {
                    ',' -> continue
                    '}' -> return hasil
                    else -> throw JsonRusak("Objek rusak di posisi ${posisi - 1}")
                }
            }
        }
    }
}
