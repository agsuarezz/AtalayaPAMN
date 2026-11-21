package es.ulpgc.pamn.atalaya.core.security

import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AndroidKeystoreManagerCipherOpsTest {

    private val keystoreManager = AndroidKeystoreManager()

    @Test
    fun `encryptWithAuthenticatedCipher y decryptWithAuthenticatedCipher funcionan con cipher AES GCM real`() {
        val keyGen = KeyGenerator.getInstance("AES").apply { init(256) }
        val secretKey = keyGen.generateKey()

        val encryptCipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
        val dek = ByteArray(32) { (it + 10).toByte() }

        val wrappedPayload = keystoreManager.encryptWithAuthenticatedCipher(encryptCipher, dek)

        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, wrappedPayload.iv))
        }
        val unwrapped = keystoreManager.decryptWithAuthenticatedCipher(decryptCipher, wrappedPayload)

        assertArrayEquals(dek, unwrapped)
    }

    @Test
    fun `decryptWithAuthenticatedCipher lanza AuthenticationFailedException si se altera el ciphertext`() {
        val keyGen = KeyGenerator.getInstance("AES").apply { init(256) }
        val secretKey = keyGen.generateKey()

        val encryptCipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, secretKey)
        }
        val wrappedPayload = keystoreManager.encryptWithAuthenticatedCipher(
            encryptCipher,
            ByteArray(32) { 0x33 }
        )
        val tamperedCiphertext = wrappedPayload.ciphertext.copyOf().apply {
            this[0] = (this[0].toInt() xor 0xFF).toByte()
        }

        val decryptCipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, wrappedPayload.iv))
        }

        assertThrows(CryptoException.AuthenticationFailedException::class.java) {
            keystoreManager.decryptWithAuthenticatedCipher(
                decryptCipher,
                wrappedPayload.copy(ciphertext = tamperedCiphertext)
            )
        }
    }
}
