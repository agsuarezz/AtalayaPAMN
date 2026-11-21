package es.ulpgc.pamn.atalaya.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room singleton que persiste la sal KDF, iteraciones y las copias
 * envueltas de la `DEK` (por contraseña maestra y opcionalmente por biometría).
 */
@Entity(tableName = "vault_metadata")
data class VaultMetaEntity(
    @PrimaryKey
    val id: Int = SINGLETON_ID,
    @ColumnInfo(name = "salt_base64")
    val saltBase64: String,
    @ColumnInfo(name = "kdf_iterations")
    val kdfIterations: Int,
    @ColumnInfo(name = "master_wrapped_dek")
    val masterWrappedDek: String,
    @ColumnInfo(name = "biometric_wrapped_dek")
    val biometricWrappedDek: String?,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMillis: Long
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
