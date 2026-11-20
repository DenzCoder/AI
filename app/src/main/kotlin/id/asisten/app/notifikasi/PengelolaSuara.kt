package id.asisten.app.notifikasi

import android.content.ContentValues
import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import id.asisten.app.R
import id.asisten.app.domain.pengingat.AlasanTolakBerkas
import id.asisten.app.domain.pengingat.BatasBerkasNada
import id.asisten.app.domain.pengingat.SuaraPengingat
import id.asisten.app.domain.pengingat.SuaraPengingatCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed interface HasilSalin {
    data class Berhasil(val suara: SuaraPengingat.Berkas) : HasilSalin
    data class Ditolak(val alasan: AlasanTolakBerkas) : HasilSalin
    data object Gagal : HasilSalin
}

/**
 * Turns a [SuaraPengingat] into an address the notification system can play, and keeps the app's own copies of
 * music the user picked. No storage permission is needed: the copy is created by this app in the media store.
 */
class PengelolaSuara(context: Context) {
    private val context = context.applicationContext
    private val resolver get() = context.contentResolver

    /**
     * The address to play, or null when the phone's default sound should be used: either the choice is
     * [SuaraPengingat.Bawaan], or the file behind it is gone (the user deleted the music, for example).
     */
    fun uri(suara: SuaraPengingat): Uri? = when (suara) {
        SuaraPengingat.Bawaan -> null
        is SuaraPengingat.Aplikasi -> idRaw(suara.kode)?.let { Uri.parse("android.resource://${context.packageName}/$it") }
        is SuaraPengingat.Sistem -> Uri.parse(suara.uri).takeIf { bisaDibuka(it) }
        is SuaraPengingat.Berkas -> Uri.parse(suara.uri).takeIf { bisaDibuka(it) }
    }

    /** Whether the choice can really be played now (used to warn the user instead of failing silently). */
    fun bisaDiputar(suara: SuaraPengingat): Boolean = suara == SuaraPengingat.Bawaan || uri(suara) != null

    private fun idRaw(kode: String): Int? = when (kode) {
        "lonceng" -> R.raw.nada_lonceng
        "kicau" -> R.raw.nada_kicau
        "kotak_musik" -> R.raw.nada_kotak_musik
        "bel_sekolah" -> R.raw.nada_bel_sekolah
        "alarm_digital" -> R.raw.nada_alarm_digital
        else -> null
    }

    private fun bisaDibuka(uri: Uri): Boolean = try {
        resolver.openAssetFileDescriptor(uri, "r")?.use { true } ?: false
    } catch (e: Exception) {
        false
    }

    /** The title of a tone picked in the system picker. */
    suspend fun namaNadaSistem(uri: Uri): String = withContext(Dispatchers.IO) {
        try {
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)
        } catch (e: Exception) {
            null
        } ?: "Nada sistem"
    }

    /** Copies the picked music file into the media store (Notifications/ASISTEN) and returns the app's own copy. */
    suspend fun salinBerkas(sumber: Uri): HasilSalin = withContext(Dispatchers.IO) {
        var baru: Uri? = null
        try {
            val (namaAsli, ukuran) = infoBerkas(sumber)
            val mime = resolver.getType(sumber)
            BatasBerkasNada.periksa(mime, namaAsli, ukuran)?.let { return@withContext HasilSalin.Ditolak(it) }
            val mimeSimpan = BatasBerkasNada.mimeUntukSimpan(mime, namaAsli)
                ?: return@withContext HasilSalin.Ditolak(AlasanTolakBerkas.BUKAN_AUDIO)

            val nilai = ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, BatasBerkasNada.namaBerkasAman(namaAsli))
                put(MediaStore.Audio.Media.TITLE, BatasBerkasNada.judulDariNama(namaAsli))
                put(MediaStore.Audio.Media.MIME_TYPE, mimeSimpan)
                put(MediaStore.Audio.Media.RELATIVE_PATH, "${Environment.DIRECTORY_NOTIFICATIONS}/ASISTEN")
                put(MediaStore.Audio.Media.IS_NOTIFICATION, 1)
                // Hidden from other apps until the copy is complete.
                put(MediaStore.Audio.Media.IS_PENDING, 1)
            }
            val koleksi = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val tujuan = resolver.insert(koleksi, nilai) ?: return@withContext HasilSalin.Gagal
            baru = tujuan

            val jumlah = resolver.openInputStream(sumber)?.use { masuk ->
                resolver.openOutputStream(tujuan)?.use { keluar -> salinDenganBatas(masuk, keluar) }
            }
            if (jumlah == null) return@withContext batalkan(tujuan, HasilSalin.Gagal)
            if (jumlah > BatasBerkasNada.MAKS_BYTE) {
                return@withContext batalkan(tujuan, HasilSalin.Ditolak(AlasanTolakBerkas.TERLALU_BESAR))
            }
            if (jumlah == 0L) return@withContext batalkan(tujuan, HasilSalin.Ditolak(AlasanTolakBerkas.KOSONG))
            resolver.update(tujuan, ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }, null, null)

            val suara = SuaraPengingatCodec.berkas(tujuan.toString(), BatasBerkasNada.judulDariNama(namaAsli))
                ?: return@withContext batalkan(tujuan, HasilSalin.Gagal)
            HasilSalin.Berhasil(suara)
        } catch (e: Exception) {
            Log.w(TAG, "Gagal menyalin musik", e)
            baru?.let { hapusDiam(it) }
            HasilSalin.Gagal
        }
    }

    /** Deletes the app's own copy of a music file. Tones from the system or the app itself are never touched. */
    fun hapusSalinan(suara: SuaraPengingat) {
        if (suara !is SuaraPengingat.Berkas) return
        val uri = Uri.parse(suara.uri)
        // Only entries of the audio media store can be ours; anything else is left alone.
        if (uri.authority != MediaStore.AUTHORITY || uri.pathSegments.getOrNull(1) != "audio") return
        hapusDiam(uri)
    }

    private fun batalkan(uri: Uri, hasil: HasilSalin): HasilSalin {
        hapusDiam(uri)
        return hasil
    }

    private fun hapusDiam(uri: Uri) {
        try {
            resolver.delete(uri, null, null)
        } catch (e: Exception) {
            // Not ours (for example after a reinstall) or already gone: nothing more to do.
            Log.w(TAG, "Salinan tidak bisa dihapus", e)
        }
    }

    private fun infoBerkas(uri: Uri): Pair<String, Long?> {
        var nama = uri.lastPathSegment ?: "nada"
        var ukuran: Long? = null
        resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { c ->
            if (c.moveToFirst()) {
                c.getColumnIndex(OpenableColumns.DISPLAY_NAME).takeIf { it >= 0 && !c.isNull(it) }?.let { nama = c.getString(it) }
                c.getColumnIndex(OpenableColumns.SIZE).takeIf { it >= 0 && !c.isNull(it) }?.let { ukuran = c.getLong(it) }
            }
        }
        return nama to ukuran
    }

    /** Copies at most one byte more than the limit, so an oversized file is noticed without writing all of it. */
    private fun salinDenganBatas(masuk: java.io.InputStream, keluar: java.io.OutputStream): Long {
        val penyangga = ByteArray(16 * 1024)
        var total = 0L
        while (total <= BatasBerkasNada.MAKS_BYTE) {
            val n = masuk.read(penyangga)
            if (n < 0) break
            keluar.write(penyangga, 0, n)
            total += n
        }
        return total
    }

    private companion object {
        const val TAG = "PengelolaSuara"
    }
}
