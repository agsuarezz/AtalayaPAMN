package es.ulpgc.pamn.atalaya.ui.screens.auth

import androidx.annotation.StringRes
import es.ulpgc.pamn.atalaya.core.security.BiometricAvailability
import es.ulpgc.pamn.atalaya.domain.model.PasswordStrength
import es.ulpgc.pamn.atalaya.domain.model.PasswordStrengthResult
import javax.crypto.Cipher

/** Modo de la pantalla de autenticación según el estado de la bóveda. */
enum class AuthMode {
    SETUP,
    UNLOCK
}

/** Nivel visual y semántico de un mensaje de estado en autenticación. */
enum class AuthMessageType {
    INFO,
    WARNING,
    ERROR
}

/** Mensaje de estado parametrizado por recurso de string para mantener i18n en `strings.xml`. */
data class AuthMessage(
    @param:StringRes val messageRes: Int,
    val type: AuthMessageType
)

/** Estado completo de formulario y controles de la pantalla de autenticación. */
data class AuthScreenData(
    val mode: AuthMode,
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val passwordStrength: PasswordStrengthResult = PasswordStrengthResult(
        strength = PasswordStrength.VERY_WEAK,
        score = 0,
        estimatedEntropyBits = 0.0,
        meetsMinimumLength = false
    ),
    @param:StringRes val passwordErrorRes: Int? = null,
    @param:StringRes val confirmPasswordErrorRes: Int? = null,
    val biometricAvailability: BiometricAvailability = BiometricAvailability.UNAVAILABLE,
    val isBiometricOptionEnabled: Boolean = false,
    val canUnlockWithBiometrics: Boolean = false,
    val isVaultCreatedPendingBiometrics: Boolean = false,
    val isProcessing: Boolean = false,
    val statusMessage: AuthMessage? = null
)

/** Eventos puntuales emitidos por [AuthViewModel] hacia la UI. */
sealed interface AuthUiEvent {
    data object NavigateToVault : AuthUiEvent
    data class LaunchBiometricPrompt(
        val cipher: Cipher,
        val isEnrollment: Boolean
    ) : AuthUiEvent
}
