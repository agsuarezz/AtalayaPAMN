package es.ulpgc.pamn.atalaya.core.security

import java.util.Base64

/**
 * Representa un bloque cifrado con AES-GCM formado por su vector de inicialización (IV)
 * y el texto cifrado (que incluye el tag de autenticación GCM).
 *
 * Sobrescribe `toString()` para no exponer jamás bytes sensibles en logs o trazas.
 */
data class EncryptedPayload(
    val iv: ByteArray,
    val ciphertext: ByteArray
) {
    fun toStorageString(): String {
        val encoder = Base64.getEncoder()
        return "$STORAGE_VERSION:${encoder.encodeToString(iv)}:${encoder.encodeToString(ciphertext)}"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedPayload) return false
        return iv.contentEquals(other.iv) && ciphertext.contentEquals(other.ciphertext)
    }

    override fun hashCode(): Int {
        var result = iv.contentHashCode()
        result = 31 * result + ciphertext.contentHashCode()
        return result
    }

    override fun toString(): String {
        return "EncryptedPayload(ivBytes=${iv.size}, ciphertextBytes=${ciphertext.size})"
    }

    companion object {
        private const val STORAGE_VERSION = "v1"
        private const val EXPECTED_PARTS = 3

        fun fromStorageString(encoded: String): EncryptedPayload {
            val parts = encoded.split(':')
            if (parts.size != EXPECTED_PARTS || parts[0] != STORAGE_VERSION) {
                throw CryptoException.CryptoOperationException(
                    "Formato de payload cifrado no válido"
                )
            }
            val decoder = Base64.getDecoder()
            val iv = try {
                decoder.decode(parts[1])
            } catch (e: IllegalArgumentException) {
                throw CryptoException.CryptoOperationException("IV Base64 no válido", e)
            }
            val ciphertext = try {
                decoder.decode(parts[2])
            } catch (e: IllegalArgumentException) {
                throw CryptoException.CryptoOperationException("Ciphertext Base64 no válido", e)
            }
            if (iv.isEmpty() || ciphertext.isEmpty()) {
                throw CryptoException.CryptoOperationException(
                    "El payload cifrado no puede contener bloques vacíos"
                )
            }
            return EncryptedPayload(iv = iv, ciphertext = ciphertext)
        }
    }
}
