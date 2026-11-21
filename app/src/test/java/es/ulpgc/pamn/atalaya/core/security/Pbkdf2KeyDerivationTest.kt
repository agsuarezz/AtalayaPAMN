package es.ulpgc.pamn.atalaya.core.security

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Test

class Pbkdf2KeyDerivationTest {

    private val keyDerivation = Pbkdf2KeyDerivation()

    @Test
    fun `deriveKey es determinista para misma clave sal e iteraciones`() {
        val salt = keyDerivation.generateSalt()
        val key1 = keyDerivation.deriveKey("ClaveMaestra#2026".toCharArray(), salt, iterations = 1_000)
        val key2 = keyDerivation.deriveKey("ClaveMaestra#2026".toCharArray(), salt, iterations = 1_000)

        assertEquals(KeyDerivation.DEFAULT_KEY_SIZE, key1.size)
        assertArrayEquals(key1, key2)
    }

    @Test
    fun `deriveKey produce claves distintas con distinta sal o distinta clave`() {
        val saltA = keyDerivation.generateSalt()
        val saltB = keyDerivation.generateSalt()

        val k1 = keyDerivation.deriveKey("ClaveMaestra#2026".toCharArray(), saltA, iterations = 1_000)
        val k2 = keyDerivation.deriveKey("ClaveMaestra#2026".toCharArray(), saltB, iterations = 1_000)
        val k3 = keyDerivation.deriveKey("OtraClaveDistinta#2026".toCharArray(), saltA, iterations = 1_000)

        assertFalse(k1.contentEquals(k2))
        assertFalse(k1.contentEquals(k3))
    }

    @Test
    fun `deriveKey funciona con las 310000 iteraciones por defecto`() {
        val salt = keyDerivation.generateSalt()
        val derived = keyDerivation.deriveKey("MaestraRobusta!99".toCharArray(), salt)

        assertEquals(KeyDerivation.DEFAULT_KEY_SIZE, derived.size)
    }

    @Test
    fun `generateSalt y generateRandomKey generan bloques aleatorios del tamano esperado`() {
        val salt1 = keyDerivation.generateSalt()
        val salt2 = keyDerivation.generateSalt()
        val key1 = keyDerivation.generateRandomKey()
        val key2 = keyDerivation.generateRandomKey()

        assertEquals(KeyDerivation.DEFAULT_SALT_SIZE, salt1.size)
        assertEquals(KeyDerivation.DEFAULT_KEY_SIZE, key1.size)
        assertFalse(salt1.contentEquals(salt2))
        assertFalse(key1.contentEquals(key2))
    }

    @Test
    fun `validaciones rechazan entradas inseguras o vacias`() {
        val validSalt = ByteArray(KeyDerivation.MIN_SALT_SIZE) { 1 }

        assertThrows(IllegalArgumentException::class.java) {
            keyDerivation.generateSalt(sizeBytes = 8)
        }
        assertThrows(IllegalArgumentException::class.java) {
            keyDerivation.generateRandomKey(sizeBytes = 16)
        }
        assertThrows(IllegalArgumentException::class.java) {
            keyDerivation.deriveKey(charArrayOf(), validSalt)
        }
        assertThrows(IllegalArgumentException::class.java) {
            keyDerivation.deriveKey("abc".toCharArray(), ByteArray(8))
        }
        assertThrows(IllegalArgumentException::class.java) {
            keyDerivation.deriveKey("abc".toCharArray(), validSalt, iterations = 500)
        }
    }
}
