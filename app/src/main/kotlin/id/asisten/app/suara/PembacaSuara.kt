package id.asisten.app.suara

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import id.asisten.app.domain.suara.PenyiapTeksUcap
import id.asisten.app.domain.suara.StatusSuara
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Reads text aloud with the phone's own text-to-speech engine, in Indonesian. Call every function from the main
 * thread. The engine is started on first use and released by [lepas], so it does not stay in memory.
 *
 * Nothing is sent to this app's servers (it has none); which engine speaks and whether it works offline is up to
 * the phone's speech settings. If the Indonesian voice is missing, [status] says so and nothing is spoken.
 */
class PembacaSuara(konteks: Context) {
    private val app = konteks.applicationContext

    private class Permintaan(val teks: String, val kecepatan: Float, val nada: Float)

    private var mesin: TextToSpeech? = null
    private var siap = false
    private var tertunda: Permintaan? = null
    private var nomor = 0

    private val _status = MutableStateFlow<StatusSuara?>(null)

    /** Null until the engine has reported back. */
    val status: StateFlow<StatusSuara?> = _status.asStateFlow()

    private val _sedangBicara = MutableStateFlow(false)
    val sedangBicara: StateFlow<Boolean> = _sedangBicara.asStateFlow()

    /** Starts the engine if needed, so [status] becomes known (the settings screen uses this). */
    fun siapkan() {
        if (mesin != null) return
        mesin = TextToSpeech(app) { kode -> sesudahInisialisasi(kode) }
    }

    /** Speaks [teks] after cleaning it for speech; whatever was being said stops first. */
    fun bicara(teks: String, kecepatan: Float, nada: Float) {
        val bersih = PenyiapTeksUcap.siapkan(teks)
        if (bersih.isEmpty()) return
        val permintaan = Permintaan(bersih, kecepatan, nada)
        val m = mesin
        if (m != null && siap) {
            ucapkan(m, permintaan)
        } else {
            tertunda = permintaan
            siapkan()
        }
    }

    fun berhenti() {
        tertunda = null
        mesin?.stop()
        _sedangBicara.value = false
    }

    /** Stops and releases the engine. A later [bicara] or [siapkan] starts it again. */
    fun lepas() {
        mesin?.stop()
        mesin?.shutdown()
        mesin = null
        siap = false
        tertunda = null
        _status.value = null
        _sedangBicara.value = false
    }

    private fun sesudahInisialisasi(kode: Int) {
        // When no engine exists at all the system calls back from inside the constructor, before `mesin` is set.
        val m = mesin
        if (kode != TextToSpeech.SUCCESS || m == null) {
            tidakSiap(StatusSuara.MESIN_GAGAL)
            return
        }
        val status = StatusSuara.dari(true, m.setLanguage(Locale.forLanguageTag("id-ID")))
        if (status != StatusSuara.SIAP) {
            tidakSiap(status)
            return
        }
        _status.value = status
        siap = true
        m.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _sedangBicara.value = true
            }

            override fun onDone(utteranceId: String?) {
                _sedangBicara.value = false
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _sedangBicara.value = false
            }

            override fun onStop(utteranceId: String?, interrupted: Boolean) {
                _sedangBicara.value = false
            }
        })
        tertunda?.let { ucapkan(m, it) }
        tertunda = null
    }

    private fun tidakSiap(status: StatusSuara) {
        _status.value = status
        siap = false
        tertunda = null
    }

    private fun ucapkan(m: TextToSpeech, p: Permintaan) {
        m.setSpeechRate(p.kecepatan)
        m.setPitch(p.nada)
        nomor++
        val hasil = m.speak(p.teks, TextToSpeech.QUEUE_FLUSH, null, "hanako-$nomor")
        if (hasil != TextToSpeech.SUCCESS) _sedangBicara.value = false
    }
}
