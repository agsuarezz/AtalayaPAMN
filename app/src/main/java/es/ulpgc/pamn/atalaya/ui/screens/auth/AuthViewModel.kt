package es.ulpgc.pamn.atalaya.ui.screens.auth

import androidx.biometric.BiometricPrompt
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import es.ulpgc.pamn.atalaya.R
import es.ulpgc.pamn.atalaya.core.security.BiometricAvailability
import es.ulpgc.pamn.atalaya.core.security.CryptoException
import es.ulpgc.pamn.atalaya.domain.model.MIN_MASTER_PASSWORD_LENGTH
import es.ulpgc.pamn.atalaya.domain.usecase.BiometricUnlockPreparation
import es.ulpgc.pamn.atalaya.domain.usecase.CompleteBiometricEnrollmentUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.CreateMasterPasswordUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.DisableBiometricsUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.EvaluatePasswordStrengthUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.GetAuthInitialStateUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.PrepareBiometricEnrollmentUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.PrepareBiometricUnlockUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.UnlockResult
import es.ulpgc.pamn.atalaya.domain.usecase.UnlockVaultWithBiometricsUseCase
import es.ulpgc.pamn.atalaya.domain.usecase.UnlockVaultWithPasswordUseCase
import es.ulpgc.pamn.atalaya.ui.state.UiState
import javax.crypto.Cipher
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de autenticación responsable del alta de contraseña maestra,
 * registro biométrico inicial y desbloqueo por contraseña o biometría Keystore.
 */
