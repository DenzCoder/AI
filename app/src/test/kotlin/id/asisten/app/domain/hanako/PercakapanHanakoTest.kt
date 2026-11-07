package id.asisten.app.domain.hanako

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PercakapanHanakoTest {

    private fun u(isi: String, waktu: Long = 0) = PesanChat(PeranChat.PENGGUNA, isi, waktu = waktu)
    private fun h(isi: String, waktu: Long = 0) = PesanChat(PeranChat.HANAKO, isi, EkspresiHanako.SENYUM, waktu)

    @Test
    fun tambahMembatasiJumlahDanPanjangPesan() {
        var daftar = emptyList<PesanChat>()
        for (i in 1..50) daftar = RiwayatChat.tambah(daftar, u("pesan $i", i.toLong()))
        assertEquals(RiwayatChat.MAKS_PESAN, daftar.size)
        assertEquals("pesan 11", daftar.first().isi)
        assertEquals("pesan 50", daftar.last().isi)

        val panjang = RiwayatChat.tambah(emptyList(), u("x".repeat(10_000)))
        assertEquals(RiwayatChat.MAKS_PANJANG, panjang.single().isi.length)
    }

    @Test
    fun untukModelMembuangPesanHanakoDiAwal() {
        val hasil = RiwayatChat.untukModel(listOf(h("sapaan"), h("lagi"), u("halo"), h("hai")))
        assertEquals(listOf("halo", "hai"), hasil.map { it.isi })
        assertEquals(PeranChat.PENGGUNA, hasil.first().peran)
    }

    @Test
    fun untukModelMenggabungPesanBeruntunDenganPeranSama() {
        val hasil = RiwayatChat.untukModel(listOf(u("satu"), u("dua"), h("jawab"), u("tiga")))
        assertEquals(3, hasil.size)
        assertEquals("satu\n\ndua", hasil[0].isi)
        assertEquals(PeranChat.HANAKO, hasil[1].peran)
        assertEquals("tiga", hasil[2].isi)
    }

    @Test
    fun untukModelHanyaMengambilPesanTerbaru() {
        val daftar = (1..30).flatMap { listOf(u("u$it"), h("h$it")) }
        val hasil = RiwayatChat.untukModel(daftar, maks = 6)
        assertEquals(listOf("u28", "h28", "u29", "h29", "u30", "h30"), hasil.map { it.isi })
        assertTrue(RiwayatChat.untukModel(emptyList()).isEmpty())
        assertTrue(RiwayatChat.untukModel(listOf(h("sendiri"))).isEmpty())
    }

    @Test
    fun kodekMengembalikanPesanPersisSamaTermasukKarakterKhusus() {
        val daftar = listOf(
            u("halo | dunia", 1),
            h("baris satu\nbaris dua\r\nbaris tiga", 2).copy(lokal = true),
            u("garis miring \\ dan \\n harfiah dan \\p harfiah", 3),
            h("[x] | | |", 4).copy(ekspresi = null),
            u("emoji \uD83D\uDE00 é", 5),
        )
        assertEquals(daftar, RiwayatChatCodec.decode(RiwayatChatCodec.encode(daftar)))
    }

    @Test
    fun hasilEncodeSatuPesanSatuBaris() {
        val teks = RiwayatChatCodec.encode(listOf(u("a\nb"), h("c")))
        assertEquals(2, teks.split("\n").size)
    }

    @Test
    fun decodeMelewatiBarisRusakTanpaGagal() {
        val teks = listOf(
            "U|-|10|0|baik",
            "rusak total",
            "X|-|10|0|peran salah",
            "U|-|bukan-angka|0|waktu salah",
            "H|EKSPRESI_ENTAH|20|0|ekspresi tak dikenal",
            "U|-|30|0|",
            "H|SENYUM|40|1|lokal",
        ).joinToString("\n")
        val hasil = RiwayatChatCodec.decode(teks)
        assertEquals(listOf("baik", "ekspresi tak dikenal", "lokal"), hasil.map { it.isi })
        assertNull(hasil[1].ekspresi)
        assertTrue(hasil[2].lokal)
    }

    @Test
    fun decodeNullAtauKosongMenghasilkanDaftarKosong() {
        assertTrue(RiwayatChatCodec.decode(null).isEmpty())
        assertTrue(RiwayatChatCodec.decode("").isEmpty())
        assertEquals("", RiwayatChatCodec.encode(emptyList()))
    }

    @Test
    fun decodeMembatasiJumlahPesan() {
        val daftar = (1..60).map { u("p$it", it.toLong()) }
        val hasil = RiwayatChatCodec.decode(RiwayatChatCodec.encode(daftar))
        assertEquals(RiwayatChat.MAKS_PESAN, hasil.size)
        assertEquals("p60", hasil.last().isi)
    }
}
