package id.asisten.app.ui.jadwal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.asisten.app.data.repo.AktivitasRepository
import id.asisten.app.data.repo.JadwalRepository
import id.asisten.app.domain.aktivitas.Tugas
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.ModeKalender
import id.asisten.app.domain.jadwal.NavigasiKalender
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.Piket
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Everything the calendar needs to draw a day, a week or a month. */
class DataJadwalUi(
    val kegiatan: List<Kegiatan> = emptyList(),
    val pelajaran: List<Pelajaran> = emptyList(),
    val jamSekolah: List<JamSekolah> = emptyList(),
    val piket: List<Piket> = emptyList(),
    val tugas: List<Tugas> = emptyList(),
)

class JadwalViewModel(
    private val jadwal: JadwalRepository,
    aktivitas: AktivitasRepository,
) : ViewModel() {

    private val _tanggal = MutableStateFlow(LocalDate.now())
    private val _mode = MutableStateFlow(ModeKalender.HARI)

    val tanggal: StateFlow<LocalDate> = _tanggal.asStateFlow()
    val mode: StateFlow<ModeKalender> = _mode.asStateFlow()

    val data: StateFlow<DataJadwalUi> = combine(
        jadwal.kegiatan, jadwal.pelajaran, jadwal.jamSekolah, jadwal.piket, aktivitas.tugas,
    ) { k, p, j, pk, t -> DataJadwalUi(k, p, j, pk, t) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DataJadwalUi())

    fun pilihMode(mode: ModeKalender) {
        _mode.value = mode
    }

    fun geser(arah: Int) {
        _tanggal.value = NavigasiKalender.geser(_tanggal.value, _mode.value, arah)
    }

    fun keHariIni() {
        _tanggal.value = LocalDate.now()
    }

    /** Opens the day view for [tanggal] (used when tapping a day in the week or month view). */
    fun bukaHari(tanggal: LocalDate) {
        _tanggal.value = tanggal
        _mode.value = ModeKalender.HARI
    }

    fun simpanKegiatan(kegiatan: Kegiatan) {
        viewModelScope.launch { jadwal.simpanKegiatan(kegiatan) }
    }

    fun hapusKegiatan(id: Long) {
        viewModelScope.launch { jadwal.hapusKegiatan(id) }
    }
}
