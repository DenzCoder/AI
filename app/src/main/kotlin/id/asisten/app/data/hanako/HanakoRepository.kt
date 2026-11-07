package id.asisten.app.data.hanako

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import id.asisten.app.domain.hanako.KunciGemini
import id.asisten.app.domain.hanako.NamaModel
import id.asisten.app.domain.hanako.PengaturanHanako
import id.asisten.app.domain.hanako.PesanChat
import id.asisten.app.domain.hanako.RiwayatChat
import id.asisten.app.domain.hanako.RiwayatChatCodec
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private object KunciHanako {
    val ONLINE_AKTIF = booleanPreferencesKey("hanako_online_aktif")
    val BAGIKAN_DATA = booleanPreferencesKey("hanako_bagikan_data")
    val MODEL = stringPreferencesKey("hanako_model")
    val KUNCI_TERENKRIPSI = stringPreferencesKey("hanako_kunci_terenkripsi")
    val RIWAYAT = stringPreferencesKey("hanako_riwayat")
}

/**
 * Hanako chat settings, the encrypted Gemini key and the chat history, in the same DataStore as the other
 * settings (new keys only; Room is untouched). The key is exposed only through [kunci], on demand and decrypted.
 */
class HanakoRepository(
    private val penyimpanan: DataStore<Preferences>,
    private val pengacak: Pengacak,
) {
    private val data: Flow<Preferences> = penyimpanan.data.catch { galat ->
        if (galat is IOException) emit(emptyPreferences()) else throw galat
    }

    val pengaturan: Flow<PengaturanHanako> = data.map { p ->
        PengaturanHanako(
            onlineAktif = p[KunciHanako.ONLINE_AKTIF] ?: false,
            bagikanData = p[KunciHanako.BAGIKAN_DATA] ?: true,
            model = p[KunciHanako.MODEL]?.takeIf { NamaModel.sah(it) } ?: NamaModel.BAWAAN,
            adaKunci = !p[KunciHanako.KUNCI_TERENKRIPSI].isNullOrEmpty(),
        )
    }.distinctUntilChanged()

    val riwayat: Flow<List<PesanChat>> =
        data.map { RiwayatChatCodec.decode(it[KunciHanako.RIWAYAT]) }.distinctUntilChanged()

    /** The decrypted key, or null when none is stored or it cannot be read (then the user must enter it again). */
    suspend fun kunci(): String? {
        val tersimpan = data.first()[KunciHanako.KUNCI_TERENKRIPSI]
        if (tersimpan.isNullOrEmpty()) return null
        return withContext(Dispatchers.IO) { pengacak.buka(tersimpan) }
    }

    /**
     * Cleans, checks and stores the key encrypted, and switches online mode on. Returns false (and stores nothing)
     * when the key looks wrong or this phone cannot encrypt it: it is never stored as plain text.
     */
    suspend fun simpanKunci(mentah: String): Boolean {
        val bersih = KunciGemini.bersihkan(mentah)
        if (!KunciGemini.masukAkal(bersih)) return false
        val terkode = withContext(Dispatchers.IO) { pengacak.acak(bersih) } ?: return false
        penyimpanan.edit { p ->
            p[KunciHanako.KUNCI_TERENKRIPSI] = terkode
            p[KunciHanako.ONLINE_AKTIF] = true
        }
        return true
    }

    suspend fun hapusKunci() {
        penyimpanan.edit { p ->
            p.remove(KunciHanako.KUNCI_TERENKRIPSI)
            p[KunciHanako.ONLINE_AKTIF] = false
        }
    }

    suspend fun ubahOnline(aktif: Boolean) {
        penyimpanan.edit { it[KunciHanako.ONLINE_AKTIF] = aktif }
    }

    suspend fun ubahBagikanData(bagikan: Boolean) {
        penyimpanan.edit { it[KunciHanako.BAGIKAN_DATA] = bagikan }
    }

    /** An invalid name is replaced by the default rather than stored. */
    suspend fun ubahModel(mentah: String) {
        val nama = NamaModel.bersihkan(mentah).takeIf { NamaModel.sah(it) } ?: NamaModel.BAWAAN
        penyimpanan.edit { it[KunciHanako.MODEL] = nama }
    }

    /** Read-modify-write inside one `edit {}` so two quick messages cannot overwrite each other. */
    suspend fun tambahPesan(pesan: PesanChat) {
        penyimpanan.edit { p ->
            val lama = RiwayatChatCodec.decode(p[KunciHanako.RIWAYAT])
            p[KunciHanako.RIWAYAT] = RiwayatChatCodec.encode(RiwayatChat.tambah(lama, pesan))
        }
    }

    suspend fun hapusRiwayat() {
        penyimpanan.edit { it.remove(KunciHanako.RIWAYAT) }
    }
}
