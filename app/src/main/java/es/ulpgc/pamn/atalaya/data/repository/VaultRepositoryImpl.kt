package es.ulpgc.pamn.atalaya.data.repository

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.CryptoException
import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import es.ulpgc.pamn.atalaya.data.local.dao.CredentialDao
import es.ulpgc.pamn.atalaya.domain.model.Credential
import es.ulpgc.pamn.atalaya.domain.repository.VaultRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Implementación de [VaultRepository] que mantiene los registros cifrados en Room y
 * únicamente los descifra en memoria mientras la sesión en [VaultSessionManager] está abierta.
 */
@Singleton
class VaultRepositoryImpl @Inject constructor(
    private val credentialDao: CredentialDao,
    private val mapper: CredentialCryptoMapper,
    private val sessionManager: VaultSessionManager,
    private val dispatchers: DispatcherProvider
) : VaultRepository {

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeCredentials(): Flow<List<Credential>> {
        return sessionManager.isUnlocked.flatMapLatest { unlocked ->
            if (!unlocked) {
                flowOf(emptyList())
            } else {
                credentialDao.observeAll().map { entities ->
                    withContext(dispatchers.default) {
                        try {
                            sessionManager.withDek { dek ->
                                entities.map { entity -> mapper.toDomain(entity, dek) }
                            }
                        } catch (_: CryptoException.VaultLockedException) {
                            emptyList()
                        }
                    }
                }
            }
        }
    }

    override suspend fun getCredentialById(id: Long): Credential? = withContext(dispatchers.io) {
        sessionManager.withDek { }
        val entity = credentialDao.getById(id) ?: return@withContext null
        withContext(dispatchers.default) {
            sessionManager.withDek { dek -> mapper.toDomain(entity, dek) }
        }
    }

    override suspend fun saveCredential(credential: Credential): Long = withContext(dispatchers.io) {
        val encryptedEntity = withContext(dispatchers.default) {
            sessionManager.withDek { dek -> mapper.toEntity(credential, dek) }
        }
        credentialDao.upsert(encryptedEntity)
    }

    override suspend fun deleteCredential(id: Long) = withContext(dispatchers.io) {
        sessionManager.withDek { }
        credentialDao.deleteById(id)
    }

    override suspend fun getAllCredentialsSnapshot(): List<Credential> = withContext(dispatchers.io) {
        sessionManager.withDek { }
        val entities = credentialDao.getAll()
        withContext(dispatchers.default) {
            sessionManager.withDek { dek ->
                entities.map { entity -> mapper.toDomain(entity, dek) }
            }
        }
    }
}
