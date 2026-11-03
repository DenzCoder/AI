package id.asisten.app.ui.aktivitas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.asisten.app.data.repo.AktivitasRepository
import id.asisten.app.domain.aktivitas.Tonggak
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.aktivitas.Tujuan
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AktivitasViewModel(private val repo: AktivitasRepository) : ViewModel() {

    val tugas: StateFlow<List<Tugas>> =
        repo.tugas.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tujuan: StateFlow<List<Tujuan>> =
        repo.tujuan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun simpanTugas(tugas: Tugas) {
        viewModelScope.launch { repo.simpanTugas(tugas) }
    }

    fun setTugasSelesai(tugas: Tugas, selesai: Boolean) {
        viewModelScope.launch { repo.setTugasSelesai(tugas, selesai) }
    }

    fun hapusTugas(id: Long) {
        viewModelScope.launch { repo.hapusTugas(id) }
    }

    fun simpanTujuan(tujuan: Tujuan) {
        viewModelScope.launch { repo.simpanTujuan(tujuan) }
    }

    fun setTujuanSelesai(tujuan: Tujuan, selesai: Boolean) {
        viewModelScope.launch { repo.setTujuanSelesai(tujuan, selesai) }
    }

    fun hapusTujuan(id: Long) {
        viewModelScope.launch { repo.hapusTujuan(id) }
    }

    fun tambahTonggak(tonggak: Tonggak) {
        viewModelScope.launch { repo.tambahTonggak(tonggak) }
    }

    fun setTonggakSelesai(tonggak: Tonggak, selesai: Boolean) {
        viewModelScope.launch { repo.setTonggakSelesai(tonggak, selesai) }
    }

    fun hapusTonggak(id: Long) {
        viewModelScope.launch { repo.hapusTonggak(id) }
    }
}
