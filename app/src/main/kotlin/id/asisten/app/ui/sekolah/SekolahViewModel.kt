package id.asisten.app.ui.sekolah

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.asisten.app.data.repo.JadwalRepository
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.Piket
import java.time.DayOfWeek
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DataSekolahUi(
    val jamSekolah: List<JamSekolah> = emptyList(),
    val pelajaran: List<Pelajaran> = emptyList(),
    val piket: List<Piket> = emptyList(),
)

class SekolahViewModel(private val jadwal: JadwalRepository) : ViewModel() {

    val data: StateFlow<DataSekolahUi> = combine(
        jadwal.jamSekolah, jadwal.pelajaran, jadwal.piket,
    ) { j, p, pk -> DataSekolahUi(j, p, pk) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataSekolahUi())

    fun simpanJam(jam: JamSekolah) {
        viewModelScope.launch { jadwal.simpanJamSekolah(jam) }
    }

    fun hapusJam(hari: DayOfWeek) {
        viewModelScope.launch { jadwal.hapusJamSekolah(hari) }
    }

    fun simpanPelajaran(pelajaran: Pelajaran) {
        viewModelScope.launch { jadwal.simpanPelajaran(pelajaran) }
    }

    fun hapusPelajaran(id: Long) {
        viewModelScope.launch { jadwal.hapusPelajaran(id) }
    }

    fun tambahPiket(piket: Piket) {
        viewModelScope.launch { jadwal.tambahPiket(piket) }
    }

    fun hapusPiket(id: Long) {
        viewModelScope.launch { jadwal.hapusPiket(id) }
    }
}
