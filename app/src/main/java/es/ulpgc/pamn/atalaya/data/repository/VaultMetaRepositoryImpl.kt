package es.ulpgc.pamn.atalaya.data.repository

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.data.local.dao.VaultMetaDao
import es.ulpgc.pamn.atalaya.data.local.entity.VaultMetaEntity
import es.ulpgc.pamn.atalaya.domain.model.VaultMetadata
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/** Implementación de [VaultMetaRepository] respaldada por Room en `dispatchers.io`. */
@Singleton
class VaultMetaRepositoryImpl @Inject constructor(
    private val vaultMetaDao: VaultMetaDao,
    private val dispatchers: DispatcherProvider
) : VaultMetaRepository {

    override suspend fun isVaultInitialized(): Boolean = withContext(dispatchers.io) {
        vaultMetaDao.countMetadata() > 0
    }

    override suspend fun getMetadata(): VaultMetadata? = withContext(dispatchers.io) {
        vaultMetaDao.getMetadata()?.toDomain()
    }

    override suspend fun saveMetadata(metadata: VaultMetadata) = withContext(dispatchers.io) {
        vaultMetaDao.upsert(metadata.toEntity())
    }

    override suspend fun updateBiometricWrappedDek(
        biometricWrappedDek: String?,
        updatedAtEpochMillis: Long
    ) = withContext(dispatchers.io) {
        vaultMetaDao.updateBiometricWrappedDek(
            biometricWrappedDek = biometricWrappedDek,
            updatedAt = updatedAtEpochMillis
        )
    }

    override suspend fun clearMetadata() = withContext(dispatchers.io) {
        vaultMetaDao.deleteAll()
    }

    private fun VaultMetaEntity.toDomain(): VaultMetadata = VaultMetadata(
        saltBase64 = saltBase64,
        kdfIterations = kdfIterations,
        masterWrappedDek = masterWrappedDek,
        biometricWrappedDek = biometricWrappedDek,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )

    private fun VaultMetadata.toEntity(): VaultMetaEntity = VaultMetaEntity(
        id = VaultMetaEntity.SINGLETON_ID,
        saltBase64 = saltBase64,
        kdfIterations = kdfIterations,
        masterWrappedDek = masterWrappedDek,
        biometricWrappedDek = biometricWrappedDek,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis
    )
}
