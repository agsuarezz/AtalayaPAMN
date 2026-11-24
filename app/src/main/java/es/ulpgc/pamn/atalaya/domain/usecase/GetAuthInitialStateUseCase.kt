package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.security.BiometricAvailability
import es.ulpgc.pamn.atalaya.core.security.BiometricAvailabilityChecker
import es.ulpgc.pamn.atalaya.core.security.CryptoException
import es.ulpgc.pamn.atalaya.core.security.KeystoreManager
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import javax.inject.Inject

/** Estado inicial de autenticación calculado a partir de BD, Keystore y hardware biométrico. */
data class AuthInitialInfo(
    val isInitialized: Boolean,
    val biometricAvailability: BiometricAvailability,
    val isBiometricEnrolledInVault: Boolean,
    val hasStaleBiometricMetadata: Boolean
)

/** Consulta si la bóveda existe y si la biometría hardware está disponible y configurada. */
class GetAuthInitialStateUseCase @Inject constructor(
    private val vaultMetaRepository: VaultMetaRepository,
    private val keystoreManager: KeystoreManager,
    private val biometricAvailabilityChecker: BiometricAvailabilityChecker
) {

    suspend operator fun invoke(): AuthInitialInfo {
        val metadata = vaultMetaRepository.getMetadata()
        val availability = biometricAvailabilityChecker.checkAvailability()
        val hasMetadataWrappedDek = metadata?.biometricWrappedDek != null
        val hasKeystoreKey = if (hasMetadataWrappedDek) {
            try {
                keystoreManager.hasBiometricKey()
            } catch (_: CryptoException) {
                false
            }
        } else {
            false
        }

        val isEnrolled = hasMetadataWrappedDek && hasKeystoreKey
        val hasStaleMetadata = hasMetadataWrappedDek && !hasKeystoreKey

        return AuthInitialInfo(
            isInitialized = metadata != null,
            biometricAvailability = availability,
            isBiometricEnrolledInVault = isEnrolled,
            hasStaleBiometricMetadata = hasStaleMetadata
        )
    }
}
