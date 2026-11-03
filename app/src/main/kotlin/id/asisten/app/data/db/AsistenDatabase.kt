package id.asisten.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration

@Database(
    entities = [
        MemoriEntitas::class,
        KegiatanEntitas::class,
        JamSekolahEntitas::class,
        PelajaranEntitas::class,
        PiketEntitas::class,
        TugasEntitas::class,
        TujuanEntitas::class,
        TonggakEntitas::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AsistenDatabase : RoomDatabase() {
    abstract fun memoriDao(): MemoriDao
    abstract fun kegiatanDao(): KegiatanDao
    abstract fun sekolahDao(): SekolahDao
    abstract fun tugasDao(): TugasDao
    abstract fun tujuanDao(): TujuanDao

    companion object {
        const val NAMA = "asisten.db"

        /**
         * Deliberately NO fallbackToDestructiveMigration(): a missing migration must fail loudly in
         * testing instead of silently wiping the user's schedule, notes and goals.
         */
        fun buat(context: Context): AsistenDatabase =
            Room.databaseBuilder(context.applicationContext, AsistenDatabase::class.java, NAMA)
                .addMigrations(*AsistenMigrasi.SEMUA)
                .build()
    }
}

/**
 * Migration policy
 *  - Version 1 is the first schema (all Phase 3 and Phase 4 tables). No build was ever installed
 *    before it, so there is nothing to migrate from yet.
 *  - Any later change to Entitas.kt MUST bump `version`, add a Migration here, and commit the new
 *    schema JSON that the KSP `room.schemaLocation` option writes into app/schemas/.
 */
object AsistenMigrasi {
    val SEMUA: Array<Migration> = emptyArray()
}
