package id.asisten.app.ui.pengaturan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.asisten.app.data.pengaturan.PengaturanRepository
import id.asisten.app.domain.pengaturan.Aksen
import id.asisten.app.domain.pengaturan.ModeTema
import id.asisten.app.domain.pengaturan.Pengaturan
import id.asisten.app.domain.pengaturan.UkuranTeks
import id.asisten.app.domain.profil.NamaPengguna
import id.asisten.app.domain.profil.ProfilPengguna
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Appearance settings and the user profile, persisted through [PengaturanRepository].
 * Both flows are null until the first read finishes, so the UI never flashes default values
 * (wrong theme, or the onboarding screen for a returning user).
 */
class PengaturanViewModel(private val repo: PengaturanRepository) : ViewModel() {

    private val aliranPengaturan: Flow<Pengaturan?> = repo.pengaturan
    private val aliranProfil: Flow<ProfilPengguna?> = repo.profil

    val pengaturan: StateFlow<Pengaturan?> =
        aliranPengaturan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val profil: StateFlow<ProfilPengguna?> =
        aliranProfil.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun ubahModeTema(mode: ModeTema) {
        viewModelScope.launch { repo.ubahPengaturan { it.copy(modeTema = mode) } }
    }

    fun ubahAksen(aksen: Aksen) {
        viewModelScope.launch { repo.ubahPengaturan { it.copy(aksen = aksen) } }
    }

    fun ubahUkuranTeks(ukuran: UkuranTeks) {
        viewModelScope.launch { repo.ubahPengaturan { it.copy(ukuranTeks = ukuran) } }
    }

    fun ubahAnimasi(aktif: Boolean) {
        viewModelScope.launch { repo.ubahPengaturan { it.copy(animasiAktif = aktif) } }
    }

    fun simpanNama(nama: String) {
        if (!NamaPengguna.valid(nama)) return
        viewModelScope.launch { repo.ubahProfil { it.copy(nama = NamaPengguna.bersihkan(nama)) } }
    }

    fun ubahMemoriAktif(aktif: Boolean) {
        viewModelScope.launch { repo.ubahProfil { it.copy(memoriAktif = aktif) } }
    }

    /** Saves the onboarding answers in one write and marks onboarding as done. */
    fun selesaikanOnboarding(nama: String, memoriAktif: Boolean) {
        if (!NamaPengguna.valid(nama)) return
        viewModelScope.launch {
            repo.ubahProfil {
                it.copy(nama = NamaPengguna.bersihkan(nama), memoriAktif = memoriAktif, onboardingSelesai = true)
            }
        }
    }
}
