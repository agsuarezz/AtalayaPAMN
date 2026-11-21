package es.ulpgc.pamn.atalaya.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.atalaya.R
import es.ulpgc.pamn.atalaya.ui.components.RetroButton
import es.ulpgc.pamn.atalaya.ui.components.RetroCard
import es.ulpgc.pamn.atalaya.ui.components.RetroTextField
import es.ulpgc.pamn.atalaya.ui.components.RetroTopBar

/**
 * Pantalla esqueleto de desbloqueo de bóveda para validar la navegación y el tema retro
 * antes de integrar la capa criptográfica y la biometría.
 */
@Composable
fun UnlockPlaceholderScreen(
    onUnlockSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var masterPassword by rememberSaveable { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            RetroTopBar(title = stringResource(R.string.unlock_title))
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RetroCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.unlock_subtitle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.unlock_pending_notice),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            RetroTextField(
                value = masterPassword,
                onValueChange = { masterPassword = it },
                label = stringResource(R.string.unlock_master_label),
                placeholder = stringResource(R.string.unlock_master_placeholder),
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            RetroButton(
                text = stringResource(R.string.unlock_action_enter),
                onClick = onUnlockSuccess,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
