package es.ulpgc.pamn.atalaya.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.atalaya.R
import es.ulpgc.pamn.atalaya.domain.model.PasswordStrength
import es.ulpgc.pamn.atalaya.domain.model.PasswordStrengthResult

/**
 * Indicador retro de fortaleza de contraseña con 5 bloques rectangulares
 * y descripción accesible unificada para TalkBack.
 */
@Composable
fun PasswordStrengthIndicator(
    strengthResult: PasswordStrengthResult,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val (labelRes, activeColor) = when (strengthResult.strength) {
        PasswordStrength.VERY_WEAK -> R.string.auth_strength_very_weak to colorScheme.error
        PasswordStrength.WEAK -> R.string.auth_strength_weak to colorScheme.error
        PasswordStrength.MEDIUM -> R.string.auth_strength_medium to colorScheme.secondary
        PasswordStrength.STRONG -> R.string.auth_strength_strong to colorScheme.primary
        PasswordStrength.VERY_STRONG -> R.string.auth_strength_very_strong to colorScheme.primary
    }

    val strengthLabel = stringResource(labelRes)
    val filledSegments = (strengthResult.score + 1).coerceIn(1, 5)
    val accessibilityDescription = stringResource(R.string.auth_strength_cd, strengthLabel)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {},
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (segment in 1..5) {
                val isFilled = segment <= filledSegments
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(
                            color = if (isFilled) activeColor else colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFilled) activeColor else colorScheme.outline,
                            shape = MaterialTheme.shapes.small
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.auth_strength_prefix, strengthLabel),
            style = MaterialTheme.typography.labelMedium,
            color = activeColor,
            modifier = Modifier.clearAndSetSemantics {}
        )

        if (!strengthResult.meetsMinimumLength) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.auth_strength_min_length_hint),
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}
