package es.ulpgc.pamn.atalaya.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.ulpgc.pamn.atalaya.data.local.entity.VaultMetaEntity

@Dao
interface VaultMetaDao {

    @Query("SELECT * FROM vault_metadata WHERE id = 1")
    suspend fun getMetadata(): VaultMetaEntity?

    @Query("SELECT COUNT(*) FROM vault_metadata WHERE id = 1")
    suspend fun countMetadata(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: VaultMetaEntity)

    @Query("UPDATE vault_metadata SET biometric_wrapped_dek = :biometricWrappedDek, updated_at = :updatedAt WHERE id = 1")
    suspend fun updateBiometricWrappedDek(biometricWrappedDek: String?, updatedAt: Long)

    @Query("DELETE FROM vault_metadata")
    suspend fun deleteAll()
}
