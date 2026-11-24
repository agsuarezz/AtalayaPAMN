package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.AesGcmCipher
import es.ulpgc.pamn.atalaya.core.security.KeyDerivation
import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import es.ulpgc.pamn.atalaya.domain.model.MIN_MASTER_PASSWORD_LENGTH
import es.ulpgc.pamn.atalaya.domain.model.VaultMetadata
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import java.util.Base64
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Inicializa la bóveda generando una sal y una `DEK` aleatorias, derivando la KEK
 * mediante PBKDF2 y guardando la `DEK` envuelta con AES-GCM.
 */
class CreateMasterPasswordUseCase @Inject constructor(
    private val vaultMetaRepository: VaultMetaRepository,
    private val keyDerivation: KeyDerivation,
    private val aesGcmCipher: AesGcmCipher,
    private val vaultSessionManager: VaultSessionManager,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(
        password: CharArray,
        nowEpochMillis: Long = System.currentTimeMillis()
    ): Result<Unit> = withContext(dispatchers.default) {
        var kek: ByteArray? = null
        var dek: ByteArray? = null
        try {
            if (password.size < MIN_MASTER_PASSWORD_LENGTH) {
                return@withContext Result.failure(
                    IllegalArgumentException(
                        "La contraseña maestra debe tener al menos $MIN_MASTER_PASSWORD_LENGTH caracteres"
                    )
                )
            }
            if (vaultMetaRepository.isVaultInitialized()) {
                return@withContext Result.failure(
                    IllegalStateException("La bóveda ya está inicializada")
                )
            }

            val salt = keyDerivation.generateSalt()
            val generatedDek = keyDerivation.generateRandomKey()
            dek = generatedDek

            val derivedKek = keyDerivation.deriveKey(
                password = password,
                salt = salt,
                iterations = KeyDerivation.DEFAULT_ITERATIONS
            )
            kek = derivedKek

            val masterWrappedDek = aesGcmCipher.encrypt(
                plaintext = generatedDek,
                key = derivedKek,
                associatedData = MASTER_WRAP_AAD
            ).toStorageString()

            val metadata = VaultMetadata(
                saltBase64 = Base64.getEncoder().encodeToString(salt),
                kdfIterations = KeyDerivation.DEFAULT_ITERATIONS,
                masterWrappedDek = masterWrappedDek,
                biometricWrappedDek = null,
                createdAtEpochMillis = nowEpochMillis,
                updatedAtEpochMillis = nowEpochMillis
            )

            vaultMetaRepository.saveMetadata(metadata)
            vaultSessionManager.unlock(generatedDek)
            Result.success(Unit)
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            password.fill('\u0000')
            kek?.fill(0)
            dek?.fill(0)
        }
    }
}
