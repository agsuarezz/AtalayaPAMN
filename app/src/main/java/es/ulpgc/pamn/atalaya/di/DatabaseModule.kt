package es.ulpgc.pamn.atalaya.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import es.ulpgc.pamn.atalaya.data.local.dao.CredentialDao
import es.ulpgc.pamn.atalaya.data.local.dao.VaultMetaDao
import es.ulpgc.pamn.atalaya.data.local.db.AtalayaDatabase
import javax.inject.Singleton

/**
 * Módulo de Hilt para la base de datos Room. No habilita `fallbackToDestructiveMigration`
 * para evitar la pérdida silenciosa de credenciales si el esquema o la BD presentan corrupción.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAtalayaDatabase(
        @ApplicationContext context: Context
    ): AtalayaDatabase {
        return Room.databaseBuilder(
            context,
            AtalayaDatabase::class.java,
            AtalayaDatabase.DATABASE_NAME
        ).build()
    }

    @Provides
    fun provideCredentialDao(database: AtalayaDatabase): CredentialDao {
        return database.credentialDao()
    }

    @Provides
    fun provideVaultMetaDao(database: AtalayaDatabase): VaultMetaDao {
        return database.vaultMetaDao()
    }
}
