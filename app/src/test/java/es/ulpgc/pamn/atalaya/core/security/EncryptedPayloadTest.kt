package es.ulpgc.pamn.atalaya.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedPayloadTest {

    @Test
    fun `toStorageString y fromStorageString conservan iv y ciphertext exactos`() {
        val iv = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12)
        val ciphertext = byteArrayOf(42, 99, -12, 0, 17, 88)
        val original = EncryptedPayload(iv = iv, ciphertext = ciphertext)

        val encoded = original.toStorageString()
        val decoded = EncryptedPayload.fromStorageString(encoded)

        assertTrue(encoded.startsWith("v1:"))
        assertArrayEquals(original.iv, decoded.iv)
        assertArrayEquals(original.ciphertext, decoded.ciphertext)
        assertEquals(original, decoded)
        assertEquals(original.hashCode(), decoded.hashCode())
    }

    @Test
    fun `fromStorageString rechaza formato o version invalidos`() {
        assertThrows(CryptoException.CryptoOperationException::class.java) {
            EncryptedPayload.fromStorageString("v2:AQID:BAUG")
        }
        assertThrows(CryptoException.CryptoOperationException::class.java) {
            EncryptedPayload.fromStorageString("sin_separadores")
        }
        assertThrows(CryptoException.CryptoOperationException::class.java) {
            EncryptedPayload.fromStorageString("v1:no_es_base64***:BAUG")
        }
        assertThrows(CryptoException.CryptoOperationException::class.java) {
            EncryptedPayload.fromStorageString("v1::BAUG")
        }
    }

    @Test
    fun `toString no expone bytes sensibles en texto ni base64`() {
        val payload = EncryptedPayload(
            iv = byteArrayOf(10, 20, 30, 40, 50, 60, 70, 80, 90, 100, 110, 120),
            ciphertext = "secreto_no_visible".toByteArray()
        )
        val rendered = payload.toString()

        assertFalse(rendered.contains("secreto_no_visible"))
        assertTrue(rendered.contains("ivBytes=12"))
        assertTrue(rendered.contains("ciphertextBytes=${payload.ciphertext.size}"))
    }

    @Test
    fun `equals distingue payloads con distinto contenido`() {
        val a = EncryptedPayload(byteArrayOf(1, 2, 3), byteArrayOf(4, 5))
        val b = EncryptedPayload(byteArrayOf(1, 2, 3), byteArrayOf(4, 6))

        assertNotEquals(a, b)
    }
}
