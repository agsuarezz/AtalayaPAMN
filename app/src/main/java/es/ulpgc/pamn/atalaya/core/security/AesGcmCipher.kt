package es.ulpgc.pamn.atalaya.core.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/** Primitiva de cifrado simétrico autenticado para datos de bóveda y DEK envuelta. */
interface AesGcmCipher {
    fun encrypt(
        plaintext: ByteArray,
        key: ByteArray,
        associatedData: ByteArray? = null
    ): EncryptedPayload

    fun decrypt(
        payload: EncryptedPayload,
        key: ByteArray,
        associatedData: ByteArray? = null
    ): ByteArray

    fun encryptString(
        plaintext: String,
        key: ByteArray,
        associatedData: ByteArray? = null
    ): String

    fun decryptString(
        encodedPayload: String,
        key: ByteArray,
        associatedData: ByteArray? = null
    ): String
}

/**
 * Cifrado AES-256-GCM (`AES/GCM/NoPadding`) con IV único de 96 bits generado con [SecureRandom]
 * y tag de autenticación de 128 bits en cada operación.
 */
class DefaultAesGcmCipher @Inject constructor() : AesGcmCipher {

    private val secureRandom = SecureRandom()

    override fun encrypt(
        plaintext: ByteArray,
        key: ByteArray,
        associatedData: ByteArray?
    ): EncryptedPayload {
        requireValidKey(key)
        val iv = ByteArray(IV_SIZE_BYTES).apply(secureRandom::nextBytes)
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = SecretKeySpec(key, KEY_ALGORITHM)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(TAG_LENGTH_BITS, iv))
            if (associatedData != null && associatedData.isNotEmpty()) {
                cipher.updateAAD(associatedData)
            }
            val ciphertext = cipher.doFinal(plaintext)
            EncryptedPayload(iv = iv, ciphertext = ciphertext)
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException("No se pudo cifrar el bloque", e)
        }
    }

    override fun decrypt(
        payload: EncryptedPayload,
        key: ByteArray,
        associatedData: ByteArray?
    ): ByteArray {
        requireValidKey(key)
        require(payload.iv.size == IV_SIZE_BYTES) {
            "El IV debe tener $IV_SIZE_BYTES bytes"
        }
        require(payload.ciphertext.isNotEmpty()) {
            "El texto cifrado no puede estar vacío"
        }
        return try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val secretKey = SecretKeySpec(key, KEY_ALGORITHM)
            cipher.init(
                Cipher.DECRYPT_MODE,
                secretKey,
                GCMParameterSpec(TAG_LENGTH_BITS, payload.iv)
            )
            if (associatedData != null && associatedData.isNotEmpty()) {
                cipher.updateAAD(associatedData)
            }
            cipher.doFinal(payload.ciphertext)
        } catch (e: AEADBadTagException) {
            throw CryptoException.AuthenticationFailedException(
                "Clave incorrecta o datos modificados",
                e
            )
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException("No se pudo descifrar el bloque", e)
        }
    }

    override fun encryptString(
        plaintext: String,
        key: ByteArray,
        associatedData: ByteArray?
    ): String {
        val payload = encrypt(
            plaintext = plaintext.toByteArray(Charsets.UTF_8),
            key = key,
            associatedData = associatedData
        )
        return payload.toStorageString()
    }

    override fun decryptString(
        encodedPayload: String,
        key: ByteArray,
        associatedData: ByteArray?
    ): String {
        val payload = EncryptedPayload.fromStorageString(encodedPayload)
        val plainBytes = decrypt(payload = payload, key = key, associatedData = associatedData)
        return plainBytes.toString(Charsets.UTF_8)
    }

    private fun requireValidKey(key: ByteArray) {
        require(key.size == KEY_SIZE_BYTES) {
            "La clave AES-256 debe tener exactamente $KEY_SIZE_BYTES bytes"
        }
    }

    private companion object {
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_ALGORITHM = "AES"
        const val KEY_SIZE_BYTES = 32
        const val IV_SIZE_BYTES = 12
        const val TAG_LENGTH_BITS = 128
    }
}
