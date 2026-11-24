package es.ulpgc.pamn.atalaya.data.repository

import es.ulpgc.pamn.atalaya.core.security.AesGcmCipher
import es.ulpgc.pamn.atalaya.data.local.entity.CredentialEntity
import es.ulpgc.pamn.atalaya.domain.model.Credential
import es.ulpgc.pamn.atalaya.domain.model.CredentialCategory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Traduce entre el modelo de dominio [Credential] y la entidad cifrada [CredentialEntity],
 * vinculando cada columna mediante datos autenticados adicionales (AAD) para impedir el
 * intercambio silencioso de campos cifrados.
 */
@Singleton
class CredentialCryptoMapper @Inject constructor(
    private val cipher: AesGcmCipher
) {
    fun toEntity(credential: Credential, dek: ByteArray): CredentialEntity {
        return CredentialEntity(
            id = credential.id,
            encryptedTitle = cipher.encryptString(credential.title, dek, AAD_TITLE),
            encryptedUsername = cipher.encryptString(credential.username, dek, AAD_USERNAME),
            encryptedPassword = cipher.encryptString(credential.password, dek, AAD_PASSWORD),
            encryptedUrl = cipher.encryptString(credential.url, dek, AAD_URL),
            encryptedNotes = cipher.encryptString(credential.notes, dek, AAD_NOTES),
            encryptedCategory = cipher.encryptString(credential.category.name, dek, AAD_CATEGORY),
            createdAtEpochMillis = credential.createdAtEpochMillis,
            updatedAtEpochMillis = credential.updatedAtEpochMillis
        )
    }

    fun toDomain(entity: CredentialEntity, dek: ByteArray): Credential {
        val rawCategory = cipher.decryptString(entity.encryptedCategory, dek, AAD_CATEGORY)
        val category = CredentialCategory.entries.firstOrNull { it.name == rawCategory }
            ?: CredentialCategory.OTHER

        return Credential(
            id = entity.id,
            title = cipher.decryptString(entity.encryptedTitle, dek, AAD_TITLE),
            username = cipher.decryptString(entity.encryptedUsername, dek, AAD_USERNAME),
            password = cipher.decryptString(entity.encryptedPassword, dek, AAD_PASSWORD),
            url = cipher.decryptString(entity.encryptedUrl, dek, AAD_URL),
            notes = cipher.decryptString(entity.encryptedNotes, dek, AAD_NOTES),
            category = category,
            createdAtEpochMillis = entity.createdAtEpochMillis,
            updatedAtEpochMillis = entity.updatedAtEpochMillis
        )
    }

    private companion object {
        val AAD_TITLE = "credential:title".toByteArray(Charsets.UTF_8)
        val AAD_USERNAME = "credential:username".toByteArray(Charsets.UTF_8)
        val AAD_PASSWORD = "credential:password".toByteArray(Charsets.UTF_8)
        val AAD_URL = "credential:url".toByteArray(Charsets.UTF_8)
        val AAD_NOTES = "credential:notes".toByteArray(Charsets.UTF_8)
        val AAD_CATEGORY = "credential:category".toByteArray(Charsets.UTF_8)
    }
}
