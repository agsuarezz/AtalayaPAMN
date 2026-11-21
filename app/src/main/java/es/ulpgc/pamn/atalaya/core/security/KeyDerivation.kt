package es.ulpgc.pamn.atalaya.core.security

import java.security.GeneralSecurityException
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import javax.inject.Inject

/** Derivación de claves desde la contraseña maestra y generación segura de aleatoriedad. */
interface KeyDerivation {
    fun generateSalt(sizeBytes: Int = DEFAULT_SALT_SIZE): ByteArray
    fun generateRandomKey(sizeBytes: Int = DEFAULT_KEY_SIZE): ByteArray
    fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int = DEFAULT_ITERATIONS
    ): ByteArray

    companion object {
        const val DEFAULT_SALT_SIZE = 16
        const val DEFAULT_KEY_SIZE = 32
        const val DEFAULT_ITERATIONS = 310_000
        const val MIN_SALT_SIZE = 16
        const val MIN_ITERATIONS = 1_000
    }
}

/**
 * Implementación de [KeyDerivation] basada en `PBKDF2WithHmacSHA256` nativo de Android (API 26+)
 * y [SecureRandom]. Limpia el array de contraseña del [PBEKeySpec] al terminar.
 */
class Pbkdf2KeyDerivation @Inject constructor() : KeyDerivation {

    private val secureRandom = SecureRandom()

    override fun generateSalt(sizeBytes: Int): ByteArray {
        require(sizeBytes >= KeyDerivation.MIN_SALT_SIZE) {
            "La sal debe tener al menos ${KeyDerivation.MIN_SALT_SIZE} bytes"
        }
        return ByteArray(sizeBytes).apply(secureRandom::nextBytes)
    }

    override fun generateRandomKey(sizeBytes: Int): ByteArray {
        require(sizeBytes >= KeyDerivation.DEFAULT_KEY_SIZE) {
            "La clave debe tener al menos ${KeyDerivation.DEFAULT_KEY_SIZE} bytes"
        }
        return ByteArray(sizeBytes).apply(secureRandom::nextBytes)
    }

    override fun deriveKey(
        password: CharArray,
        salt: ByteArray,
        iterations: Int
    ): ByteArray {
        require(password.isNotEmpty()) { "La contraseña no puede estar vacía" }
        require(salt.size >= KeyDerivation.MIN_SALT_SIZE) {
            "La sal debe tener al menos ${KeyDerivation.MIN_SALT_SIZE} bytes"
        }
        require(iterations >= KeyDerivation.MIN_ITERATIONS) {
            "El número mínimo de iteraciones es ${KeyDerivation.MIN_ITERATIONS}"
        }

        val spec = PBEKeySpec(password, salt, iterations, KeyDerivation.DEFAULT_KEY_SIZE * 8)
        return try {
            val factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM)
            factory.generateSecret(spec).encoded
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException("No se pudo derivar la clave", e)
        } finally {
            spec.clearPassword()
        }
    }

    private companion object {
        const val PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256"
    }
}
