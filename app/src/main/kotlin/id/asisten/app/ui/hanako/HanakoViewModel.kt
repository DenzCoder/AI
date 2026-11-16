package id.asisten.app.ui.hanako

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import id.asisten.app.data.ai.KlienGemini
import id.asisten.app.data.hanako.HanakoRepository
import id.asisten.app.data.hanako.HasilPelaksanaan
import id.asisten.app.data.hanako.PelaksanaAksi
import id.asisten.app.data.pengaturan.PengaturanRepository
import id.asisten.app.data.repo.AktivitasRepository
import id.asisten.app.data.repo.JadwalRepository
import id.asisten.app.data.repo.MemoriRepository
import id.asisten.app.data.suara.SuaraRepository
import id.asisten.app.domain.hanako.BalasanAksi
import id.asisten.app.domain.hanako.DataSaatIni
import id.asisten.app.domain.hanako.EkspresiHanako
import id.asisten.app.domain.hanako.HasilGemini
import id.asisten.app.domain.hanako.HasilUrai
import id.asisten.app.domain.hanako.JenisGalat
import id.asisten.app.domain.hanako.KonteksHanako
import id.asisten.app.domain.hanako.LabelAksi
import id.asisten.app.domain.hanako.LatarHanako
import id.asisten.app.domain.hanako.PanggilanFungsi
import id.asisten.app.domain.hanako.PengaturanHanako
import id.asisten.app.domain.hanako.PenjawabLokal
import id.asisten.app.domain.hanako.PermintaanGemini
import id.asisten.app.domain.hanako.PeranChat
import id.asisten.app.domain.hanako.PesanChat
import id.asisten.app.domain.hanako.RiwayatChat
import id.asisten.app.domain.hanako.RiwayatUsulan
import id.asisten.app.domain.hanako.UraiAksi
import id.asisten.app.domain.hanako.UraiBalasan
import id.asisten.app.domain.hanako.UsulanMentah
import id.asisten.app.domain.hanako.UsulanTampil
import id.asisten.app.domain.jadwal.PenyusunHarian
import id.asisten.app.domain.memori.KonteksAi
import id.asisten.app.domain.suara.PengaturanSuara
import id.asisten.app.suara.PembacaSuara
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** A failed request, kept on screen with the user's message so "Coba lagi" can resend it. */
data class GalatChat(val jenis: JenisGalat, val rinci: String = "")

sealed interface HasilUji {
    data object Memproses : HasilUji
    data object Berhasil : HasilUji
    data class Gagal(val jenis: JenisGalat, val rinci: String) : HasilUji
}

enum class PesanKunci { DISIMPAN, DITOLAK, PERLU_SETUJU }

