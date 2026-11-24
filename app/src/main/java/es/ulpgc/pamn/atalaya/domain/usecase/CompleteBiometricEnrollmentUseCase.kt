package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.KeystoreManager
import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.withContext

/**
 * Envuelve la `DEK` activa de sesión con el [Cipher] ya autenticado por `BiometricPrompt`
 * y persiste el bloque resultante en [VaultMetaRepository].
 */
class CompleteBiometricEnrollmentUseCase @Inject constructor(
    private val keystoreManager: KeystoreManager,
    private val vaultSessionManager: VaultSessionManager,
    private val vaultMetaRepository: VaultMetaRepository,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(
        authenticatedCipher: Cipher,
        nowEpochMillis: Long = System.currentTimeMillis()
    ) = withContext(dispatchers.default) {
        val wrappedDek = vaultSessionManager.withDek { dek ->
            keystoreManager.encryptWithAuthenticatedCipher(authenticatedCipher, dek).toStorageString()
        }
        vaultMetaRepository.updateBiometricWrappedDek(
            biometricWrappedDek = wrappedDek,
            updatedAtEpochMillis = nowEpochMillis
        )
    }
}
