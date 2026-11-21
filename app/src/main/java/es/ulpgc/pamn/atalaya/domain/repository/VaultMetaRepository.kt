package es.ulpgc.pamn.atalaya.domain.repository

import es.ulpgc.pamn.atalaya.domain.model.VaultMetadata

/** Contrato para gestionar los metadatos de inicialización, KDF y DEK envuelta de la bóveda. */
interface VaultMetaRepository {
    suspend fun isVaultInitialized(): Boolean
    suspend fun getMetadata(): VaultMetadata?
    suspend fun saveMetadata(metadata: VaultMetadata)
    suspend fun updateBiometricWrappedDek(biometricWrappedDek: String?, updatedAtEpochMillis: Long)
    suspend fun clearMetadata()
}
