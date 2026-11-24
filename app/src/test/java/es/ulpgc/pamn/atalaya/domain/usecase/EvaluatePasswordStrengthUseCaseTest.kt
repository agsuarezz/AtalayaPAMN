package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.domain.model.PasswordStrength
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EvaluatePasswordStrengthUseCaseTest {

    private val useCase = EvaluatePasswordStrengthUseCase()

    @Test
    fun `cadena vacia devuelve VERY_WEAK con score 0`() {
        val result = useCase("")

        assertEquals(PasswordStrength.VERY_WEAK, result.strength)
        assertEquals(0, result.score)
        assertFalse(result.meetsMinimumLength)
        assertEquals(0.0, result.estimatedEntropyBits, 0.001)
    }

    @Test
    fun `contrasena menor a 10 caracteres nunca alcanza MEDIUM aunque mezcle simbolos`() {
        val result = useCase("Ab1!xy9@Z")

        assertFalse(result.meetsMinimumLength)
        assertTrue(result.strength == PasswordStrength.VERY_WEAK || result.strength == PasswordStrength.WEAK)
        assertTrue(result.score <= 1)
    }

    @Test
    fun `contrasena de 10 minusculas cumple longitud minima y da MEDIUM`() {
        val result = useCase("abcdefghij")

        assertTrue(result.meetsMinimumLength)
        assertEquals(PasswordStrength.MEDIUM, result.strength)
        assertEquals(2, result.score)
    }

    @Test
    fun `contrasena de 12 caracteres mixtos alcanza STRONG`() {
        val result = useCase("AbcDef123456")

        assertTrue(result.meetsMinimumLength)
        assertEquals(PasswordStrength.STRONG, result.strength)
        assertEquals(3, result.score)
    }

    @Test
    fun `contrasena larga con todas las clases alcanza VERY_STRONG`() {
        val result = useCase("Atalaya#2026!Segura$")

        assertTrue(result.meetsMinimumLength)
        assertEquals(PasswordStrength.VERY_STRONG, result.strength)
        assertEquals(4, result.score)
        assertTrue(result.estimatedEntropyBits >= 80.0)
    }
}
