package es.ulpgc.pamn.atalaya.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class DefaultAesGcmCipherTest {

    private val keyDerivation = Pbkdf2KeyDerivation()
    private val cipher = DefaultAesGcmCipher()

    @Test
    fun `encrypt y decrypt hacen round trip en bytes y strings con unicode`() {
        val key = keyDerivation.generateRandomKey()
        val textoOriginal = "Contraseña 🔐 — ULPGC PAMN ñáéíóú"

        val encryptedString = cipher.encryptString(textoOriginal, key)
        val decryptedString = cipher.decryptString(encryptedString, key)

        assertEquals(textoOriginal, decryptedString)

        val emptyEncrypted = cipher.encryptString("", key)
        assertEquals("", cipher.decryptString(emptyEncrypted, key))

        val rawInput = byteArrayOf(0, 1, 2, 127, -128)
        val rawPayload = cipher.encrypt(rawInput, key)
        assertArrayEquals(rawInput, cipher.decrypt(rawPayload, key))
    }

    @Test
    fun `cada cifrado genera un IV unico incluso con mismo texto y clave`() {
        val key = keyDerivation.generateRandomKey()
        val p1 = cipher.encrypt("mismo_secreto".toByteArray(), key)
        val p2 = cipher.encrypt("mismo_secreto".toByteArray(), key)

        assertFalse(p1.iv.contentEquals(p2.iv))
        assertFalse(p1.ciphertext.contentEquals(p2.ciphertext))
    }

    @Test
    fun `clave incorrecta lanza AuthenticationFailedException`() {
        val keyA = keyDerivation.generateRandomKey()
        val keyB = keyDerivation.generateRandomKey()
        val payload = cipher.encrypt("dato_critico".toByteArray(), keyA)

        assertThrows(CryptoException.AuthenticationFailedException::class.java) {
            cipher.decrypt(payload, keyB)
        }
    }

    @Test
    fun `manipular ciphertext o IV lanza AuthenticationFailedException`() {
        val key = keyDerivation.generateRandomKey()
        val payload = cipher.encrypt("integridad_gcm".toByteArray(), key)

        val tamperedCiphertext = payload.ciphertext.copyOf().apply {
            this[0] = (this[0].toInt() xor 0x01).toByte()
        }
        assertThrows(CryptoException.AuthenticationFailedException::class.java) {
            cipher.decrypt(payload.copy(ciphertext = tamperedCiphertext), key)
        }

        val tamperedIv = payload.iv.copyOf().apply {
            this[0] = (this[0].toInt() xor 0x01).toByte()
        }
        assertThrows(CryptoException.AuthenticationFailedException::class.java) {
            cipher.decrypt(payload.copy(iv = tamperedIv), key)
        }
    }

    @Test
    fun `AAD correcto descifra y AAD distinto falla autenticacion`() {
        val key = keyDerivation.generateRandomKey()
        val aad = "credential_id:42".toByteArray()
        val wrongAad = "credential_id:99".toByteArray()

        val encoded = cipher.encryptString("token_secreto", key, associatedData = aad)

        assertEquals("token_secreto", cipher.decryptString(encoded, key, associatedData = aad))
        assertThrows(CryptoException.AuthenticationFailedException::class.java) {
            cipher.decryptString(encoded, key, associatedData = wrongAad)
        }
    }

    @Test
    fun `rechaza claves cuyo tamano no sea 32 bytes`() {
        val shortKey = ByteArray(16) { 7 }

        assertThrows(IllegalArgumentException::class.java) {
            cipher.encrypt("hola".toByteArray(), shortKey)
        }
    }
}
