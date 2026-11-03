package id.asisten.app.data.db

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests against a real (in-memory) Room database. They need an emulator or device,
 * so they are NOT part of `testDebugUnitTest`; run them with `./gradlew connectedDebugAndroidTest`.
 */
@RunWith(AndroidJUnit4::class)
class AsistenDatabaseTest {

    private lateinit var db: AsistenDatabase

    @Before
    fun buat() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AsistenDatabase::class.java,
        ).allowMainThreadQueries().build()
    }

    @After
    fun tutup() {
        db.close()
    }

    @Test
    fun memoriTerbaruTampilPertama() = runBlocking {
        val dao = db.memoriDao()
        dao.sisip(MemoriEntitas(kategori = "PREFERENSI", isi = "lama", sensitif = false, dibuat = 1))
        dao.sisip(MemoriEntitas(kategori = "KEBIASAAN", isi = "baru", sensitif = true, dibuat = 2))
        val daftar = dao.amati().first()
        assertEquals(listOf("baru", "lama"), daftar.map { it.isi })
        assertTrue(daftar.first().sensitif)
    }

    @Test
    fun hapusDanHapusSemuaMemori() = runBlocking {
        val dao = db.memoriDao()
        val id = dao.sisip(MemoriEntitas(kategori = "TUJUAN", isi = "a", sensitif = false, dibuat = 1))
        dao.sisip(MemoriEntitas(kategori = "KONTEKS", isi = "b", sensitif = false, dibuat = 2))
        dao.hapus(id)
        assertEquals(listOf("b"), dao.amati().first().map { it.isi })
        dao.hapusSemua()
        assertTrue(dao.amati().first().isEmpty())
    }

    @Test
    fun jamSekolahMenggantikanBarisHariYangSama() = runBlocking {
        val dao = db.sekolahDao()
        dao.simpanJam(JamSekolahEntitas(hari = 1, masukMenit = 7 * 60, pulangMenit = 14 * 60))
        dao.simpanJam(JamSekolahEntitas(hari = 1, masukMenit = 7 * 60 + 30, pulangMenit = 15 * 60))
        val daftar = dao.amatiJam().first()
        assertEquals(1, daftar.size)
        assertEquals(7 * 60 + 30, daftar.single().masukMenit)
    }

    @Test
    fun menghapusTujuanJugaMenghapusTonggaknya() = runBlocking {
        val dao = db.tujuanDao()
        val tujuanId = dao.sisipTujuan(
            TujuanEntitas(judul = "Lulus ujian", jangka = "PENDEK", tenggatHari = null, selesai = false, selesaiPada = null),
        )
        dao.sisipTonggak(TonggakEntitas(tujuanId = tujuanId, judul = "Belajar bab 1", selesai = false))
        dao.sisipTonggak(TonggakEntitas(tujuanId = tujuanId, judul = "Latihan soal", selesai = true))
        assertEquals(2, dao.amatiTonggak().first().size)

        dao.hapusTujuan(tujuanId)

        assertTrue(dao.amatiTujuan().first().isEmpty())
        assertTrue(dao.amatiTonggak().first().isEmpty())
    }

    @Test
    fun tugasBisaDiubahMenjadiSelesai() = runBlocking {
        val dao = db.tugasDao()
        val id = dao.sisip(
            TugasEntitas(
                judul = "PR IPA", jenis = "PR", mapel = "IPA", tenggatHari = 20000L,
                prioritas = "SEDANG", selesai = false, selesaiPada = null, catatan = "",
            ),
        )
        val asli = dao.amati().first().single { it.id == id }
        dao.ubah(asli.copy(selesai = true, selesaiPada = 123L))
        val sesudah = dao.amati().first().single { it.id == id }
        assertTrue(sesudah.selesai)
        assertEquals(123L, sesudah.selesaiPada)
    }
}