@HiltViewModel
class AuthViewModel @Inject constructor(
    private val getAuthInitialStateUseCase: GetAuthInitialStateUseCase,
    private val evaluatePasswordStrengthUseCase: EvaluatePasswordStrengthUseCase,
    private val createMasterPasswordUseCase: CreateMasterPasswordUseCase,
    private val unlockVaultWithPasswordUseCase: UnlockVaultWithPasswordUseCase,
    private val prepareBiometricEnrollmentUseCase: PrepareBiometricEnrollmentUseCase,
    private val completeBiometricEnrollmentUseCase: CompleteBiometricEnrollmentUseCase,
    private val prepareBiometricUnlockUseCase: PrepareBiometricUnlockUseCase,
    private val unlockVaultWithBiometricsUseCase: UnlockVaultWithBiometricsUseCase,
    private val disableBiometricsUseCase: DisableBiometricsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<AuthScreenData>>(UiState.Loading)
    val uiState: StateFlow<UiState<AuthScreenData>> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 4)
    val events: SharedFlow<AuthUiEvent> = _events.asSharedFlow()

    init {
        loadInitialState()
    }

    fun loadInitialState() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val info = getAuthInitialStateUseCase()
                var warningMessage: AuthMessage? = null
                if (info.isInitialized && info.hasStaleBiometricMetadata) {
                    disableBiometricsUseCase()
                    warningMessage = AuthMessage(
                        messageRes = R.string.auth_warning_biometric_invalidated,
                        type = AuthMessageType.WARNING
                    )
                }

                val canBiometrics = info.biometricAvailability == BiometricAvailability.AVAILABLE
                val data = if (!info.isInitialized) {
                    AuthScreenData(
                        mode = AuthMode.SETUP,
                        biometricAvailability = info.biometricAvailability,
                        isBiometricOptionEnabled = canBiometrics
                    )
                } else {
                    AuthScreenData(
                        mode = AuthMode.UNLOCK,
                        biometricAvailability = info.biometricAvailability,
                        canUnlockWithBiometrics = canBiometrics && info.isBiometricEnrolledInVault,
                        statusMessage = warningMessage
                    )
                }
                _uiState.value = UiState.Success(data)
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                _uiState.value = UiState.Error("")
            }
        }
    }

    fun onPasswordChanged(value: String) {
        updateSuccessState { current ->
            val strength = if (current.mode == AuthMode.SETUP) {
                evaluatePasswordStrengthUseCase(value)
            } else {
                current.passwordStrength
            }
            current.copy(
                password = value,
                passwordStrength = strength,
                passwordErrorRes = null,
                statusMessage = null
            )
        }
    }

    fun onConfirmPasswordChanged(value: String) {
        updateSuccessState { current ->
            current.copy(
                confirmPassword = value,
                confirmPasswordErrorRes = null,
                statusMessage = null
            )
        }
    }

    fun onTogglePasswordVisibility() {
        updateSuccessState { current ->
            current.copy(isPasswordVisible = !current.isPasswordVisible)
        }
    }

    fun onToggleConfirmPasswordVisibility() {
        updateSuccessState { current ->
            current.copy(isConfirmPasswordVisible = !current.isConfirmPasswordVisible)
        }
    }

    fun onBiometricOptionChanged(enabled: Boolean) {
        updateSuccessState { current ->
            if (current.biometricAvailability != BiometricAvailability.AVAILABLE) {
                current.copy(isBiometricOptionEnabled = false)
            } else {
                current.copy(isBiometricOptionEnabled = enabled)
            }
        }
    }

    fun onCreateVaultSubmit() {
        val current = currentSuccessData() ?: return
        if (current.mode != AuthMode.SETUP || current.isProcessing) return

        if (current.password.length < MIN_MASTER_PASSWORD_LENGTH) {
            updateSuccessState {
                it.copy(passwordErrorRes = R.string.auth_error_password_too_short)
            }
            return
        }
        if (current.password != current.confirmPassword) {
            updateSuccessState {
                it.copy(confirmPasswordErrorRes = R.string.auth_error_passwords_mismatch)
            }
            return
        }

        val enrollBiometrics = current.isBiometricOptionEnabled &&
            current.biometricAvailability == BiometricAvailability.AVAILABLE
        val passwordChars = current.password.toCharArray()

        viewModelScope.launch {
            updateSuccessState {
                it.copy(
                    isProcessing = true,
                    passwordErrorRes = null,
                    confirmPasswordErrorRes = null,
                    statusMessage = AuthMessage(
                        messageRes = R.string.auth_setup_processing,
                        type = AuthMessageType.INFO
                    )
                )
            }

            val createResult = createMasterPasswordUseCase(passwordChars)
            if (createResult.isFailure) {
                updateSuccessState {
                    it.copy(
                        isProcessing = false,
                        statusMessage = AuthMessage(
                            messageRes = R.string.auth_error_create_vault,
                            type = AuthMessageType.ERROR
                        )
                    )
                }
                return@launch
            }

            updateSuccessState {
                it.copy(
                    password = "",
                    confirmPassword = "",
                    isVaultCreatedPendingBiometrics = enrollBiometrics
                )
            }

            if (!enrollBiometrics) {
                updateSuccessState { it.copy(isProcessing = false, statusMessage = null) }
                _events.tryEmit(AuthUiEvent.NavigateToVault)
                return@launch
            }

            launchBiometricEnrollmentPrompt()
        }
    }

    fun onRetryBiometricEnrollment() {
        val current = currentSuccessData() ?: return
        if (!current.isVaultCreatedPendingBiometrics || current.isProcessing) return

        viewModelScope.launch {
            updateSuccessState { it.copy(isProcessing = true, statusMessage = null) }
            launchBiometricEnrollmentPrompt()
        }
    }

    fun onSkipBiometricEnrollment() {
        val current = currentSuccessData() ?: return
        if (!current.isVaultCreatedPendingBiometrics || current.isProcessing) return

        viewModelScope.launch {
            updateSuccessState { it.copy(isProcessing = true) }
            disableBiometricsUseCase()
            updateSuccessState {
                it.copy(
                    isProcessing = false,
                    isVaultCreatedPendingBiometrics = false,
                    statusMessage = null
                )
            }
            _events.tryEmit(AuthUiEvent.NavigateToVault)
        }
    }

    fun onUnlockWithPasswordSubmit() {
        val current = currentSuccessData() ?: return
        if (current.mode != AuthMode.UNLOCK || current.isProcessing) return

        if (current.password.isEmpty()) {
            updateSuccessState {
                it.copy(passwordErrorRes = R.string.auth_error_password_empty)
            }
            return
        }

        val passwordChars = current.password.toCharArray()
        viewModelScope.launch {
            updateSuccessState {
                it.copy(
                    isProcessing = true,
                    passwordErrorRes = null,
                    statusMessage = AuthMessage(
                        messageRes = R.string.unlock_processing,
                        type = AuthMessageType.INFO
                    )
                )
            }

            when (unlockVaultWithPasswordUseCase(passwordChars)) {
                UnlockResult.Success -> {
                    updateSuccessState {
                        it.copy(
                            password = "",
                            isProcessing = false,
                            statusMessage = null
                        )
                    }
                    _events.tryEmit(AuthUiEvent.NavigateToVault)
                }
                UnlockResult.InvalidPassword -> {
                    updateSuccessState {
                        it.copy(
                            isProcessing = false,
                            statusMessage = AuthMessage(
                                messageRes = R.string.auth_error_invalid_password,
                                type = AuthMessageType.ERROR
                            )
                        )
                    }
                }
                UnlockResult.VaultNotInitialized -> {
                    updateSuccessState {
                        it.copy(
                            isProcessing = false,
                            statusMessage = AuthMessage(
                                messageRes = R.string.auth_error_not_initialized,
                                type = AuthMessageType.ERROR
                            )
                        )
                    }
                }
                is UnlockResult.CorruptedVault -> {
                    updateSuccessState {
                        it.copy(
                            isProcessing = false,
                            statusMessage = AuthMessage(
                                messageRes = R.string.auth_error_corrupted_vault,
                                type = AuthMessageType.ERROR
                            )
                        )
                    }
                }
            }
        }
    }

    fun onUnlockWithBiometricsClicked() {
        val current = currentSuccessData() ?: return
        if (current.mode != AuthMode.UNLOCK || !current.canUnlockWithBiometrics || current.isProcessing) {
            return
        }

        viewModelScope.launch {
            updateSuccessState {
                it.copy(
                    isProcessing = true,
                    passwordErrorRes = null,
                    statusMessage = null
                )
            }

            when (val prep = prepareBiometricUnlockUseCase()) {
                is BiometricUnlockPreparation.Ready -> {
                    updateSuccessState { it.copy(isProcessing = false) }
                    _events.tryEmit(
                        AuthUiEvent.LaunchBiometricPrompt(
                            cipher = prep.cipher,
                            isEnrollment = false
                        )
                    )
                }
                BiometricUnlockPreparation.KeyInvalidated -> {
                    updateSuccessState {
                        it.copy(
                            isProcessing = false,
                            canUnlockWithBiometrics = false,
                            statusMessage = AuthMessage(
                                messageRes = R.string.auth_warning_biometric_invalidated,
                                type = AuthMessageType.WARNING
                            )
                        )
                    }
                }
                BiometricUnlockPreparation.NotConfigured -> {
                    updateSuccessState {
                        it.copy(
                            isProcessing = false,
                            canUnlockWithBiometrics = false,
                            statusMessage = AuthMessage(
                                messageRes = R.string.auth_warning_biometric_cancelled,
                                type = AuthMessageType.INFO
                            )
                        )
                    }
                }
                is BiometricUnlockPreparation.Error -> {
                    updateSuccessState {
                        it.copy(
                            isProcessing = false,
                            statusMessage = AuthMessage(
                                messageRes = R.string.auth_error_biometric_hardware,
                                type = AuthMessageType.ERROR
                            )
                        )
                    }
                }
            }
        }
    }

    fun onBiometricPromptSucceeded(authenticatedCipher: Cipher, isEnrollment: Boolean) {
        viewModelScope.launch {
            updateSuccessState { it.copy(isProcessing = true, statusMessage = null) }
            if (isEnrollment) {
                try {
                    completeBiometricEnrollmentUseCase(authenticatedCipher)
                    updateSuccessState {
                        it.copy(
                            isProcessing = false,
                            isVaultCreatedPendingBiometrics = false,
                            statusMessage = null
                        )
                    }
                    _events.tryEmit(AuthUiEvent.NavigateToVault)
                } catch (ce: CancellationException) {
                    throw ce
                } catch (_: Exception) {
                    disableBiometricsUseCase()
                    updateSuccessState {
                        it.copy(
                            isProcessing = false,
                            statusMessage = AuthMessage(
                                messageRes = R.string.auth_error_biometric_hardware,
                                type = AuthMessageType.ERROR
                            )
                        )
                    }
                }
            } else {
                when (unlockVaultWithBiometricsUseCase(authenticatedCipher)) {
                    UnlockResult.Success -> {
                        updateSuccessState {
                            it.copy(
                                password = "",
                                isProcessing = false,
                                statusMessage = null
                            )
                        }
                        _events.tryEmit(AuthUiEvent.NavigateToVault)
                    }
                    UnlockResult.InvalidPassword,
                    is UnlockResult.CorruptedVault -> {
                        updateSuccessState {
                            it.copy(
                                isProcessing = false,
                                statusMessage = AuthMessage(
                                    messageRes = R.string.auth_error_biometric_hardware,
                                    type = AuthMessageType.ERROR
                                )
                            )
                        }
                    }
                    UnlockResult.VaultNotInitialized -> {
                        updateSuccessState {
                            it.copy(
                                isProcessing = false,
                                statusMessage = AuthMessage(
                                    messageRes = R.string.auth_error_not_initialized,
                                    type = AuthMessageType.ERROR
                                )
                            )
                        }
                    }
                }
            }
        }
    }

    fun onBiometricPromptCancelled(isEnrollment: Boolean) {
        if (isEnrollment) {
            viewModelScope.launch { disableBiometricsUseCase() }
        }
        updateSuccessState { current ->
            current.copy(
                isProcessing = false,
                statusMessage = AuthMessage(
                    messageRes = if (isEnrollment) {
                        R.string.auth_warning_biometric_enroll_cancelled
                    } else {
                        R.string.auth_warning_biometric_cancelled
                    },
                    type = AuthMessageType.INFO
                )
            )
        }
    }

    fun onBiometricPromptError(errorCode: Int, isEnrollment: Boolean) {
        if (isEnrollment) {
            viewModelScope.launch { disableBiometricsUseCase() }
        }
        val (messageRes, messageType) = when (errorCode) {
            BiometricPrompt.ERROR_LOCKOUT,
            BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> {
                R.string.auth_warning_biometric_lockout to AuthMessageType.WARNING
            }
            else -> {
                val res = if (isEnrollment) {
                    R.string.auth_warning_biometric_enroll_cancelled
                } else {
                    R.string.auth_error_biometric_hardware
                }
                res to AuthMessageType.ERROR
            }
        }
        updateSuccessState { current ->
            current.copy(
                isProcessing = false,
                statusMessage = AuthMessage(
                    messageRes = messageRes,
                    type = messageType
                )
            )
        }
    }

    fun onBiometricHostUnavailable(isEnrollment: Boolean) {
        updateSuccessState { current ->
            current.copy(
                isProcessing = false,
                statusMessage = AuthMessage(
                    messageRes = if (isEnrollment) {
                        R.string.auth_warning_biometric_enroll_cancelled
                    } else {
                        R.string.auth_error_no_fragment_activity
                    },
                    type = AuthMessageType.ERROR
                )
            )
        }
    }

    private suspend fun launchBiometricEnrollmentPrompt() {
        try {
            val cipher = prepareBiometricEnrollmentUseCase()
            updateSuccessState { it.copy(isProcessing = false, statusMessage = null) }
            _events.tryEmit(
                AuthUiEvent.LaunchBiometricPrompt(
                    cipher = cipher,
                    isEnrollment = true
                )
            )
        } catch (ce: CancellationException) {
            throw ce
        } catch (_: CryptoException) {
            updateSuccessState {
                it.copy(
                    isProcessing = false,
                    statusMessage = AuthMessage(
                        messageRes = R.string.auth_warning_biometric_enroll_cancelled,
                        type = AuthMessageType.WARNING
                    )
                )
            }
        }
    }

    private fun currentSuccessData(): AuthScreenData? {
        return (_uiState.value as? UiState.Success)?.data
    }

    private inline fun updateSuccessState(transform: (AuthScreenData) -> AuthScreenData) {
        val current = _uiState.value
        if (current is UiState.Success) {
            _uiState.value = UiState.Success(transform(current.data))
        }
    }
}
