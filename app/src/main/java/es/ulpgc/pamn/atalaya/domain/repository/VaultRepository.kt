package es.ulpgc.pamn.atalaya.domain.repository

import es.ulpgc.pamn.atalaya.domain.model.Credential
import kotlinx.coroutines.flow.Flow

/** Contrato de acceso y persistencia cifrada de credenciales de la bóveda. */
interface VaultRepository {
    fun observeCredentials(): Flow<List<Credential>>
    suspend fun getCredentialById(id: Long): Credential?
    suspend fun saveCredential(credential: Credential): Long
    suspend fun deleteCredential(id: Long)
    suspend fun getAllCredentialsSnapshot(): List<Credential>
}
