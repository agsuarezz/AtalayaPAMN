package es.ulpgc.pamn.atalaya.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import es.ulpgc.pamn.atalaya.R
import es.ulpgc.pamn.atalaya.core.security.BiometricPromptHelper
import es.ulpgc.pamn.atalaya.core.security.findFragmentActivity
import es.ulpgc.pamn.atalaya.ui.components.RetroButton
import es.ulpgc.pamn.atalaya.ui.components.RetroButtonVariant
import es.ulpgc.pamn.atalaya.ui.components.RetroCard
import es.ulpgc.pamn.atalaya.ui.components.RetroTopBar
import es.ulpgc.pamn.atalaya.ui.state.UiState

/**
 * Pantalla principal de autenticación de Atalaya: gestiona tanto el alta inicial
 * de contraseña maestra como el desbloqueo por contraseña o biometría Keystore.
 */
@Composable
fun AuthScreen(
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val enrollTitle = stringResource(R.string.biometric_prompt_enroll_title)
    val enrollSubtitle = stringResource(R.string.biometric_prompt_enroll_subtitle)
    val unlockTitle = stringResource(R.string.biometric_prompt_unlock_title)
    val unlockSubtitle = stringResource(R.string.biometric_prompt_unlock_subtitle)
    val cancelLabel = stringResource(R.string.biometric_prompt_cancel)

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                AuthUiEvent.NavigateToVault -> onUnlockSuccess()
                is AuthUiEvent.LaunchBiometricPrompt -> {
                    val activity = context.findFragmentActivity()
                    if (activity == null) {
                        viewModel.onBiometricHostUnavailable(event.isEnrollment)
                    } else {
                        BiometricPromptHelper.authenticateWithCrypto(
                            activity = activity,
                            cipher = event.cipher,
                            title = if (event.isEnrollment) enrollTitle else unlockTitle,
                            subtitle = if (event.isEnrollment) enrollSubtitle else unlockSubtitle,
                            negativeButtonText = cancelLabel,
                            onSuccess = { authenticatedCipher ->
                                viewModel.onBiometricPromptSucceeded(
                                    authenticatedCipher = authenticatedCipher,
                                    isEnrollment = event.isEnrollment
                                )
                            },
                            onCancelled = {
                                viewModel.onBiometricPromptCancelled(event.isEnrollment)
                            },
                            onError = { errorCode, _ ->
                                viewModel.onBiometricPromptError(
                                    errorCode = errorCode,
                                    isEnrollment = event.isEnrollment
                                )
                            }
                        )
                    }
                }
            }
        }
    }

    AuthScreenContent(
        uiState = uiState,
        onRetryLoad = viewModel::loadInitialState,
        onPasswordChanged = viewModel::onPasswordChanged,
        onConfirmPasswordChanged = viewModel::onConfirmPasswordChanged,
        onTogglePasswordVisibility = viewModel::onTogglePasswordVisibility,
        onToggleConfirmPasswordVisibility = viewModel::onToggleConfirmPasswordVisibility,
        onBiometricOptionChanged = viewModel::onBiometricOptionChanged,
        onCreateVaultSubmit = viewModel::onCreateVaultSubmit,
        onRetryBiometricEnrollment = viewModel::onRetryBiometricEnrollment,
        onSkipBiometricEnrollment = viewModel::onSkipBiometricEnrollment,
        onUnlockWithPasswordSubmit = viewModel::onUnlockWithPasswordSubmit,
        onUnlockWithBiometricsClicked = viewModel::onUnlockWithBiometricsClicked,
        modifier = modifier
    )
}

/** Composable sin estado de [AuthScreen] para facilitar pruebas de UI y previsualizaciones. */
@Composable
fun AuthScreenContent(
    uiState: UiState<AuthScreenData>,
    onRetryLoad: () -> Unit,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onBiometricOptionChanged: (Boolean) -> Unit,
    onCreateVaultSubmit: () -> Unit,
    onRetryBiometricEnrollment: () -> Unit,
    onSkipBiometricEnrollment: () -> Unit,
    onUnlockWithPasswordSubmit: () -> Unit,
    onUnlockWithBiometricsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val topBarTitle = when (uiState) {
        is UiState.Success -> {
            if (uiState.data.mode == AuthMode.SETUP) {
                stringResource(R.string.auth_setup_title)
            } else {
                stringResource(R.string.unlock_title)
            }
        }
        UiState.Loading, is UiState.Error -> stringResource(R.string.unlock_title)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            RetroTopBar(title = topBarTitle)
        }
    ) { innerPadding ->
        when (uiState) {
            UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }

            is UiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RetroCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = MaterialTheme.colorScheme.error
                    ) {
                        Text(
                            text = stringResource(R.string.auth_error_load_state),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        if (uiState.message.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = uiState.message,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    RetroButton(
                        text = stringResource(R.string.auth_action_retry),
                        onClick = onRetryLoad,
                        variant = RetroButtonVariant.PRIMARY,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            is UiState.Success -> {
                val data = uiState.data
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    when (data.mode) {
                        AuthMode.SETUP -> MasterPasswordSetupContent(
                            data = data,
                            onPasswordChanged = onPasswordChanged,
                            onConfirmPasswordChanged = onConfirmPasswordChanged,
                            onTogglePasswordVisibility = onTogglePasswordVisibility,
                            onToggleConfirmPasswordVisibility = onToggleConfirmPasswordVisibility,
                            onBiometricOptionChanged = onBiometricOptionChanged,
                            onCreateVaultSubmit = onCreateVaultSubmit,
                            onRetryBiometricEnrollment = onRetryBiometricEnrollment,
                            onSkipBiometricEnrollment = onSkipBiometricEnrollment
                        )

                        AuthMode.UNLOCK -> VaultUnlockContent(
                            data = data,
                            onPasswordChanged = onPasswordChanged,
                            onTogglePasswordVisibility = onTogglePasswordVisibility,
                            onUnlockWithPasswordSubmit = onUnlockWithPasswordSubmit,
                            onUnlockWithBiometricsClicked = onUnlockWithBiometricsClicked
                        )
                    }
                }
            }
        }
    }
}
