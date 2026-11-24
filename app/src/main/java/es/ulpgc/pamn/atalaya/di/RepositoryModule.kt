package es.ulpgc.pamn.atalaya.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.ulpgc.pamn.atalaya.data.repository.VaultMetaRepositoryImpl
import es.ulpgc.pamn.atalaya.data.repository.VaultRepositoryImpl
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import es.ulpgc.pamn.atalaya.domain.repository.VaultRepository
import javax.inject.Singleton

/** Módulo de Hilt que enlaza las interfaces de dominio con sus implementaciones de datos. */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindVaultRepository(
        implementation: VaultRepositoryImpl
    ): VaultRepository

    @Binds
    @Singleton
    abstract fun bindVaultMetaRepository(
        implementation: VaultMetaRepositoryImpl
    ): VaultMetaRepository
}