class HanakoViewModel(
    private val hanako: HanakoRepository,
    private val pengaturan: PengaturanRepository,
    private val jadwal: JadwalRepository,
    private val aktivitas: AktivitasRepository,
    private val memori: MemoriRepository,
    private val klien: KlienGemini,
    private val suara: SuaraRepository,
    private val pembaca: PembacaSuara,
    private val pelaksana: PelaksanaAksi,
) : ViewModel() {

    val riwayat: StateFlow<List<PesanChat>> =
        hanako.riwayat.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Null until the first read from storage, so the screen never flashes default values. */
    val pengaturanHanako: StateFlow<PengaturanHanako?> =
        hanako.pengaturan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /**
     * Changes Hanako proposed and the user has not decided on yet. Rebuilt whenever the data changes, so a proposal
     * that no longer makes sense (the task was deleted, the date passed) disappears by itself.
     */
    val usulan: StateFlow<List<UsulanTampil>> = combine(
        hanako.usulan, aktivitas.tugas, jadwal.kegiatan, jadwal.pelajaran, pengaturan.pengingat,
    ) { mentah, tugas, kegiatan, pelajaran, pengingat ->
        val sekarang = System.currentTimeMillis()
        val data = DataSaatIni(LocalDate.now(), tugas, kegiatan, pelajaran, pengingat, sekarang)
        RiwayatUsulan.tampil(RiwayatUsulan.segar(mentah, sekarang), data)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val kunciAksi = Mutex()
    private val _sedangMelaksanakan = MutableStateFlow(false)
    val sedangMelaksanakan: StateFlow<Boolean> = _sedangMelaksanakan.asStateFlow()

    private val _sedangMenjawab = MutableStateFlow(false)
    val sedangMenjawab: StateFlow<Boolean> = _sedangMenjawab.asStateFlow()

    private val _galat = MutableStateFlow<GalatChat?>(null)
    val galat: StateFlow<GalatChat?> = _galat.asStateFlow()

    private val _hasilUji = MutableStateFlow<HasilUji?>(null)
    val hasilUji: StateFlow<HasilUji?> = _hasilUji.asStateFlow()

    /** Each new reply of Hanako (not the stored history), for read-aloud. */
    private val _balasanBaru = MutableSharedFlow<PesanChat>(extraBufferCapacity = 4)
    val balasanBaru: SharedFlow<PesanChat> = _balasanBaru.asSharedFlow()

    val pengaturanSuara: StateFlow<PengaturanSuara> =
        suara.pengaturan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PengaturanSuara())
    val sedangBicara = pembaca.sedangBicara
    val statusSuara = pembaca.status

    private val _pesanKunci = MutableStateFlow<PesanKunci?>(null)
    val pesanKunci: StateFlow<PesanKunci?> = _pesanKunci.asStateFlow()

    fun kirim(teks: String) {
        if (_sedangMenjawab.value) return
        val bersih = teks.trim().take(RiwayatChat.MAKS_PANJANG)
        if (bersih.isEmpty()) return
        _galat.value = null
        _sedangMenjawab.value = true
        viewModelScope.launch {
            try {
                hanako.tambahPesan(PesanChat(PeranChat.PENGGUNA, bersih, waktu = System.currentTimeMillis()))
                jawab(bersih)
            } finally {
                _sedangMenjawab.value = false
            }
        }
    }

    /** Sends the last user message again (it stays in the history when a request fails). */
    fun cobaLagi() {
        if (_sedangMenjawab.value) return
        _sedangMenjawab.value = true
        _galat.value = null
        viewModelScope.launch {
            try {
                val terakhir = hanako.riwayat.first().lastOrNull()
                if (terakhir != null && terakhir.peran == PeranChat.PENGGUNA) jawab(terakhir.isi)
            } finally {
                _sedangMenjawab.value = false
            }
        }
    }

    fun tutupGalat() {
        _galat.value = null
    }

    private suspend fun jawab(pertanyaan: String) {
        try {
            val p = hanako.pengaturan.first()
            if (!p.online) {
                // Local mode: answers come from the data on the phone only; nothing is sent anywhere.
                val b = PenjawabLokal.jawab(pertanyaan, kumpulkanLatar(bagikan = true, ikutkanMemori = false))
                simpanBalasan(PesanChat(PeranChat.HANAKO, b.teks, b.ekspresi, System.currentTimeMillis(), lokal = true))
                return
            }
            val kunci = hanako.kunci()
            if (kunci == null) {
                _galat.value = GalatChat(JenisGalat.TANPA_KUNCI)
                return
            }
            val bisaMengusulkan = p.bisaMengusulkan
            val latar = kumpulkanLatar(bagikan = p.bagikanData, ikutkanMemori = true, aksi = bisaMengusulkan)
            val badan = PermintaanGemini.badan(KonteksHanako.sistem(latar), hanako.riwayat.first(), alat = bisaMengusulkan)
            when (val hasil = klien.kirim(kunci, p.model, badan)) {
                is HasilGemini.Gagal -> _galat.value = GalatChat(hasil.jenis, hasil.rinci)
                is HasilGemini.Berhasil -> {
                    val b = UraiBalasan.urai(hasil.teks)
                    val ekspresi = b.ekspresi ?: EkspresiHanako.NORMAL
                    if (bisaMengusulkan && hasil.panggilan.isNotEmpty()) {
                        simpanUsulan(b.teks, hasil.panggilan, ekspresi)
                    } else if (b.teks.isEmpty()) {
                        _galat.value = GalatChat(JenisGalat.KOSONG)
                    } else {
                        simpanBalasan(PesanChat(PeranChat.HANAKO, b.teks, ekspresi, System.currentTimeMillis()))
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _galat.value = GalatChat(JenisGalat.TIDAK_DIKENAL, e.javaClass.simpleName)
        }
    }

    /**
     * The model asked for changes. Each call is checked (UraiAksi) and the valid ones are stored as proposals; nothing
     * is changed until the user taps Setujui. The chat text always says so, whatever the model wrote.
     */
    private suspend fun simpanUsulan(teksModel: String, panggilan: List<PanggilanFungsi>, ekspresi: EkspresiHanako) {
        val data = pelaksana.dataSaatIni()
        val susun = BalasanAksi.susun(teksModel, panggilan, data)
        val sekarang = System.currentTimeMillis()
        val ada = hanako.usulan.first()
        val baru = ArrayList<UsulanMentah>()
        for (c in susun.disimpan) {
            val id = RiwayatUsulan.idBaru(ada + baru, sekarang)
            RiwayatUsulan.mentah(id, sekarang, c)?.let { baru.add(it) }
        }
        hanako.tambahUsulan(baru, sekarang)
        simpanBalasan(PesanChat(PeranChat.HANAKO, susun.teks, ekspresi, sekarang))
    }

    // --- the user's decision on a proposal ---

    fun setujui(id: Long) = putuskan(id, setuju = true)

    fun tolak(id: Long) = putuskan(id, setuju = false)

    private fun putuskan(id: Long, setuju: Boolean) {
        viewModelScope.launch {
            // The lock also stops a double tap from running the same proposal twice: the second call finds it gone.
            kunciAksi.withLock {
                val mentah = hanako.usulan.first().firstOrNull { it.id == id } ?: return@withLock
                _sedangMelaksanakan.value = true
                try {
                    prosesKeputusan(mentah, setuju)
                } catch (e: CancellationException) {
                    throw e
                } finally {
                    _sedangMelaksanakan.value = false
                }
            }
        }
    }

    private suspend fun prosesKeputusan(mentah: UsulanMentah, setuju: Boolean) {
        val data = pelaksana.dataSaatIni()
        val panggilan = RiwayatUsulan.panggilan(mentah)
        val hasil = panggilan?.let { UraiAksi.urai(it, data) }
        if (hasil !is HasilUrai.Sah) {
            hanako.hapusUsulan(mentah.id)
            laporkan(LabelAksi.laporTidakBerlaku((hasil as? HasilUrai.Tidak)?.alasan ?: "Isinya tidak terbaca."))
            return
        }
        val aksi = hasil.aksi
        val ringkas = LabelAksi.ringkas(aksi, data)
        if (!setuju) {
            hanako.hapusUsulan(mentah.id)
            laporkan(LabelAksi.laporTolak(ringkas))
            return
        }
        try {
            when (val h = pelaksana.jalankan(aksi, data)) {
                HasilPelaksanaan.Berhasil -> {
                    hanako.hapusUsulan(mentah.id)
                    laporkan(LabelAksi.laporSetuju(ringkas))
                }
                is HasilPelaksanaan.Gagal -> {
                    hanako.hapusUsulan(mentah.id)
                    laporkan(LabelAksi.laporGagal(ringkas, h.alasan))
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The proposal stays on screen so the user can try again.
            laporkan(LabelAksi.laporError(ringkas))
        }
    }

    /** The app's own report in the chat (not written by the AI, not read aloud). */
    private suspend fun laporkan(teks: String) {
        hanako.tambahPesan(PesanChat(PeranChat.HANAKO, teks, waktu = System.currentTimeMillis(), aksi = true))
    }

    /** Stores Hanako's reply and announces it, so the screen can read it aloud when that is switched on. */
    private suspend fun simpanBalasan(pesan: PesanChat) {
        hanako.tambahPesan(pesan)
        _balasanBaru.tryEmit(pesan)
    }

    /**
     * Reads the current data. With [bagikan] false the lists stay empty and the prompt says nothing is shared.
     * Memories pass through [KonteksAi.pilih]: none when memory is off, and sensitive ones never.
     */
    private suspend fun kumpulkanLatar(bagikan: Boolean, ikutkanMemori: Boolean, aksi: Boolean = false): LatarHanako {
        val sekarang = LocalDateTime.now()
        val profil = pengaturan.profil.first()
        val pengingat = pengaturan.pengingat.first()
        val mode = pengingat.modeEfektif(System.currentTimeMillis())
        if (!bagikan) {
            return LatarHanako(
                namaPengguna = profil.nama, sekarang = sekarang, mode = mode, dataDibagikan = false, aksiAktif = aksi,
            )
        }
        val memoriDipilih = if (ikutkanMemori) KonteksAi.pilih(memori.semua.first(), profil.memoriAktif) else emptyList()
        return LatarHanako(
            namaPengguna = profil.nama,
            sekarang = sekarang,
            jadwalHariIni = PenyusunHarian.susun(sekarang.toLocalDate(), jadwal.kegiatan.first(), jadwal.pelajaran.first()),
            tugas = aktivitas.tugas.first(),
            tujuanAktif = aktivitas.tujuan.first(),
            memori = memoriDipilih,
            mode = mode,
            kegiatan = if (aksi) jadwal.kegiatan.first() else emptyList(),
            pelajaran = if (aksi) jadwal.pelajaran.first() else emptyList(),
            pengingat = pengingat,
            aksiAktif = aksi,
        )
    }

    // --- settings ---

    fun simpanKunci(mentah: String, setuju: Boolean) {
        if (!setuju) {
            _pesanKunci.value = PesanKunci.PERLU_SETUJU
            return
        }
        viewModelScope.launch {
            _pesanKunci.value = if (hanako.simpanKunci(mentah)) PesanKunci.DISIMPAN else PesanKunci.DITOLAK
            _hasilUji.value = null
        }
    }

    fun hapusKunci() {
        viewModelScope.launch {
            hanako.hapusKunci()
            _pesanKunci.value = null
            _hasilUji.value = null
        }
    }

    fun ubahOnline(aktif: Boolean) {
        viewModelScope.launch { hanako.ubahOnline(aktif) }
    }

    fun ubahAksi(aktif: Boolean) {
        viewModelScope.launch { hanako.ubahAksi(aktif) }
    }

    fun ubahBagikanData(bagikan: Boolean) {
        viewModelScope.launch { hanako.ubahBagikanData(bagikan) }
    }

    fun ubahModel(nama: String) {
        viewModelScope.launch {
            hanako.ubahModel(nama)
            _hasilUji.value = null
        }
    }

    fun hapusRiwayat() {
        viewModelScope.launch { hanako.hapusRiwayat() }
        _galat.value = null
    }

    /** One tiny real request, so the user can see whether the key and model work. */
    fun ujiKoneksi() {
        if (_hasilUji.value == HasilUji.Memproses) return
        _hasilUji.value = HasilUji.Memproses
        viewModelScope.launch {
            _hasilUji.value = try {
                val p = hanako.pengaturan.first()
                val kunci = hanako.kunci()
                if (kunci == null) {
                    HasilUji.Gagal(JenisGalat.TANPA_KUNCI, "")
                } else {
                    val badan = PermintaanGemini.badan(
                        "Balas dengan satu kata saja.",
                        listOf(PesanChat(PeranChat.PENGGUNA, "Halo")),
                    )
                    when (val h = klien.kirim(kunci, p.model, badan)) {
                        is HasilGemini.Berhasil -> HasilUji.Berhasil
                        is HasilGemini.Gagal -> HasilUji.Gagal(h.jenis, h.rinci)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                HasilUji.Gagal(JenisGalat.TIDAK_DIKENAL, e.javaClass.simpleName)
            }
        }
    }

    fun tutupPesanKunci() {
        _pesanKunci.value = null
    }

    // --- voice ---

    fun bacaKeras(teks: String) {
        val p = pengaturanSuara.value
        pembaca.bicara(teks, p.kecepatan, p.nada)
    }

    fun berhenti() {
        pembaca.berhenti()
    }

    /** Starts the speech engine so its status is known (used when the settings screen opens). */
    fun periksaSuara() {
        pembaca.siapkan()
    }

    fun ubahBacaOtomatis(aktif: Boolean) {
        viewModelScope.launch { suara.ubahBacaOtomatis(aktif) }
    }

    fun ubahKecepatan(nilai: Float) {
        viewModelScope.launch { suara.ubahKecepatan(nilai) }
    }

    fun ubahNada(nilai: Float) {
        viewModelScope.launch { suara.ubahNada(nilai) }
    }

    override fun onCleared() {
        pembaca.lepas()
    }
}
