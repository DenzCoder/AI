package id.asisten.app.ui.pengingat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.asisten.app.data.pengaturan.PengaturanRepository
import id.asisten.app.domain.pengingat.LabelPengingat
import id.asisten.app.domain.pengingat.ModePengingat
import id.asisten.app.domain.pengingat.PengaturanPengingat
import id.asisten.app.domain.pengingat.PengingatAkanDatang
import id.asisten.app.domain.pengingat.PenyusunNotifikasi
import id.asisten.app.notifikasi.PenjadwalPengingat
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PengingatViewModel(
    private val pengaturanRepo: PengaturanRepository,
    private val penjadwal: PenjadwalPengingat,
) : ViewModel() {

    /** Null until the first read from storage, so the screen never flashes default values. */
    val pengaturan: StateFlow<PengaturanPengingat?> =
        pengaturanRepo.pengingat.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The next reminders that will really fire, computed from the current data and settings. */
    val berikutnya: StateFlow<List<PengingatAkanDatang>> = combine(
        penjadwal.dataPengingat,
        pengaturanRepo.pengingat,
        pengaturanRepo.keadaanPengingat,
    ) { data, p, keadaan ->
        PenyusunNotifikasi.akanDatang(data, keadaan.tunda, p, System.currentTimeMillis(), ZoneId.systemDefault())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _hasilUji = MutableStateFlow<String?>(null)
    val hasilUji: StateFlow<String?> = _hasilUji

    fun ubah(transformasi: (PengaturanPengingat) -> PengaturanPengingat) {
        viewModelScope.launch { pengaturanRepo.ubahPengingat(transformasi) }
    }

    fun mulaiMode(mode: ModePengingat, durasiMenit: Int) {
        ubah { it.mulaiMode(mode, durasiMenit, System.currentTimeMillis()) }
    }

    fun akhiriMode() {
        ubah { it.akhiriMode() }
    }

    fun kirimUji(notifikasiDiizinkan: Boolean) {
        viewModelScope.launch {
            _hasilUji.value =
                if (notifikasiDiizinkan) LabelPengingat.hasilUji(penjadwal.kirimUji()) else LabelPengingat.UJI_DITOLAK
        }
    }
}
