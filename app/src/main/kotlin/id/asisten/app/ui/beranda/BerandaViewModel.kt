package id.asisten.app.ui.beranda

import androidx.lifecycle.ViewModel
import id.asisten.app.domain.Sapaan
import id.asisten.app.domain.SapaanProvider
import id.asisten.app.domain.dasbor.RiwayatTataLetak
import id.asisten.app.domain.dasbor.WidgetBeranda
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Beranda state. The widget arrangement and its undo history live in memory only;
 * saving arrangements permanently arrives with the database in Phase 3.
 */
class BerandaViewModel : ViewModel() {

    private val _sapaan = MutableStateFlow(SapaanProvider.untuk(LocalTime.now()))
    val sapaan: StateFlow<Sapaan> = _sapaan.asStateFlow()

    private val _riwayat = MutableStateFlow(RiwayatTataLetak())
    val riwayat: StateFlow<RiwayatTataLetak> = _riwayat.asStateFlow()

    private val _mengatur = MutableStateFlow(false)
    val mengatur: StateFlow<Boolean> = _mengatur.asStateFlow()

    /** Recomputes the greeting, called when the screen returns to the foreground. */
    fun segarkan() {
        _sapaan.value = SapaanProvider.untuk(LocalTime.now())
    }

    fun mulaiMengatur() {
        _mengatur.value = true
    }

    fun selesaiMengatur() {
        _mengatur.value = false
    }

    fun pindah(widget: WidgetBeranda, arah: Int) =
        _riwayat.update { it.terapkan(it.sekarang.pindah(widget, arah)) }

    fun aturTampil(widget: WidgetBeranda, tampil: Boolean) = _riwayat.update {
        it.terapkan(if (tampil) it.sekarang.tampilkan(widget) else it.sekarang.sembunyikan(widget))
    }

    fun urungkan() = _riwayat.update { it.urungkan() }

    fun kembalikanStandar() = _riwayat.update { it.kembalikanStandar() }
}
