package id.asisten.app.ui.pengaturan

import androidx.lifecycle.ViewModel
import id.asisten.app.domain.pengaturan.Aksen
import id.asisten.app.domain.pengaturan.ModeTema
import id.asisten.app.domain.pengaturan.Pengaturan
import id.asisten.app.domain.pengaturan.UkuranTeks
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Holds appearance settings for the current process only.
 * Persistence to DataStore is added in Phase 3 (Local Database).
 */
class PengaturanViewModel : ViewModel() {

    private val _pengaturan = MutableStateFlow(Pengaturan())
    val pengaturan: StateFlow<Pengaturan> = _pengaturan.asStateFlow()

    fun ubahModeTema(mode: ModeTema) = _pengaturan.update { it.copy(modeTema = mode) }

    fun ubahAksen(aksen: Aksen) = _pengaturan.update { it.copy(aksen = aksen) }

    fun ubahUkuranTeks(ukuran: UkuranTeks) = _pengaturan.update { it.copy(ukuranTeks = ukuran) }

    fun ubahAnimasi(aktif: Boolean) = _pengaturan.update { it.copy(animasiAktif = aktif) }
}
