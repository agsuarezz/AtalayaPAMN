package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.CryptoException
import es.ulpgc.pamn.atalaya.core.security.KeystoreManager
import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.withContext

/**
 * Genera la clave hardware biométrica en Android Keystore y prepara el [Cipher]
 * en modo cifrado para ser autenticado con `BiometricPrompt.CryptoObject`.
 */
class PrepareBiometricEnrollmentUseCase @Inject constructor(
    private val keystoreManager: KeystoreManager,
    private val vaultSessionManager: VaultSessionManager,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(): Cipher = withContext(dispatchers.default) {
        if (!vaultSessionManager.isUnlocked.value) {
            throw CryptoException.VaultLockedException()
        }
        keystoreManager.generateBiometricKey()
        keystoreManager.createBiometricEncryptCipher()
    }
}
