package id.asisten.app.data.hanako

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts small secrets. Both functions return null on any failure; callers must never fall back to plain text. */
interface Pengacak {
    fun acak(teks: String): String?
    fun buka(terkode: String): String?
}

/**
 * AES-256-GCM with a key that lives in the Android Keystore (hardware-backed where the phone has it) and never
 * leaves it. The stored text is Base64(iv + ciphertext). If the Keystore key is lost (for example after a restore
 * to another phone) [buka] returns null and the app asks for the API key again.
 */
class PengacakKeystore(private val alias: String = "asisten_hanako_kunci") : Pengacak {

    private fun kunciAes(): SecretKey {
        val toko = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (toko.getKey(alias, null) as? SecretKey)?.let { return it }
        val pembuat = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        pembuat.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return pembuat.generateKey()
    }

    override fun acak(teks: String): String? = try {
        val sandi = Cipher.getInstance(TRANSFORMASI)
        sandi.init(Cipher.ENCRYPT_MODE, kunciAes())
        val hasil = sandi.iv + sandi.doFinal(teks.toByteArray(StandardCharsets.UTF_8))
        Base64.getEncoder().encodeToString(hasil)
    } catch (e: Exception) {
        null
    }

    override fun buka(terkode: String): String? = try {
        val semua = Base64.getDecoder().decode(terkode)
        if (semua.size <= PANJANG_IV) {
            null
        } else {
            val sandi = Cipher.getInstance(TRANSFORMASI)
            sandi.init(Cipher.DECRYPT_MODE, kunciAes(), GCMParameterSpec(128, semua, 0, PANJANG_IV))
            String(sandi.doFinal(semua, PANJANG_IV, semua.size - PANJANG_IV), StandardCharsets.UTF_8)
        }
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val TRANSFORMASI = "AES/GCM/NoPadding"
        const val PANJANG_IV = 12
    }
}
