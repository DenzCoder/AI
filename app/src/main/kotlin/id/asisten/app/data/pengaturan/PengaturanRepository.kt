package id.asisten.app.data.pengaturan

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.asisten.app.domain.dasbor.RiwayatTataLetak
import id.asisten.app.domain.dasbor.TataLetakCodec
import id.asisten.app.domain.keEnum
import id.asisten.app.domain.pengaturan.Aksen
import id.asisten.app.domain.pengaturan.ModeTema
import id.asisten.app.domain.pengaturan.Pengaturan
import id.asisten.app.domain.pengaturan.UkuranTeks
import id.asisten.app.domain.pengingat.KeadaanPengingat
import id.asisten.app.domain.pengingat.ModePengingat
import id.asisten.app.domain.pengingat.PengaturanPengingat
import id.asisten.app.domain.pengingat.TundaCodec
import id.asisten.app.domain.profil.NamaPengguna
import id.asisten.app.domain.profil.ProfilPengguna
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** One DataStore per process; declared at top level as the DataStore docs require. */
val Context.penyimpananPengaturan: DataStore<Preferences> by preferencesDataStore(name = "pengaturan")

private object Kunci {
    val TEMA = stringPreferencesKey("mode_tema")
    val AKSEN = stringPreferencesKey("aksen")
    val UKURAN_TEKS = stringPreferencesKey("ukuran_teks")
    val ANIMASI = booleanPreferencesKey("animasi_aktif")
    val NAMA = stringPreferencesKey("nama")
    val ONBOARDING = booleanPreferencesKey("onboarding_selesai")
    val MEMORI_AKTIF = booleanPreferencesKey("memori_aktif")
    val TATA_LETAK = stringPreferencesKey("tata_letak_beranda")

    // Reminders (Phase 5). New keys only: existing keys and the Room schema are untouched.
    val PENGINGAT_AKTIF = booleanPreferencesKey("pengingat_aktif")
    val PENGINGAT_SEBELUM_KEGIATAN = intPreferencesKey("pengingat_menit_kegiatan")
    val PENGINGAT_TUGAS_AKTIF = booleanPreferencesKey("pengingat_tugas_aktif")
    val PENGINGAT_TUGAS_HARI_H = intPreferencesKey("pengingat_tugas_menit_h")
    val PENGINGAT_TUGAS_SEBELUM = intPreferencesKey("pengingat_tugas_menit_h1")
    val PENGINGAT_SEKOLAH_AKTIF = booleanPreferencesKey("pengingat_sekolah_aktif")
    val PENGINGAT_SEBELUM_SEKOLAH = intPreferencesKey("pengingat_menit_sekolah")
    val PENGINGAT_TUNDA_MENIT = intPreferencesKey("pengingat_tunda_menit")
    val PENGINGAT_TENANG_AKTIF = booleanPreferencesKey("pengingat_tenang_aktif")
    val PENGINGAT_TENANG_MULAI = intPreferencesKey("pengingat_tenang_mulai")
    val PENGINGAT_TENANG_SELESAI = intPreferencesKey("pengingat_tenang_selesai")
    val PENGINGAT_MODE = stringPreferencesKey("pengingat_mode")
    val PENGINGAT_MODE_SAMPAI = longPreferencesKey("pengingat_mode_sampai")
    val PENGINGAT_PROSES_TERAKHIR = longPreferencesKey("pengingat_proses_terakhir")
    val PENGINGAT_TUNDA = stringPreferencesKey("pengingat_tunda")
}

/**
 * Appearance settings, user profile and dashboard arrangement, stored in DataStore (small key-value data).
 * Larger structured records (schedule, tasks, goals, memory) live in Room.
 * Every update is a read-modify-write inside one `edit {}` so concurrent changes cannot overwrite each other.
 */
class PengaturanRepository(private val penyimpanan: DataStore<Preferences>) {

    private val data: Flow<Preferences> = penyimpanan.data.catch { galat ->
        // A corrupted/unreadable file must not crash the app; fall back to defaults.
        if (galat is IOException) emit(emptyPreferences()) else throw galat
    }

    val pengaturan: Flow<Pengaturan> = data.map { it.bacaPengaturan() }.distinctUntilChanged()

    val profil: Flow<ProfilPengguna> = data.map { it.bacaProfil() }.distinctUntilChanged()

    val riwayatTataLetak: Flow<RiwayatTataLetak> =
        data.map { TataLetakCodec.decodeRiwayat(it[Kunci.TATA_LETAK]) }.distinctUntilChanged()

    val pengingat: Flow<PengaturanPengingat> = data.map { it.bacaPengingat() }.distinctUntilChanged()

    /** Scheduler bookkeeping (last handled instant, snoozes). Kept apart from [pengingat] on purpose:
     *  the scheduler writes it, and it must not re-trigger the settings observer. */
    val keadaanPengingat: Flow<KeadaanPengingat> = data.map { it.bacaKeadaan() }.distinctUntilChanged()

    suspend fun ubahPengaturan(ubah: (Pengaturan) -> Pengaturan) {
        penyimpanan.edit { p ->
            val baru = ubah(p.bacaPengaturan())
            p[Kunci.TEMA] = baru.modeTema.name
            p[Kunci.AKSEN] = baru.aksen.name
            p[Kunci.UKURAN_TEKS] = baru.ukuranTeks.name
            p[Kunci.ANIMASI] = baru.animasiAktif
        }
    }

