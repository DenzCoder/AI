package id.asisten.app.notifikasi

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper

/**
 * Plays a short preview of a reminder sound inside the app, on the same stream the notification will use
 * (alarm volume or notification volume). Long music is cut after [BATAS_MS]. Call [berhenti] when the screen closes.
 * All calls come from the main thread.
 */
class PemutarContoh(context: Context) {
    private val context = context.applicationContext
    private val handler = Handler(Looper.getMainLooper())
    private val potong = Runnable { berhenti() }
    private var pemutar: MediaPlayer? = null
    private var saatBerhenti: (() -> Unit)? = null

    /** Starts the preview; false when the file cannot be played. [saatBerhenti] runs when it ends or is stopped. */
    fun putar(uri: Uri, volumeAlarm: Boolean, saatBerhenti: () -> Unit): Boolean {
        berhenti()
        this.saatBerhenti = saatBerhenti
        return try {
            val mp = MediaPlayer()
            pemutar = mp
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(if (volumeAlarm) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            mp.setDataSource(context, uri)
            mp.setOnPreparedListener { it.start() }
            mp.setOnCompletionListener { berhenti() }
            mp.setOnErrorListener { _, _, _ ->
                berhenti()
                true
            }
            mp.prepareAsync()
            handler.postDelayed(potong, BATAS_MS)
            true
        } catch (e: Exception) {
            berhenti()
            false
        }
    }

    fun berhenti() {
        handler.removeCallbacks(potong)
        val mp = pemutar
        pemutar = null
        if (mp != null) {
            try {
                mp.stop()
            } catch (e: IllegalStateException) {
                // Not started yet or already stopped: releasing below is enough.
            }
            mp.release()
        }
        val selesai = saatBerhenti
        saatBerhenti = null
        selesai?.invoke()
    }

    private companion object {
        const val BATAS_MS = 6_000L
    }
}
