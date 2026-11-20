package id.asisten.app.data.pengingat

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import id.asisten.app.domain.pengingat.PengaturanSuaraPengingat
import id.asisten.app.domain.pengingat.SuaraPengingat
import id.asisten.app.domain.pengingat.SuaraPengingatCodec
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private object KunciSuaraPengingat {
    val SUARA = stringPreferencesKey("pengingat_suara")
    val VOLUME_ALARM = booleanPreferencesKey("pengingat_suara_volume_alarm")
}

/**
 * The sound of the reminder notification, in the shared DataStore (new keys only). It is kept apart from
 * the other reminder settings on purpose: changing the sound must not re-arm the alarms.
 */
class SuaraPengingatRepository(private val penyimpanan: DataStore<Preferences>) {

    private val data: Flow<Preferences> = penyimpanan.data.catch { galat ->
        if (galat is IOException) emit(emptyPreferences()) else throw galat
    }

    val pengaturan: Flow<PengaturanSuaraPengingat> = data.map { p ->
        PengaturanSuaraPengingat(
            suara = SuaraPengingatCodec.decode(p[KunciSuaraPengingat.SUARA]),
            volumeAlarm = p[KunciSuaraPengingat.VOLUME_ALARM] ?: false,
        )
    }.distinctUntilChanged()

    suspend fun ubahSuara(suara: SuaraPengingat) {
        penyimpanan.edit { it[KunciSuaraPengingat.SUARA] = SuaraPengingatCodec.encode(suara) }
    }

    suspend fun ubahVolumeAlarm(pakai: Boolean) {
        penyimpanan.edit { it[KunciSuaraPengingat.VOLUME_ALARM] = pakai }
    }
}
