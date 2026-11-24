package es.ulpgc.pamn.atalaya.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import es.ulpgc.pamn.atalaya.core.security.AesGcmCipher
import es.ulpgc.pamn.atalaya.core.security.AndroidBiometricAvailabilityChecker
import es.ulpgc.pamn.atalaya.core.security.AndroidKeystoreManager
import es.ulpgc.pamn.atalaya.core.security.BiometricAvailabilityChecker
import es.ulpgc.pamn.atalaya.core.security.DefaultAesGcmCipher
import es.ulpgc.pamn.atalaya.core.security.KeyDerivation
import es.ulpgc.pamn.atalaya.core.security.KeystoreManager
import es.ulpgc.pamn.atalaya.core.security.Pbkdf2KeyDerivation
import javax.inject.Singleton

/** Módulo de Hilt para las primitivas criptográficas y el gestor del Keystore. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SecurityModule {

    @Binds
    @Singleton
    abstract fun bindKeyDerivation(
        implementation: Pbkdf2KeyDerivation
    ): KeyDerivation

    @Binds
    @Singleton
    abstract fun bindAesGcmCipher(
        implementation: DefaultAesGcmCipher
    ): AesGcmCipher

    @Binds
    @Singleton
    abstract fun bindKeystoreManager(
        implementation: AndroidKeystoreManager
    ): KeystoreManager

    @Binds
    @Singleton
    abstract fun bindBiometricAvailabilityChecker(
        implementation: AndroidBiometricAvailabilityChecker
    ): BiometricAvailabilityChecker
}
