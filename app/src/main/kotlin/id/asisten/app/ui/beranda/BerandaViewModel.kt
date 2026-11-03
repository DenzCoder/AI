package id.asisten.app.ui.beranda

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.asisten.app.data.pengaturan.PengaturanRepository
import id.asisten.app.data.repo.AktivitasRepository
import id.asisten.app.data.repo.JadwalRepository
import id.asisten.app.domain.Sapaan
import id.asisten.app.domain.SapaanProvider
import id.asisten.app.domain.dasbor.PenyusunBeranda
import id.asisten.app.domain.dasbor.RingkasanBeranda
import id.asisten.app.domain.dasbor.RiwayatTataLetak
import id.asisten.app.domain.dasbor.WidgetBeranda
import id.asisten.app.domain.jadwal.JamSekolah
import id.asisten.app.domain.jadwal.Kegiatan
import id.asisten.app.domain.jadwal.Pelajaran
import id.asisten.app.domain.jadwal.Piket
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private class DataJadwal(
    val kegiatan: List<Kegiatan>,
    val pelajaran: List<Pelajaran>,
    val jamSekolah: List<JamSekolah>,
    val piket: List<Piket>,
)

/** Beranda state: everything shown comes from recorded data; the arrangement is saved in DataStore. */
class BerandaViewModel(
    private val pengaturan: PengaturanRepository,
    jadwal: JadwalRepository,
    aktivitas: AktivitasRepository,
) : ViewModel() {

    private val _hariIni = MutableStateFlow(LocalDate.now())
    private val _sapaan = MutableStateFlow(SapaanProvider.untuk(LocalTime.now()))
    private val _mengatur = MutableStateFlow(false)

    val sapaan: StateFlow<Sapaan> = _sapaan.asStateFlow()
    val mengatur: StateFlow<Boolean> = _mengatur.asStateFlow()

    val nama: StateFlow<String> =
        pengaturan.profil.map { it.nama }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val riwayat: StateFlow<RiwayatTataLetak> =
        pengaturan.riwayatTataLetak.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RiwayatTataLetak())

    private val dataJadwal = combine(
        jadwal.kegiatan, jadwal.pelajaran, jadwal.jamSekolah, jadwal.piket,
    ) { k, p, j, pk -> DataJadwal(k, p, j, pk) }

    val ringkasan: StateFlow<RingkasanBeranda> = combine(
        _hariIni, dataJadwal, aktivitas.tugas, aktivitas.tujuan,
    ) { hari, d, tugas, tujuan ->
        PenyusunBeranda.susun(hari, d.kegiatan, d.pelajaran, d.jamSekolah, d.piket, tugas, tujuan)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RingkasanBeranda(hariIni = LocalDate.now()))

    /** Recomputes the greeting and today's date, called when the screen returns to the foreground. */
    fun segarkan() {
        _hariIni.value = LocalDate.now()
        _sapaan.value = SapaanProvider.untuk(LocalTime.now())
    }

    fun mulaiMengatur() {
        _mengatur.value = true
    }

    fun selesaiMengatur() {
        _mengatur.value = false
    }

    fun pindah(widget: WidgetBeranda, arah: Int) {
        ubah { it.terapkan(it.sekarang.pindah(widget, arah)) }
    }

    fun aturTampil(widget: WidgetBeranda, tampil: Boolean) {
        ubah {
            it.terapkan(if (tampil) it.sekarang.tampilkan(widget) else it.sekarang.sembunyikan(widget))
        }
    }

    fun urungkan() {
        ubah { it.urungkan() }
    }

    fun kembalikanStandar() {
        ubah { it.kembalikanStandar() }
    }

    private fun ubah(blok: (RiwayatTataLetak) -> RiwayatTataLetak) {
        viewModelScope.launch { pengaturan.ubahRiwayatTataLetak(blok) }
    }
}
