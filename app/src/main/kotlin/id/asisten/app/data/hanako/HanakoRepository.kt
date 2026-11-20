package id.asisten.app.data.hanako

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import id.asisten.app.domain.hanako.KunciAi
import id.asisten.app.domain.hanako.NamaModel
import id.asisten.app.domain.hanako.PengaturanHanako
import id.asisten.app.domain.hanako.PenyediaAi
import id.asisten.app.domain.hanako.PesanChat
import id.asisten.app.domain.hanako.RiwayatChat
import id.asisten.app.domain.hanako.RiwayatChatCodec
import id.asisten.app.domain.hanako.RiwayatUsulan
import id.asisten.app.domain.hanako.UsulanMentah
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
    val AKSI_AKTIF = booleanPreferencesKey("hanako_aksi_aktif")
    val USULAN = stringPreferencesKey("hanako_usulan")
    val PENYEDIA = stringPreferencesKey("hanako_penyedia")

    /**
     * Gemini keeps the original key names (so an existing key and model survive the update); the other services
     * get their own, so each service has its own key and its own model name.
     */
    fun kunci(p: PenyediaAi): Preferences.Key<String> =
        if (p == PenyediaAi.GEMINI) KUNCI_TERENKRIPSI else stringPreferencesKey("hanako_kunci_terenkripsi_${p.kode}")

    fun model(p: PenyediaAi): Preferences.Key<String> =
        if (p == PenyediaAi.GEMINI) MODEL else stringPreferencesKey("hanako_model_${p.kode}")
}

/**
 * Hanako chat settings, the encrypted API keys (one per AI service) and the chat history, in the same DataStore as
 * the other settings (new keys only; Room is untouched). A key is exposed only through [kunci], on demand and
 * decrypted.
 */
