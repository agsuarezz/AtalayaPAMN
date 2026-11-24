package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.core.security.VaultSessionManager
import javax.inject.Inject

/** Bloquea inmediatamente la bóveda limpiando la `DEK` de la memoria de sesión. */
class LockVaultUseCase @Inject constructor(
    private val vaultSessionManager: VaultSessionManager
) {
    operator fun invoke() {
        vaultSessionManager.lock()
    }
}
