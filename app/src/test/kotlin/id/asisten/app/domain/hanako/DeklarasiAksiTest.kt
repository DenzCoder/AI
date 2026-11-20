package id.asisten.app.domain.hanako

import id.asisten.app.domain.hanako.DataUji.panggil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The declarations are what the model sees; [UraiAksi] is what the app accepts. These tests are driven from the
 * declarations so the two cannot drift apart: every declared name and field must really be read by the parser.
 */
class DeklarasiAksiTest {

    private val fungsi = DeklarasiAksi.fungsiAksi

    @Suppress("UNCHECKED_CAST")
    private fun sifat(f: Map<String, Any?>): Map<String, Map<String, Any?>> =
        ((f["parameters"] as? Map<String, Any?>)?.get("properties") as? Map<String, Map<String, Any?>>) ?: emptyMap()

    @Suppress("UNCHECKED_CAST")
    private fun wajib(f: Map<String, Any?>): List<String> =
        ((f["parameters"] as? Map<String, Any?>)?.get("required") as? List<String>) ?: emptyList()

    /** One valid call per function (with every id existing in [DataUji.data]); the mode function needs an active mode. */
    private val dasar: Map<String, Map<String, Any?>> = mapOf(
        NamaAksi.TAMBAH_TUGAS to mapOf("judul" to "PR"),
        NamaAksi.UBAH_TUGAS to mapOf("id" to 1.0, "prioritas" to "RENDAH"),
        NamaAksi.TANDAI_TUGAS to mapOf("id" to 1.0, "selesai" to true),
        NamaAksi.HAPUS_TUGAS to mapOf("id" to 1.0),
        NamaAksi.TAMBAH_KEGIATAN to mapOf("judul" to "Les", "hari" to "RABU", "mulai" to "16:00", "selesai" to "17:00"),
        NamaAksi.UBAH_KEGIATAN to mapOf("id" to 10.0, "mulai" to "15:00"),
        NamaAksi.HAPUS_KEGIATAN to mapOf("id" to 10.0),
        NamaAksi.TAMBAH_PELAJARAN to mapOf("hari" to "SENIN", "mulai" to "10:00", "selesai" to "11:00", "nama" to "IPA"),
        NamaAksi.UBAH_PELAJARAN to mapOf("id" to 20.0, "nama" to "Mat"),
        NamaAksi.HAPUS_PELAJARAN to mapOf("id" to 20.0),
        NamaAksi.ATUR_PENGINGAT to mapOf("tunda_menit" to 15.0),
        NamaAksi.MULAI_MODE to mapOf("mode" to "FOKUS"),
        NamaAksi.AKHIRI_MODE to emptyMap(),
    )

    private fun data() = DataUji.dataModeAktif()

    @Test
    fun namaYangDideklarasikanSamaDenganYangDikenalParser() {
        val nama = fungsi.map { it["name"] as String }
        assertEquals(nama.size, nama.toSet().size)
        assertEquals(NamaAksi.SEMUA.toSet(), nama.toSet())
        assertEquals(NamaAksi.SEMUA.toSet(), dasar.keys)
    }

    @Test
    fun setiapContohDasarSah() {
        for ((nama, args) in dasar) {
            val hasil = UraiAksi.urai(PanggilanFungsi(nama, args), data())
            assertTrue("$nama: $hasil", hasil is HasilUrai.Sah)
        }
    }

    @Test
    fun setiapIsianTerdeklarasiBenarBenarDibacaParser() {
        var diuji = 0
        for (f in fungsi) {
            val nama = f["name"] as String
            for ((kunci, skema) in sifat(f)) {
                val buruk: Any = when (skema["type"]) {
                    "STRING" -> when {
                        skema.containsKey("enum") -> "###"
                        (skema["description"] as String).contains("Format") -> "###"
                        else -> "x".repeat(400)
                    }
                    "INTEGER" -> -999.0
                    "BOOLEAN" -> "mungkin"
                    else -> error("tipe tak dikenal di $nama.$kunci")
                }
                val hasil = UraiAksi.urai(PanggilanFungsi(nama, dasar.getValue(nama) + (kunci to buruk)), data())
                assertTrue("$nama.$kunci: nilai buruk seharusnya ditolak, tetapi $hasil", hasil is HasilUrai.Tidak)
                assertTrue(
                    "$nama.$kunci: alasan tidak menyebut '$kunci': ${(hasil as HasilUrai.Tidak).alasan}",
                    hasil.alasan.contains("'$kunci'"),
                )
                diuji++
            }
        }
        assertTrue("terlalu sedikit isian diuji: $diuji", diuji >= 50)
    }

    @Test
    fun setiapIsianWajibBenarBenarDiwajibkanParser() {
        var diuji = 0
        for (f in fungsi) {
            val nama = f["name"] as String
            for (kunci in wajib(f)) {
                assertTrue("$nama: contoh dasar tidak memuat '$kunci'", dasar.getValue(nama).containsKey(kunci))
                val tanpa = dasar.getValue(nama) - kunci
                val hasil = UraiAksi.urai(PanggilanFungsi(nama, tanpa), data())
                assertTrue("$nama tanpa $kunci seharusnya ditolak, tetapi $hasil", hasil is HasilUrai.Tidak)
                diuji++
            }
        }
        assertTrue(diuji >= 15)
    }

    @Test
    fun isianWajibAdalahBagianDariIsianTerdeklarasi() {
        for (f in fungsi) {
            assertTrue((f["name"] as String), sifat(f).keys.containsAll(wajib(f)))
        }
    }

