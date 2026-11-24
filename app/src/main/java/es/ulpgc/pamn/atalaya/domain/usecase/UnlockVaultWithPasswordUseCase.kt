package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.AesGcmCipher
import es.ulpgc.pamn.atalaya.core.security.CryptoException
import es.ulpgc.pamn.atalaya.core.security.EncryptedPayload
import es.ulpgc.pamn.atalaya.core.security.KeyDerivation
import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/** Resultado del intento de desbloqueo de la bóveda. */
sealed interface UnlockResult {
    data object Success : UnlockResult
    data object InvalidPassword : UnlockResult
    data object VaultNotInitialized : UnlockResult
    data class CorruptedVault(val cause: Throwable? = null) : UnlockResult
}

/**
 * Desbloquea la bóveda derivando la KEK con PBKDF2 a partir de la contraseña maestra
 * y descifrando la `DEK` almacenada en [VaultMetaRepository].
 */
class UnlockVaultWithPasswordUseCase @Inject constructor(
    private val vaultMetaRepository: VaultMetaRepository,
    private val keyDerivation: KeyDerivation,
    private val aesGcmCipher: AesGcmCipher,
    private val vaultSessionManager: VaultSessionManager,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(password: CharArray): UnlockResult = withContext(dispatchers.default) {
        var kek: ByteArray? = null
        var dek: ByteArray? = null
        try {
            if (password.isEmpty()) {
                return@withContext UnlockResult.InvalidPassword
            }

            val metadata = vaultMetaRepository.getMetadata()
                ?: return@withContext UnlockResult.VaultNotInitialized

            val salt = try {
                Base64.getDecoder().decode(metadata.saltBase64)
            } catch (e: IllegalArgumentException) {
                return@withContext UnlockResult.CorruptedVault(e)
            }

            if (salt.size < KeyDerivation.MIN_SALT_SIZE ||
                metadata.kdfIterations < KeyDerivation.MIN_ITERATIONS
            ) {
                return@withContext UnlockResult.CorruptedVault()
            }

            val wrappedPayload = try {
                EncryptedPayload.fromStorageString(metadata.masterWrappedDek)
            } catch (e: CryptoException.CryptoOperationException) {
                return@withContext UnlockResult.CorruptedVault(e)
            }

            val derivedKek = keyDerivation.deriveKey(
                password = password,
                salt = salt,
                iterations = metadata.kdfIterations
            )
            kek = derivedKek

            val decryptedDek = aesGcmCipher.decrypt(
                payload = wrappedPayload,
                key = derivedKek,
                associatedData = MASTER_WRAP_AAD
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
            password.fill('\u0000')
            kek?.fill(0)
            dek?.fill(0)
        }
    }
}