class HanakoRepository(
    private val penyimpanan: DataStore<Preferences>,
    private val pengacak: Pengacak,
) {
    private val data: Flow<Preferences> = penyimpanan.data.catch { galat ->
        if (galat is IOException) emit(emptyPreferences()) else throw galat
    }

    val pengaturan: Flow<PengaturanHanako> = data.map { p ->
        val penyedia = PenyediaAi.dariKode(p[KunciHanako.PENYEDIA])
        PengaturanHanako(
            onlineAktif = p[KunciHanako.ONLINE_AKTIF] ?: false,
            bagikanData = p[KunciHanako.BAGIKAN_DATA] ?: true,
            penyedia = penyedia,
            model = p[KunciHanako.model(penyedia)]?.takeIf { NamaModel.sah(it, penyedia) } ?: penyedia.modelBawaan,
            adaKunci = !p[KunciHanako.kunci(penyedia)].isNullOrEmpty(),
            aksiAktif = p[KunciHanako.AKSI_AKTIF] ?: true,
            penyediaBerkunci = PenyediaAi.entries.filter { !p[KunciHanako.kunci(it)].isNullOrEmpty() }.toSet(),
        )
    }.distinctUntilChanged()

    val riwayat: Flow<List<PesanChat>> =
        data.map { RiwayatChatCodec.decode(it[KunciHanako.RIWAYAT]) }.distinctUntilChanged()

    /** Proposals waiting for the user's decision (raw; the view model checks them against the current data). */
    val usulan: Flow<List<UsulanMentah>> =
        data.map { RiwayatUsulan.decode(it[KunciHanako.USULAN]) }.distinctUntilChanged()

    /** The decrypted key of [penyedia], or null when none is stored or it cannot be read (then it must be entered again). */
    suspend fun kunci(penyedia: PenyediaAi): String? {
        val tersimpan = data.first()[KunciHanako.kunci(penyedia)]
        if (tersimpan.isNullOrEmpty()) return null
        return withContext(Dispatchers.IO) { pengacak.buka(tersimpan) }
    }

    /**
     * Cleans, checks and stores the key of [penyedia] encrypted, makes it the service in use and switches online
     * mode on. Returns false (and stores nothing) when the key looks wrong or this phone cannot encrypt it: it is
     * never stored as plain text.
     */
    suspend fun simpanKunci(penyedia: PenyediaAi, mentah: String): Boolean {
        val bersih = KunciAi.bersihkan(mentah)
        if (!KunciAi.masukAkal(bersih)) return false
        val terkode = withContext(Dispatchers.IO) { pengacak.acak(bersih) } ?: return false
        penyimpanan.edit { p ->
            p[KunciHanako.kunci(penyedia)] = terkode
            p[KunciHanako.PENYEDIA] = penyedia.kode
            p[KunciHanako.ONLINE_AKTIF] = true
        }
        return true
    }

    /** Online mode is switched off only when the removed key belongs to the service in use. */
    suspend fun hapusKunci(penyedia: PenyediaAi) {
        penyimpanan.edit { p ->
            p.remove(KunciHanako.kunci(penyedia))
            if (PenyediaAi.dariKode(p[KunciHanako.PENYEDIA]) == penyedia) p[KunciHanako.ONLINE_AKTIF] = false
        }
    }

    /** Picks the service in use. Keys and models of the other services stay stored. */
    suspend fun ubahPenyedia(penyedia: PenyediaAi) {
        penyimpanan.edit { it[KunciHanako.PENYEDIA] = penyedia.kode }
    }

    suspend fun ubahOnline(aktif: Boolean) {
        penyimpanan.edit { it[KunciHanako.ONLINE_AKTIF] = aktif }
    }

    suspend fun ubahAksi(aktif: Boolean) {
        penyimpanan.edit { it[KunciHanako.AKSI_AKTIF] = aktif }
    }

    suspend fun ubahBagikanData(bagikan: Boolean) {
        penyimpanan.edit { it[KunciHanako.BAGIKAN_DATA] = bagikan }
    }

    /** An invalid name is replaced by the service's default rather than stored. */
    suspend fun ubahModel(penyedia: PenyediaAi, mentah: String) {
        val nama = NamaModel.bersihkan(mentah, penyedia).takeIf { NamaModel.sah(it, penyedia) } ?: penyedia.modelBawaan
        penyimpanan.edit { it[KunciHanako.model(penyedia)] = nama }
    }

    /** Read-modify-write inside one `edit {}` so two quick messages cannot overwrite each other. */
    suspend fun tambahPesan(pesan: PesanChat) {
        penyimpanan.edit { p ->
            val lama = RiwayatChatCodec.decode(p[KunciHanako.RIWAYAT])
            p[KunciHanako.RIWAYAT] = RiwayatChatCodec.encode(RiwayatChat.tambah(lama, pesan))
        }
    }

    /** Also drops old proposals. One `edit {}`, so it cannot race with [hapusUsulan]. */
    suspend fun tambahUsulan(baru: List<UsulanMentah>, sekarang: Long) {
        if (baru.isEmpty()) return
        penyimpanan.edit { p ->
            val lama = RiwayatUsulan.segar(RiwayatUsulan.decode(p[KunciHanako.USULAN]), sekarang)
            p[KunciHanako.USULAN] = RiwayatUsulan.encode(RiwayatUsulan.tambah(lama, baru))
        }
    }

    suspend fun hapusUsulan(id: Long) {
        penyimpanan.edit { p ->
            val sisa = RiwayatUsulan.hapus(RiwayatUsulan.decode(p[KunciHanako.USULAN]), id)
            p.remove(KunciHanako.USULAN)
            if (sisa.isNotEmpty()) p[KunciHanako.USULAN] = RiwayatUsulan.encode(sisa)
        }
    }

    /** Clearing the conversation also clears the proposals that belong to it. */
    suspend fun hapusRiwayat() {
        penyimpanan.edit {
            it.remove(KunciHanako.RIWAYAT)
            it.remove(KunciHanako.USULAN)
        }
    }
}
