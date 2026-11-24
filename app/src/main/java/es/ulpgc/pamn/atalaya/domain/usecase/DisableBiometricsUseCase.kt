package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.dispatchers.DispatcherProvider
import es.ulpgc.pamn.atalaya.core.security.KeystoreManager
import es.ulpgc.pamn.atalaya.domain.repository.VaultMetaRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext

/**
 * Elimina la clave biométrica del Android Keystore y limpia la `DEK` envuelta para biometría
 * en los metadatos de la bóveda.
 */
class DisableBiometricsUseCase @Inject constructor(
    private val keystoreManager: KeystoreManager,
    private val vaultMetaRepository: VaultMetaRepository,
    private val dispatchers: DispatcherProvider
) {

    suspend operator fun invoke(
        nowEpochMillis: Long = System.currentTimeMillis()
    ) = withContext(dispatchers.default) {
        try {
            keystoreManager.deleteBiometricKey()
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: Exception) {
            // Si el Keystore ya no contiene la clave o falla al borrarla, se limpia igualmente el metadato local.
        }
        vaultMetaRepository.updateBiometricWrappedDek(
            biometricWrappedDek = null,
            updatedAtEpochMillis = nowEpochMillis
        )
    }
}
