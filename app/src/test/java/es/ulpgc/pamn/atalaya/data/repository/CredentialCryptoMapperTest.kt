package es.ulpgc.pamn.atalaya.data.repository

import es.ulpgc.pamn.atalaya.core.security.CryptoException
import es.ulpgc.pamn.atalaya.core.security.DefaultAesGcmCipher
import es.ulpgc.pamn.atalaya.core.security.Pbkdf2KeyDerivation
import es.ulpgc.pamn.atalaya.domain.model.Credential
import es.ulpgc.pamn.atalaya.domain.model.CredentialCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CredentialCryptoMapperTest {

    private val mapper = CredentialCryptoMapper(DefaultAesGcmCipher())
    private val keyDerivation = Pbkdf2KeyDerivation()

    @Test
    fun `toEntity cifra todos los campos sensibles y toDomain los recupera integros`() {
        val dek = keyDerivation.generateRandomKey()
        val original = Credential(
            id = 7L,
            title = "Banco ULPGC",
            username = "alvaro.garcia@ulpgc.es",
            password = "ClaveSuperSecreta#99!",
            url = "https://banco.ejemplo.es",
            notes = "PIN tarjeta secundario: 4829",
            category = CredentialCategory.BANKING,
            createdAtEpochMillis = 1_700_000_000_000L,
            updatedAtEpochMillis = 1_700_000_050_000L
        )

        val entity = mapper.toEntity(original, dek)

        assertEquals(original.id, entity.id)
        assertFalse(entity.encryptedTitle.contains(original.title))
        assertFalse(entity.encryptedUsername.contains(original.username))
        assertFalse(entity.encryptedPassword.contains(original.password))
        assertFalse(entity.encryptedUrl.contains(original.url))
        assertFalse(entity.encryptedNotes.contains(original.notes))
        assertFalse(entity.encryptedCategory.contains(original.category.name))

        val restored = mapper.toDomain(entity, dek)
        assertEquals(original, restored)
    }

    @Test
    fun `intercambiar columnas cifradas falla autenticacion gracias al AAD por columna`() {
        val dek = keyDerivation.generateRandomKey()
        val original = Credential(
            id = 1L,
            title = "Correo",
            username = "usuario_real",
            password = "password_real",
            createdAtEpochMillis = 100L,
            updatedAtEpochMillis = 200L
        )

        val entity = mapper.toEntity(original, dek)
        val swappedEntity = entity.copy(encryptedPassword = entity.encryptedUsername)

        assertThrows(CryptoException.AuthenticationFailedException::class.java) {
            mapper.toDomain(swappedEntity, dek)
        }
    }

    @Test
    fun `toString de Credential no expone titulo usuario ni contrasena`() {
        val credential = Credential(
            id = 12L,
            title = "TituloPrivado",
            username = "usuario_privado",
            password = "PasswordSecreto123!",
            url = "https://privado.test",
            notes = "Nota privada",
            category = CredentialCategory.WORK,
            createdAtEpochMillis = 10L,
            updatedAtEpochMillis = 20L
        )

        val rendered = credential.toString()

        assertFalse(rendered.contains("TituloPrivado"))
        assertFalse(rendered.contains("usuario_privado"))
        assertFalse(rendered.contains("PasswordSecreto123!"))
        assertFalse(rendered.contains("privado.test"))
        assertFalse(rendered.contains("Nota privada"))
        assertTrue(rendered.contains("id=12"))
        assertTrue(rendered.contains("WORK"))
    }
}
