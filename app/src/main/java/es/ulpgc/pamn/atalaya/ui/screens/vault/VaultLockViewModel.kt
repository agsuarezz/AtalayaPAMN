package es.ulpgc.pamn.atalaya.ui.screens.vault

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import es.ulpgc.pamn.atalaya.domain.usecase.LockVaultUseCase
import javax.inject.Inject

/**
 * ViewModel temporal para el esqueleto de bóveda que garantiza la limpieza de la `DEK`
 * en memoria al pulsar «Bloquear bóveda».
 */
@HiltViewModel
class VaultLockViewModel @Inject constructor(
    private val lockVaultUseCase: LockVaultUseCase
) : ViewModel() {

    fun lockVault() {
        lockVaultUseCase()
    }
}
