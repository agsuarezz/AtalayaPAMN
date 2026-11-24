package es.ulpgc.pamn.atalaya.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.atalaya.R
import es.ulpgc.pamn.atalaya.core.security.BiometricAvailability
import es.ulpgc.pamn.atalaya.ui.components.RetroButton
import es.ulpgc.pamn.atalaya.ui.components.RetroButtonVariant
import es.ulpgc.pamn.atalaya.ui.components.RetroCard
import es.ulpgc.pamn.atalaya.ui.components.RetroTextField

/** Contenido de la pantalla para desbloquear una bóveda ya inicializada. */
@Composable
fun VaultUnlockContent(
    data: AuthScreenData,
    onPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onUnlockWithPasswordSubmit: () -> Unit,
    onUnlockWithBiometricsClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.unlock_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = colorScheme.onSurface
            )
            val biometricNoticeRes = when {
                data.canUnlockWithBiometrics -> null
                data.biometricAvailability == BiometricAvailability.NOT_ENROLLED ->
                    R.string.unlock_biometric_not_enrolled_notice
                data.biometricAvailability == BiometricAvailability.UNAVAILABLE ->
                    R.string.unlock_biometric_unavailable_notice
                else -> null
            }
            if (biometricNoticeRes != null) {
                Text(
                    text = stringResource(biometricNoticeRes),
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }

        data.statusMessage?.let { status ->
            AuthStatusBanner(status = status)
        }

        RetroTextField(
            value = data.password,
            onValueChange = onPasswordChanged,
            label = stringResource(R.string.unlock_master_label),
            placeholder = stringResource(R.string.unlock_master_placeholder),
            errorMessage = data.passwordErrorRes?.let { stringResource(it) },
            enabled = !data.isProcessing,
            visualTransformation = if (data.isPasswordVisible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
                autoCorrectEnabled = false
            ),
            keyboardActions = KeyboardActions(onDone = { onUnlockWithPasswordSubmit() }),
            trailingIcon = {
                PasswordVisibilityToggleButton(
                    isVisible = data.isPasswordVisible,
                    onToggle = onTogglePasswordVisibility
                )
            },
            modifier = Modifier.fillMaxWidth()
        )

        RetroButton(
            text = stringResource(R.string.unlock_action_enter),
            onClick = onUnlockWithPasswordSubmit,
            enabled = !data.isProcessing,
            variant = RetroButtonVariant.PRIMARY,
            modifier = Modifier.fillMaxWidth()
        )

        if (data.canUnlockWithBiometrics) {
            RetroButton(
                text = stringResource(R.string.unlock_action_biometric),
                onClick = onUnlockWithBiometricsClicked,
                enabled = !data.isProcessing,
                variant = RetroButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
