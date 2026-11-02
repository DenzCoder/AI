package id.asisten.app.ui.beranda

import androidx.lifecycle.ViewModel
import id.asisten.app.domain.Sapaan
import id.asisten.app.domain.SapaanProvider
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BerandaViewModel : ViewModel() {

    private val _sapaan = MutableStateFlow(SapaanProvider.untuk(LocalTime.now()))
    val sapaan: StateFlow<Sapaan> = _sapaan.asStateFlow()

    /** Recomputes the greeting, called when the screen returns to the foreground. */
    fun segarkan() {
        _sapaan.value = SapaanProvider.untuk(LocalTime.now())
    }
}
