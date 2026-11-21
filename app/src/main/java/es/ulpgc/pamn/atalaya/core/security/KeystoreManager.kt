package es.ulpgc.pamn.atalaya.core.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import java.security.GeneralSecurityException
import java.security.KeyStore
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject

/**
 * Gestiona la clave hardware del Android Keystore usada con `BiometricPrompt.CryptoObject`
 * para envolver y desenvolver la clave de datos (`DEK`).
 */
interface KeystoreManager {
    fun hasBiometricKey(): Boolean
    fun generateBiometricKey()
    fun deleteBiometricKey()
    fun createBiometricEncryptCipher(): Cipher
    fun createBiometricDecryptCipher(iv: ByteArray): Cipher
    fun encryptWithAuthenticatedCipher(cipher: Cipher, plaintext: ByteArray): EncryptedPayload
    fun decryptWithAuthenticatedCipher(cipher: Cipher, payload: EncryptedPayload): ByteArray
}

/** Implementación respaldada por `AndroidKeyStore` con invalidación automática al cambiar biometría. */
class AndroidKeystoreManager @Inject constructor() : KeystoreManager {

    override fun hasBiometricKey(): Boolean {
        return try {
            loadKeyStore().containsAlias(BIOMETRIC_KEY_ALIAS)
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException("No se pudo consultar el Keystore", e)
        }
    }

    override fun generateBiometricKey() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val builder = KeyGenParameterSpec.Builder(
                BIOMETRIC_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(AES_KEY_BITS)
                .setRandomizedEncryptionRequired(true)
                .setUserAuthenticationRequired(true)
                .setInvalidatedByBiometricEnrollment(true)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                builder.setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG)
            } else {
                @Suppress("DEPRECATION")
                builder.setUserAuthenticationValidityDurationSeconds(-1)
            }

            keyGenerator.init(builder.build())
            keyGenerator.generateKey()
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException(
                "No se pudo generar la clave biométrica en Keystore",
                e
            )
        }
    }

    override fun deleteBiometricKey() {
        try {
            val keyStore = loadKeyStore()
            if (keyStore.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                keyStore.deleteEntry(BIOMETRIC_KEY_ALIAS)
            }
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException(
                "No se pudo eliminar la clave biométrica del Keystore",
                e
            )
        }
    }

    override fun createBiometricEncryptCipher(): Cipher {
        val secretKey = getOrThrowBiometricKey()
        return try {
            Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
                init(Cipher.ENCRYPT_MODE, secretKey)
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw CryptoException.KeyInvalidatedException(cause = e)
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException(
                "No se pudo inicializar el cifrador biométrico",
                e
            )
        }
    }

    override fun createBiometricDecryptCipher(iv: ByteArray): Cipher {
        require(iv.isNotEmpty()) { "El IV biométrico no puede estar vacío" }
        val secretKey = getOrThrowBiometricKey()
        return try {
            Cipher.getInstance(AES_GCM_TRANSFORMATION).apply {
                init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
            }
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw CryptoException.KeyInvalidatedException(cause = e)
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException(
                "No se pudo inicializar el descifrador biométrico",
                e
            )
        }
    }

    override fun encryptWithAuthenticatedCipher(
        cipher: Cipher,
        plaintext: ByteArray
    ): EncryptedPayload {
        return try {
            val iv = cipher.iv?.copyOf()
                ?: throw CryptoException.CryptoOperationException("El cifrador no generó un IV")
            val ciphertext = cipher.doFinal(plaintext)
            EncryptedPayload(iv = iv, ciphertext = ciphertext)
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException(
                "Error al cifrar con el CryptoObject autenticado",
                e
            )
        }
    }

    override fun decryptWithAuthenticatedCipher(
        cipher: Cipher,
        payload: EncryptedPayload
    ): ByteArray {
        require(payload.ciphertext.isNotEmpty()) { "El texto cifrado no puede estar vacío" }
        return try {
            cipher.doFinal(payload.ciphertext)
        } catch (e: AEADBadTagException) {
            throw CryptoException.AuthenticationFailedException(
                "La autenticación del bloque biométrico ha fallado",
                e
            )
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException(
                "Error al descifrar con el CryptoObject autenticado",
                e
            )
        }
    }

    private fun getOrThrowBiometricKey(): SecretKey {
        val keyStore = loadKeyStore()
        val key = try {
            keyStore.getKey(BIOMETRIC_KEY_ALIAS, null) as? SecretKey
        } catch (e: GeneralSecurityException) {
            throw CryptoException.CryptoOperationException("Error al obtener clave del Keystore", e)
        }
        return key ?: throw CryptoException.KeyInvalidatedException(
            "No existe clave biométrica registrada en el Keystore"
        )
    }

    private fun loadKeyStore(): KeyStore {
        return try {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        } catch (e: Exception) {
            throw CryptoException.CryptoOperationException("No se pudo abrir AndroidKeyStore", e)
        }
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val BIOMETRIC_KEY_ALIAS = "es.ulpgc.pamn.atalaya.biometric_kek"
        const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        const val AES_KEY_BITS = 256
        const val GCM_TAG_BITS = 128
    }
}
