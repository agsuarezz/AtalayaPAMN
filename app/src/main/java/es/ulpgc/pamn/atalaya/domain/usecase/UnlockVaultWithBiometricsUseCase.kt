package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.CryptoException
import es.ulpgc.pamn.atalaya.core.security.EncryptedPayload
import es.ulpgc.pamn.atalaya.core.security.KeystoreManager
import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Descifra la `DEK` biométrica usando el [Cipher] autenticado por `BiometricPrompt`
 * y abre la sesión en [VaultSessionManager].
 */
class UnlockVaultWithBiometricsUseCase @Inject constructor(
    private val vaultMetaRepository: VaultMetaRepository,
    private val keystoreManager: KeystoreManager,
    private val vaultSessionManager: VaultSessionManager,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(authenticatedCipher: Cipher): UnlockResult =
        withContext(dispatchers.default) {
            var dek: ByteArray? = null
            try {
                val metadata = vaultMetaRepository.getMetadata()
                    ?: return@withContext UnlockResult.VaultNotInitialized
                val encodedWrappedDek = metadata.biometricWrappedDek
                    ?: return@withContext UnlockResult.CorruptedVault()

                val payload = EncryptedPayload.fromStorageString(encodedWrappedDek)
                val decryptedDek = keystoreManager.decryptWithAuthenticatedCipher(
                    cipher = authenticatedCipher,
                    payload = payload
                )
                dek = decryptedDek

                vaultSessionManager.unlock(decryptedDek)
                UnlockResult.Success
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: CryptoException.AuthenticationFailedException) {
                UnlockResult.InvalidPassword
            } catch (e: CryptoException.CryptoOperationException) {
                UnlockResult.CorruptedVault(e)
            } catch (e: IllegalArgumentException) {
                UnlockResult.CorruptedVault(e)
            } catch (e: Exception) {
                UnlockResult.CorruptedVault(e)
            } finally {
                dek?.fill(0)
            }
        }
}
