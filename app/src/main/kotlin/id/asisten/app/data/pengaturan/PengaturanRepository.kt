package id.asisten.app.data.pengaturan

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import id.asisten.app.domain.dasbor.RiwayatTataLetak
import id.asisten.app.domain.dasbor.TataLetakCodec
import id.asisten.app.domain.keEnum
import id.asisten.app.domain.pengaturan.Aksen
import id.asisten.app.domain.pengaturan.ModeTema
import id.asisten.app.domain.pengaturan.Pengaturan
import id.asisten.app.domain.pengaturan.UkuranTeks
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
