package es.ulpgc.pamn.atalaya.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.ulpgc.pamn.atalaya.core.dispatchers.DefaultDispatcherProvider
import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import javax.inject.Singleton

/** Módulo de Hilt para dependencias globales de la aplicación. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindDispatcherProvider(
        implementation: DefaultDispatcherProvider
    ): DispatcherProvider
}