    suspend fun ubahProfil(ubah: (ProfilPengguna) -> ProfilPengguna) {
        penyimpanan.edit { p ->
            val baru = ubah(p.bacaProfil())
            p[Kunci.NAMA] = NamaPengguna.bersihkan(baru.nama)
            p[Kunci.ONBOARDING] = baru.onboardingSelesai
            p[Kunci.MEMORI_AKTIF] = baru.memoriAktif
        }
    }

    suspend fun ubahRiwayatTataLetak(ubah: (RiwayatTataLetak) -> RiwayatTataLetak) {
        penyimpanan.edit { p ->
            val baru = ubah(TataLetakCodec.decodeRiwayat(p[Kunci.TATA_LETAK]))
            p[Kunci.TATA_LETAK] = TataLetakCodec.encodeRiwayat(baru)
        }
    }

    suspend fun ubahPengingat(ubah: (PengaturanPengingat) -> PengaturanPengingat) {
        penyimpanan.edit { p ->
            val baru = ubah(p.bacaPengingat()).normalisasi()
            p[Kunci.PENGINGAT_AKTIF] = baru.aktif
            p[Kunci.PENGINGAT_SEBELUM_KEGIATAN] = baru.menitSebelumKegiatan
            p[Kunci.PENGINGAT_TUGAS_AKTIF] = baru.tugasAktif
            p[Kunci.PENGINGAT_TUGAS_HARI_H] = baru.menitTugasHariH
            p[Kunci.PENGINGAT_TUGAS_SEBELUM] = baru.menitTugasSebelum
            p[Kunci.PENGINGAT_SEKOLAH_AKTIF] = baru.sekolahAktif
            p[Kunci.PENGINGAT_SEBELUM_SEKOLAH] = baru.menitSebelumSekolah
            p[Kunci.PENGINGAT_TUNDA_MENIT] = baru.tundaMenit
            p[Kunci.PENGINGAT_TENANG_AKTIF] = baru.jamTenangAktif
            p[Kunci.PENGINGAT_TENANG_MULAI] = baru.jamTenangMulai
            p[Kunci.PENGINGAT_TENANG_SELESAI] = baru.jamTenangSelesai
            p[Kunci.PENGINGAT_MODE] = baru.mode.name
            p[Kunci.PENGINGAT_MODE_SAMPAI] = baru.modeSampai
        }
    }

    /** Read-modify-write of the scheduler state; returns the state that was stored. */
    suspend fun ubahKeadaanPengingat(ubah: (KeadaanPengingat) -> KeadaanPengingat): KeadaanPengingat {
        var hasil = KeadaanPengingat()
        penyimpanan.edit { p ->
            hasil = ubah(p.bacaKeadaan())
            p[Kunci.PENGINGAT_PROSES_TERAKHIR] = hasil.prosesTerakhir
            p[Kunci.PENGINGAT_TUNDA] = TundaCodec.encode(hasil.tunda)
        }
        return hasil
    }

    private fun Preferences.bacaPengingat(): PengaturanPengingat {
        val bawaan = PengaturanPengingat()
        return PengaturanPengingat(
            aktif = this[Kunci.PENGINGAT_AKTIF] ?: bawaan.aktif,
            menitSebelumKegiatan = this[Kunci.PENGINGAT_SEBELUM_KEGIATAN] ?: bawaan.menitSebelumKegiatan,
            tugasAktif = this[Kunci.PENGINGAT_TUGAS_AKTIF] ?: bawaan.tugasAktif,
            menitTugasHariH = this[Kunci.PENGINGAT_TUGAS_HARI_H] ?: bawaan.menitTugasHariH,
            menitTugasSebelum = this[Kunci.PENGINGAT_TUGAS_SEBELUM] ?: bawaan.menitTugasSebelum,
            sekolahAktif = this[Kunci.PENGINGAT_SEKOLAH_AKTIF] ?: bawaan.sekolahAktif,
            menitSebelumSekolah = this[Kunci.PENGINGAT_SEBELUM_SEKOLAH] ?: bawaan.menitSebelumSekolah,
            tundaMenit = this[Kunci.PENGINGAT_TUNDA_MENIT] ?: bawaan.tundaMenit,
            jamTenangAktif = this[Kunci.PENGINGAT_TENANG_AKTIF] ?: bawaan.jamTenangAktif,
            jamTenangMulai = this[Kunci.PENGINGAT_TENANG_MULAI] ?: bawaan.jamTenangMulai,
            jamTenangSelesai = this[Kunci.PENGINGAT_TENANG_SELESAI] ?: bawaan.jamTenangSelesai,
            mode = this[Kunci.PENGINGAT_MODE].keEnum(ModePengingat.NORMAL),
            modeSampai = this[Kunci.PENGINGAT_MODE_SAMPAI] ?: 0L,
        ).normalisasi()
    }

    private fun Preferences.bacaKeadaan() = KeadaanPengingat(
        prosesTerakhir = this[Kunci.PENGINGAT_PROSES_TERAKHIR] ?: 0L,
        tunda = TundaCodec.decode(this[Kunci.PENGINGAT_TUNDA]),
    )

    private fun Preferences.bacaPengaturan() = Pengaturan(
        modeTema = this[Kunci.TEMA].keEnum(ModeTema.SISTEM),
        aksen = this[Kunci.AKSEN].keEnum(Aksen.JINGGA),
        ukuranTeks = this[Kunci.UKURAN_TEKS].keEnum(UkuranTeks.NORMAL),
        animasiAktif = this[Kunci.ANIMASI] ?: true,
    )

    private fun Preferences.bacaProfil() = ProfilPengguna(
        nama = this[Kunci.NAMA] ?: "",
        onboardingSelesai = this[Kunci.ONBOARDING] ?: false,
        memoriAktif = this[Kunci.MEMORI_AKTIF] ?: true,
    )
}