    @Test
    fun daftarPilihanSamaDenganEnumDiAplikasi() {
        val semua = fungsi.flatMap { sifat(it).entries }
        fun pilihan(kunci: String) = semua.filter { it.key == kunci }.map { (it.value["enum"] as List<*>).map { v -> v as String } }.toSet()
        assertEquals(setOf(id.asisten.app.domain.Prioritas.entries.map { it.name }), pilihan("prioritas"))
        assertEquals(setOf(id.asisten.app.domain.aktivitas.JenisTugas.entries.map { it.name }), pilihan("jenis"))
        assertEquals(setOf(listOf("SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU", "MINGGU")), pilihan("hari"))
        assertEquals(setOf(listOf("FOKUS", "ISTIRAHAT")), pilihan("mode"))
        // Setiap pilihan diterima parser.
        for (p in id.asisten.app.domain.Prioritas.entries) {
            assertTrue(UraiAksi.urai(panggil(NamaAksi.TAMBAH_TUGAS, "judul" to "a", "prioritas" to p.name), data()) is HasilUrai.Sah)
        }
    }

    @Test
    fun jsonValidDanBentukSesuaiApi() {
        val akar = MiniJson.urai(DeklarasiAksi.json()) as List<*>
        assertEquals(1, akar.size)
        val deklarasi = MiniJson.ambil(akar, 0, "functionDeclarations") as List<*>
        assertEquals(NamaAksi.SEMUA.size, deklarasi.size)
        for (d in deklarasi) {
            val peta = d as Map<*, *>
            assertTrue((peta["name"] as String).matches(Regex("[a-z_]+")))
            assertTrue((peta["description"] as String).isNotBlank())
            val params = peta["parameters"] as Map<*, *>?
            if (params != null) {
                assertEquals("OBJECT", params["type"])
                for ((_, skema) in params["properties"] as Map<*, *>) {
                    val tipe = (skema as Map<*, *>)["type"]
                    assertTrue(tipe in setOf("STRING", "INTEGER", "BOOLEAN"))
                    assertTrue((skema["description"] as String).isNotBlank())
                }
            }
        }
    }

    @Test
    fun fungsiTanpaIsianTidakMemuatParameters() {
        val akhiri = fungsi.first { it["name"] == NamaAksi.AKHIRI_MODE }
        assertFalse(akhiri.containsKey("parameters"))
    }

    @Test
    fun teksDeklarasiTidakMemuatKataTerlarang() {
        // Badan permintaan tidak boleh memuat kunci API atau pengaturan thinking (lihat ProtokolGeminiTest).
        val json = DeklarasiAksi.json().lowercase()
        assertFalse(json.contains("key"))
        assertFalse(json.contains("thinking"))
    }

    // ---------- bentuk OpenAI (Groq, DeepSeek) ----------

    private val alatOpenAi = DeklarasiAksi.alatOpenAi.map { it as Map<*, *> }

    @Suppress("UNCHECKED_CAST")
    private fun isiFungsi(a: Map<*, *>) = a["function"] as Map<String, Any?>

    @Test
    fun bentukOpenAiMemuatFungsiYangSamaDenganGemini() {
        assertEquals(NamaAksi.SEMUA, alatOpenAi.map { isiFungsi(it)["name"] })
        assertEquals(fungsi.map { it["description"] }, alatOpenAi.map { isiFungsi(it)["description"] })
        assertTrue(alatOpenAi.all { it["type"] == "function" })
    }

    @Test
    fun jenisSkemaMenjadiHurufKecilTanpaMengubahIsiPilihan() {
        val teks = DeklarasiAksi.alatOpenAi.let { MiniJson.tulis(it) }
        for (besar in listOf("\"STRING\"", "\"INTEGER\"", "\"BOOLEAN\"", "\"OBJECT\"")) assertFalse(besar, teks.contains(besar))
        assertTrue(teks.contains("\"type\":\"string\""))
        assertTrue(teks.contains("\"type\":\"integer\""))
        assertTrue(teks.contains("\"type\":\"boolean\""))
        // nilai pilihan (enum) tetap huruf besar karena itulah yang dibaca UraiAksi
        val tambah = alatOpenAi.map { isiFungsi(it) }.first { it["name"] == NamaAksi.TAMBAH_TUGAS }
        @Suppress("UNCHECKED_CAST")
        val sifat = (tambah["parameters"] as Map<String, Any?>)["properties"] as Map<String, Map<String, Any?>>
        assertEquals(listOf("TUGAS", "PR", "UJIAN"), sifat["jenis"]!!["enum"])
    }

    @Test
    fun setiapFungsiOpenAiPunyaParametersObjekBahkanTanpaBidang() {
        for (a in alatOpenAi) {
            val f = isiFungsi(a)
            @Suppress("UNCHECKED_CAST")
            val p = f["parameters"] as Map<String, Any?>
            assertEquals(f["name"].toString(), "object", p["type"])
            assertTrue(p["properties"] is Map<*, *>)
        }
        val akhiri = alatOpenAi.map { isiFungsi(it) }.first { it["name"] == NamaAksi.AKHIRI_MODE }
        @Suppress("UNCHECKED_CAST")
        assertTrue(((akhiri["parameters"] as Map<String, Any?>)["properties"] as Map<*, *>).isEmpty())
    }

    @Test
    fun bentukOpenAiTidakMengubahDeklarasiGemini() {
        val teks = DeklarasiAksi.json()
        assertTrue(teks.contains("\"type\":\"OBJECT\""))
        assertTrue(teks.contains("\"type\":\"STRING\""))
    }

    @Test
    fun bidangWajibDanSifatSamaDiKeduaBentuk() {
        for ((g, o) in fungsi.zip(alatOpenAi.map { isiFungsi(it) })) {
            assertEquals(g["name"].toString(), wajib(g), wajib(o))
            assertEquals(g["name"].toString(), sifat(g).keys, sifat(o).keys)
        }
    }
}

