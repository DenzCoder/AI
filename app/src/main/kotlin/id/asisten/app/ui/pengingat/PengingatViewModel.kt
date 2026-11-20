package id.asisten.app.ui.pengingat

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.asisten.app.data.pengaturan.PengaturanRepository
import id.asisten.app.data.pengingat.SuaraPengingatRepository
import id.asisten.app.domain.pengingat.LabelPengingat
import id.asisten.app.domain.pengingat.ModePengingat
import id.asisten.app.domain.pengingat.LabelSuaraPengingat
import id.asisten.app.domain.pengingat.PengaturanPengingat
import id.asisten.app.domain.pengingat.PengaturanSuaraPengingat
import id.asisten.app.domain.pengingat.PengingatAkanDatang
import id.asisten.app.domain.pengingat.PenyusunNotifikasi
import id.asisten.app.domain.pengingat.SuaraPengingat
import id.asisten.app.domain.pengingat.SuaraPengingatCodec
import id.asisten.app.notifikasi.HasilSalin
import id.asisten.app.notifikasi.PengelolaSuara
import id.asisten.app.notifikasi.PenjadwalPengingat
import java.time.ZoneId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PengingatViewModel(
    private val pengaturanRepo: PengaturanRepository,
    private val penjadwal: PenjadwalPengingat,
    private val suaraRepo: SuaraPengingatRepository,
    private val pengelola: PengelolaSuara,
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

    /** The reminder sound; null until the first read from storage. */
    val suara: StateFlow<PengaturanSuaraPengingat?> =
        suaraRepo.pengaturan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _pesanSuara = MutableStateFlow<String?>(null)
    val pesanSuara: StateFlow<String?> = _pesanSuara

    private val _sedangMenyalin = MutableStateFlow(false)
    val sedangMenyalin: StateFlow<Boolean> = _sedangMenyalin

    fun tutupPesanSuara() {
        _pesanSuara.value = null
    }

    /** Tells the screen what to show when a preview cannot start. */
    fun laporGagalPutar() {
        _pesanSuara.value = LabelSuaraPengingat.GAGAL_PUTAR
    }

    fun laporPemilihTidakAda() {
        _pesanSuara.value = LabelSuaraPengingat.GAGAL_PEMILIH
    }

    /** The address to preview, or null when the file is gone. The phone's default has no address of its own here. */
    fun alamatContoh(suara: SuaraPengingat): Uri? = pengelola.uri(suara)

    fun pilihBawaan() {
        viewModelScope.launch { ganti(SuaraPengingat.Bawaan) }
    }

    fun pilihNadaAplikasi(kode: String) {
        viewModelScope.launch { ganti(SuaraPengingat.Aplikasi(kode)) }
    }

    /** A tone from the system picker. */
    fun pilihNadaSistem(uri: Uri) {
        viewModelScope.launch {
            val nama = pengelola.namaNadaSistem(uri)
            val baru = SuaraPengingatCodec.sistem(uri.toString(), nama)
            if (baru == null) _pesanSuara.value = LabelSuaraPengingat.GAGAL_NADA else ganti(baru)
        }
    }

    /** Music from the phone: the app copies it, then switches to the copy. The old copy is deleted. */
    fun pilihMusik(sumber: Uri) {
        if (_sedangMenyalin.value) return
        _sedangMenyalin.value = true
        viewModelScope.launch {
            try {
                when (val hasil = pengelola.salinBerkas(sumber)) {
                    is HasilSalin.Berhasil -> ganti(hasil.suara)
                    is HasilSalin.Ditolak -> _pesanSuara.value = LabelSuaraPengingat.tolak(hasil.alasan)
                    HasilSalin.Gagal -> _pesanSuara.value = LabelSuaraPengingat.GAGAL_SALIN
                }
            } finally {
                _sedangMenyalin.value = false
            }
        }
    }

    fun ubahVolumeAlarm(pakai: Boolean) {
        viewModelScope.launch { suaraRepo.ubahVolumeAlarm(pakai) }
    }

    /** Saves the choice and deletes the app's copy of the music that is no longer used. */
    private suspend fun ganti(baru: SuaraPengingat) {
        val lama = suaraRepo.pengaturan.first().suara
        suaraRepo.ubahSuara(baru)
        _pesanSuara.value = null
        if (lama is SuaraPengingat.Berkas && lama != baru) pengelola.hapusSalinan(lama)
    }

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
