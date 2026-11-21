package es.ulpgc.pamn.atalaya.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import es.ulpgc.pamn.atalaya.data.local.entity.CredentialEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CredentialDao {

    @Query("SELECT * FROM credentials ORDER BY updated_at DESC")
    fun observeAll(): Flow<List<CredentialEntity>>

    @Query("SELECT * FROM credentials ORDER BY updated_at DESC")
    suspend fun getAll(): List<CredentialEntity>

    @Query("SELECT * FROM credentials WHERE id = :id")
    suspend fun getById(id: Long): CredentialEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CredentialEntity): Long

    @Query("DELETE FROM credentials WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM credentials")
    suspend fun deleteAll()
}
