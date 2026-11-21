package es.ulpgc.pamn.atalaya.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import es.ulpgc.pamn.atalaya.data.local.dao.CredentialDao
import es.ulpgc.pamn.atalaya.data.local.dao.VaultMetaDao
import es.ulpgc.pamn.atalaya.data.local.entity.CredentialEntity
import es.ulpgc.pamn.atalaya.data.local.entity.VaultMetaEntity

@Database(
    entities = [
        CredentialEntity::class,
        VaultMetaEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AtalayaDatabase : RoomDatabase() {
    abstract fun credentialDao(): CredentialDao
    abstract fun vaultMetaDao(): VaultMetaDao

    companion object {
        const val DATABASE_NAME = "atalaya_vault.db"
    }
}
