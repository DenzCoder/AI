package id.asisten.app.domain.hanako

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class MiniJsonTest {

    private fun harusRusak(teks: String) {
        try {
            MiniJson.urai(teks)
            fail("Seharusnya JsonRusak untuk: $teks")
        } catch (e: MiniJson.JsonRusak) {
            // diharapkan
        }
    }

    @Test
    fun membacaObjekBersarangDanLarik() {
        val json = MiniJson.urai("""{"a":1,"b":[true,false,null,"x"],"c":{"d":-2.5e1}}""")
        assertEquals(1.0, MiniJson.ambil(json, "a"))
        assertEquals(true, MiniJson.ambil(json, "b", 0))
        assertEquals(false, MiniJson.ambil(json, "b", 1))
        assertNull(MiniJson.ambil(json, "b", 2))
        assertEquals("x", MiniJson.ambil(json, "b", 3))
        assertEquals(-25.0, MiniJson.ambil(json, "c", "d"))
    }

    @Test
    fun spasiDiAntaraTokenDiabaikan() {
        val json = MiniJson.urai(" \n { \"a\" : [ 1 , 2 ] ,\t\"b\" : { } } \r\n")
        assertEquals(2.0, MiniJson.ambil(json, "a", 1))
        assertEquals(emptyMap<String, Any?>(), MiniJson.ambil(json, "b"))
    }

    @Test
    fun membacaEscapeString() {
        val json = MiniJson.urai("\"a\\\"b\\\\c\\/d\\n\\t\\u0041\\u00e9\"")
        assertEquals("a\"b\\c/d\n\tA\u00e9", json)
    }

    @Test
    fun kutipLaluUraiMengembalikanTeksAsli() {
        val contoh = listOf(
            "",
            "halo",
            "dia bilang \"ya\"",
            "garis miring \\ dan / biasa",
            "baris1\nbaris2\r\nbaris3\ttab",
            "kontrol \u0001\u001f",
            "emoji \uD83D\uDE00 dan huruf é ü 漢字",
            "[senyum] {\"a\":1}",
        )
        for (s in contoh) assertEquals(s, MiniJson.urai(MiniJson.kutip(s)))
    }

    @Test
    fun kutipMenghasilkanJsonYangSahDiAwalDanAkhir() {
        val q = MiniJson.kutip("a\nb")
        assertEquals("\"a\\nb\"", q)
        assertTrue(!q.contains('\n'))
    }

    @Test
    fun masukanRusakMelempar() {
        listOf("", "{", "[1,", "{\"a\" 1}", "tru", "\"abc", "{}x", "[1 2]", "{\"a\":}", "\"\\q\"", "\"\\u12\"", "-", "{1:2}")
            .forEach { harusRusak(it) }
    }

    @Test
    fun terlaluDalamDitolakTanpaMenghabiskanTumpukan() {
        harusRusak("[".repeat(100) + "]".repeat(100))
        // 30 level masih boleh
        MiniJson.urai("[".repeat(30) + "]".repeat(30))
    }

    @Test
    fun ambilTidakPernahMelempar() {
        val json = MiniJson.urai("""{"a":[1,2],"b":"teks"}""")
        assertNull(MiniJson.ambil(json, "tidakada"))
        assertNull(MiniJson.ambil(json, "a", 5))
        assertNull(MiniJson.ambil(json, "a", -1))
        assertNull(MiniJson.ambil(json, "b", 0))
        assertNull(MiniJson.ambil(json, "a", "x"))
        assertNull(MiniJson.ambil(null, "a"))
    }

    @Test
    fun uraiAtauNullUntukSampah() {
        assertNull(MiniJson.uraiAtauNull("<html>bukan json</html>"))
        assertNull(MiniJson.uraiAtauNull(""))
        assertEquals(1.0, MiniJson.uraiAtauNull("1"))
    }
}
