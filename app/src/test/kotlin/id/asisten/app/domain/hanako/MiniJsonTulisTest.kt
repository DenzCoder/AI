package id.asisten.app.domain.hanako

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MiniJsonTulisTest {

    @Test
    fun menulisNilaiDasar() {
        assertEquals("null", MiniJson.tulis(null))
        assertEquals("true", MiniJson.tulis(true))
        assertEquals("\"a\\\"b\"", MiniJson.tulis("a\"b"))
        assertEquals("[]", MiniJson.tulis(emptyList<Any>()))
        assertEquals("{}", MiniJson.tulis(emptyMap<String, Any>()))
    }

    @Test
    fun bilanganBulatTanpaTitikDesimal() {
        assertEquals("12", MiniJson.tulis(12.0))
        assertEquals("12", MiniJson.tulis(12))
        assertEquals("12", MiniJson.tulis(12L))
        assertEquals("-3", MiniJson.tulis(-3.0))
        assertEquals("1.5", MiniJson.tulis(1.5))
    }

    @Test
    fun bersambungDenganUrai() {
        val asli = mapOf(
            "id" to 7.0,
            "judul" to "PR \"Mat\" \\ baris\nbaru | tab\t",
            "tanda" to listOf(true, false, null),
            "dalam" to mapOf("a" to listOf(1.5, "x")),
            "emoji" to "halo 😀 dunia",
        )
        val balik = MiniJson.urai(MiniJson.tulis(asli))
        assertEquals(asli, balik)
    }

    @Test
    fun hasilTidakPernahMengandungBarisBaruMentah() {
        val teks = MiniJson.tulis(mapOf("a" to "satu\ndua\r\ntiga", "b" to "x\u0001y"))
        assertTrue(!teks.contains('\n'))
        assertTrue(!teks.contains('\r'))
    }

    @Test
    fun menolakTipeYangTidakDidukung() {
        assertThrows(IllegalArgumentException::class.java) { MiniJson.tulis(object {}) }
        assertThrows(IllegalArgumentException::class.java) { MiniJson.tulis(mapOf(1 to "a")) }
    }

    @Test
    fun menolakAngkaTakTerhingga() {
        assertThrows(IllegalArgumentException::class.java) { MiniJson.tulis(Double.NaN) }
        assertThrows(IllegalArgumentException::class.java) { MiniJson.tulis(Double.POSITIVE_INFINITY) }
    }

    @Test
    fun menolakSarangYangTerlaluDalam() {
        var dalam: Any? = "x"
        repeat(70) { dalam = listOf(dalam) }
        assertThrows(IllegalArgumentException::class.java) { MiniJson.tulis(dalam) }
    }
}
