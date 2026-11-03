package id.asisten.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoriDao {
    @Query("SELECT * FROM memori ORDER BY dibuat DESC, id DESC")
    fun amati(): Flow<List<MemoriEntitas>>

    @Insert
    suspend fun sisip(item: MemoriEntitas): Long

    @Update
    suspend fun ubah(item: MemoriEntitas)

    @Query("DELETE FROM memori WHERE id = :id")
    suspend fun hapus(id: Long)

    @Query("DELETE FROM memori")
    suspend fun hapusSemua()
}

@Dao
interface KegiatanDao {
    @Query("SELECT * FROM kegiatan ORDER BY id")
    fun amati(): Flow<List<KegiatanEntitas>>

    @Insert
    suspend fun sisip(item: KegiatanEntitas): Long

    @Update
    suspend fun ubah(item: KegiatanEntitas)

    @Query("DELETE FROM kegiatan WHERE id = :id")
    suspend fun hapus(id: Long)
}

@Dao
interface SekolahDao {
    @Query("SELECT * FROM jam_sekolah ORDER BY hari")
    fun amatiJam(): Flow<List<JamSekolahEntitas>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun simpanJam(jam: JamSekolahEntitas)

    @Query("DELETE FROM jam_sekolah WHERE hari = :hari")
    suspend fun hapusJam(hari: Int)

    @Query("SELECT * FROM pelajaran ORDER BY hari, mulaiMenit, id")
    fun amatiPelajaran(): Flow<List<PelajaranEntitas>>

    @Insert
    suspend fun sisipPelajaran(item: PelajaranEntitas): Long

    @Update
    suspend fun ubahPelajaran(item: PelajaranEntitas)

    @Query("DELETE FROM pelajaran WHERE id = :id")
    suspend fun hapusPelajaran(id: Long)

    @Query("SELECT * FROM piket ORDER BY hari, id")
    fun amatiPiket(): Flow<List<PiketEntitas>>

    @Insert
    suspend fun sisipPiket(item: PiketEntitas): Long

    @Query("DELETE FROM piket WHERE id = :id")
    suspend fun hapusPiket(id: Long)
}

@Dao
interface TugasDao {
    @Query("SELECT * FROM tugas ORDER BY id")
    fun amati(): Flow<List<TugasEntitas>>

    @Insert
    suspend fun sisip(item: TugasEntitas): Long

    @Update
    suspend fun ubah(item: TugasEntitas)

    @Query("DELETE FROM tugas WHERE id = :id")
    suspend fun hapus(id: Long)
}

@Dao
interface TujuanDao {
    @Query("SELECT * FROM tujuan ORDER BY id")
    fun amatiTujuan(): Flow<List<TujuanEntitas>>

    @Query("SELECT * FROM tonggak ORDER BY id")
    fun amatiTonggak(): Flow<List<TonggakEntitas>>

    @Insert
    suspend fun sisipTujuan(item: TujuanEntitas): Long

    @Update
    suspend fun ubahTujuan(item: TujuanEntitas)

    /** Milestones are removed with the goal through the foreign key's ON DELETE CASCADE. */
    @Query("DELETE FROM tujuan WHERE id = :id")
    suspend fun hapusTujuan(id: Long)

    @Insert
    suspend fun sisipTonggak(item: TonggakEntitas): Long

    @Update
    suspend fun ubahTonggak(item: TonggakEntitas)

    @Query("DELETE FROM tonggak WHERE id = :id")
    suspend fun hapusTonggak(id: Long)
}
