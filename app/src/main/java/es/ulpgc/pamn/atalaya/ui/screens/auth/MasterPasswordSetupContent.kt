package es.ulpgc.pamn.atalaya.ui.screens.auth

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
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

/** Contenido de la pantalla para el alta inicial de contraseña maestra y biometría opcional. */
@Composable
fun MasterPasswordSetupContent(
    data: AuthScreenData,
    onPasswordChanged: (String) -> Unit,
    onConfirmPasswordChanged: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onBiometricOptionChanged: (Boolean) -> Unit,
    onCreateVaultSubmit: () -> Unit,
    onRetryBiometricEnrollment: () -> Unit,
    onSkipBiometricEnrollment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        RetroCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.auth_setup_intro_title),
                style = MaterialTheme.typography.titleMedium,
                color = colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.auth_setup_intro_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant
            )
        }

        data.statusMessage?.let { status ->
            AuthStatusBanner(status = status)
        }

        if (data.isVaultCreatedPendingBiometrics) {
            RetroButton(
                text = stringResource(R.string.auth_setup_action_retry_biometrics),
                onClick = onRetryBiometricEnrollment,
                enabled = !data.isProcessing,
                variant = RetroButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
            RetroButton(
                text = stringResource(R.string.auth_setup_action_skip_biometrics),
                onClick = onSkipBiometricEnrollment,
                enabled = !data.isProcessing,
                variant = RetroButtonVariant.SECONDARY,
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            RetroTextField(
                value = data.password,
                onValueChange = onPasswordChanged,
                label = stringResource(R.string.auth_setup_password_label),
                placeholder = stringResource(R.string.auth_setup_password_placeholder),
                errorMessage = data.passwordErrorRes?.let { stringResource(it) },
                enabled = !data.isProcessing,
                visualTransformation = if (data.isPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Next,
                    autoCorrectEnabled = false
                ),
                trailingIcon = {
                    PasswordVisibilityToggleButton(
                        isVisible = data.isPasswordVisible,
                        onToggle = onTogglePasswordVisibility
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            PasswordStrengthIndicator(strengthResult = data.passwordStrength)

            RetroTextField(
                value = data.confirmPassword,
                onValueChange = onConfirmPasswordChanged,
                label = stringResource(R.string.auth_setup_confirm_label),
                placeholder = stringResource(R.string.auth_setup_confirm_placeholder),
                errorMessage = data.confirmPasswordErrorRes?.let { stringResource(it) },
                enabled = !data.isProcessing,
                visualTransformation = if (data.isConfirmPasswordVisible) {
                    VisualTransformation.None
                } else {
                    PasswordVisualTransformation()
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done,
                    autoCorrectEnabled = false
                ),
                keyboardActions = KeyboardActions(onDone = { onCreateVaultSubmit() }),
                trailingIcon = {
                    PasswordVisibilityToggleButton(
                        isVisible = data.isConfirmPasswordVisible,
                        onToggle = onToggleConfirmPasswordVisibility
                    )
                },
                modifier = Modifier.fillMaxWidth()
            )

            BiometricEnrollmentOptionRow(
                availability = data.biometricAvailability,
                checked = data.isBiometricOptionEnabled,
                enabled = !data.isProcessing,
                onCheckedChange = onBiometricOptionChanged
            )

            RetroButton(
                text = stringResource(R.string.auth_setup_action_create),
                onClick = onCreateVaultSubmit,
                enabled = !data.isProcessing,
                variant = RetroButtonVariant.PRIMARY,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun BiometricEnrollmentOptionRow(
    availability: BiometricAvailability,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isHardwareReady = availability == BiometricAvailability.AVAILABLE
    val isInteractive = enabled && isHardwareReady
    val subtitleRes = when (availability) {
        BiometricAvailability.AVAILABLE -> R.string.auth_setup_biometric_available
        BiometricAvailability.NOT_ENROLLED -> R.string.auth_setup_biometric_not_enrolled
        BiometricAvailability.UNAVAILABLE -> R.string.auth_setup_biometric_unavailable
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .border(
                width = 1.dp,
                color = if (isHardwareReady && checked) colorScheme.primary else colorScheme.outline,
                shape = MaterialTheme.shapes.small
            )
            .toggleable(
                value = isHardwareReady && checked,
                enabled = isInteractive,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isHardwareReady && checked,
            onCheckedChange = null,
            enabled = isInteractive,
            colors = CheckboxDefaults.colors(
                checkedColor = colorScheme.primary,
                uncheckedColor = colorScheme.outline,
                checkmarkColor = colorScheme.onPrimary
            ),
            modifier = Modifier.clearAndSetSemantics {}
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.auth_setup_biometric_title),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isHardwareReady) colorScheme.onSurface else colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(subtitleRes),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
internal fun PasswordVisibilityToggleButton(
    isVisible: Boolean,
    onToggle: () -> Unit
) {
    IconButton(
        onClick = onToggle,
        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
    ) {
        Icon(
            imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
            contentDescription = stringResource(
                if (isVisible) R.string.auth_hide_password_cd else R.string.auth_show_password_cd
            ),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
internal fun AuthStatusBanner(
    status: AuthMessage,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val (borderColor, textColor) = when (status.type) {
        AuthMessageType.INFO -> colorScheme.outline to colorScheme.onSurfaceVariant
        AuthMessageType.WARNING -> colorScheme.secondary to colorScheme.secondary
        AuthMessageType.ERROR -> colorScheme.error to colorScheme.error
    }

    RetroCard(
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite },
        borderColor = borderColor
    ) {
        Text(
            text = stringResource(status.messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor
        )
    }
}
