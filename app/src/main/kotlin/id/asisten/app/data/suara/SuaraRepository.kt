package id.asisten.app.data.suara

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import id.asisten.app.domain.suara.PengaturanSuara
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private object KunciSuara {
    val BACA_OTOMATIS = booleanPreferencesKey("suara_baca_otomatis")
    val KECEPATAN = floatPreferencesKey("suara_kecepatan")
    val NADA = floatPreferencesKey("suara_nada")
}

/** Voice settings in the shared DataStore (new keys only). Stored values are normalised when read. */
class SuaraRepository(private val penyimpanan: DataStore<Preferences>) {

    val pengaturan: Flow<PengaturanSuara> = penyimpanan.data
        .catch { galat -> if (galat is IOException) emit(emptyPreferences()) else throw galat }
        .map { p ->
            PengaturanSuara(
                bacaOtomatis = p[KunciSuara.BACA_OTOMATIS] ?: false,
                kecepatan = p[KunciSuara.KECEPATAN] ?: 1.0f,
                nada = p[KunciSuara.NADA] ?: 1.0f,
            ).normalisasi()
        }
        .distinctUntilChanged()

    suspend fun ubahBacaOtomatis(aktif: Boolean) {
        penyimpanan.edit { it[KunciSuara.BACA_OTOMATIS] = aktif }
    }

    suspend fun ubahKecepatan(nilai: Float) {
        penyimpanan.edit { it[KunciSuara.KECEPATAN] = PengaturanSuara(kecepatan = nilai).normalisasi().kecepatan }
    }

    suspend fun ubahNada(nilai: Float) {
        penyimpanan.edit { it[KunciSuara.NADA] = PengaturanSuara(nada = nilai).normalisasi().nada }
    }
}
