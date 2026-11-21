package es.ulpgc.pamn.atalaya.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room para almacenar una credencial con todos sus campos sensibles cifrados
 * individualmente mediante AES-GCM.
 */
@Entity(tableName = "credentials")
data class CredentialEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    @ColumnInfo(name = "encrypted_title")
    val encryptedTitle: String,
    @ColumnInfo(name = "encrypted_username")
    val encryptedUsername: String,
    @ColumnInfo(name = "encrypted_password")
    val encryptedPassword: String,
    @ColumnInfo(name = "encrypted_url")
    val encryptedUrl: String,
    @ColumnInfo(name = "encrypted_notes")
    val encryptedNotes: String,
    @ColumnInfo(name = "encrypted_category")
    val encryptedCategory: String,
    @ColumnInfo(name = "created_at")
    val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at")
    val updatedAtEpochMillis: Long
)
