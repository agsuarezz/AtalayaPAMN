package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.CryptoException
import es.ulpgc.pamn.atalaya.core.security.EncryptedPayload
import es.ulpgc.pamn.atalaya.core.security.KeystoreManager
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/** Resultado de preparar el descifrado biométrico antes de mostrar `BiometricPrompt`. */
sealed interface BiometricUnlockPreparation {
    data class Ready(val cipher: Cipher) : BiometricUnlockPreparation
    data object KeyInvalidated : BiometricUnlockPreparation
    data object NotConfigured : BiometricUnlockPreparation
    data class Error(val cause: Throwable? = null) : BiometricUnlockPreparation
}

/**
 * Inicializa el [Cipher] de descifrado con el IV de la `DEK` biométrica almacenada.
 * Si la clave hardware fue invalidada por cambios biométricos del sistema, limpia los restos
 * locales y devuelve [BiometricUnlockPreparation.KeyInvalidated].
 */
class PrepareBiometricUnlockUseCase @Inject constructor(
    private val vaultMetaRepository: VaultMetaRepository,
    private val keystoreManager: KeystoreManager,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(
        nowEpochMillis: Long = System.currentTimeMillis()
    ): BiometricUnlockPreparation = withContext(dispatchers.default) {
        try {
            val metadata = vaultMetaRepository.getMetadata()
                ?: return@withContext BiometricUnlockPreparation.NotConfigured
            val encodedWrappedDek = metadata.biometricWrappedDek
                ?: return@withContext BiometricUnlockPreparation.NotConfigured

            val payload = EncryptedPayload.fromStorageString(encodedWrappedDek)
            val cipher = keystoreManager.createBiometricDecryptCipher(payload.iv)
            BiometricUnlockPreparation.Ready(cipher)
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: CryptoException.KeyInvalidatedException) {
            try {
                keystoreManager.deleteBiometricKey()
            } catch (_: Exception) {
                // Ignorado: la clave ya estaba invalidada o ausente.
            }
            vaultMetaRepository.updateBiometricWrappedDek(
                biometricWrappedDek = null,
                updatedAtEpochMillis = nowEpochMillis
            )
            BiometricUnlockPreparation.KeyInvalidated
        } catch (e: CryptoException.CryptoOperationException) {
            BiometricUnlockPreparation.Error(e)
        } catch (e: IllegalArgumentException) {
            BiometricUnlockPreparation.Error(e)
        }
    }
}
