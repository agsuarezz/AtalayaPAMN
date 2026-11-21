package es.ulpgc.pamn.atalaya.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Variantes visuales del botón retro de Atalaya. */
enum class RetroButtonVariant {
    PRIMARY,
    SECONDARY,
    WARNING,
    DANGER
}

/**
 * Botón rectangular estilo terminal con área táctil mínima de 48 dp y
 * lectura limpia en TalkBack (oculta los corchetes decorativos).
 */
@Composable
fun RetroButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    variant: RetroButtonVariant = RetroButtonVariant.PRIMARY
) {
    val colorScheme = MaterialTheme.colorScheme
    val (containerColor, contentColor, borderColor) = when (variant) {
        RetroButtonVariant.PRIMARY -> Triple(
            colorScheme.primary,
            colorScheme.onPrimary,
            colorScheme.primary
        )
        RetroButtonVariant.SECONDARY -> Triple(
            colorScheme.surface,
            colorScheme.primary,
            colorScheme.outline
        )
        RetroButtonVariant.WARNING -> Triple(
            colorScheme.surface,
            colorScheme.secondary,
            colorScheme.secondary
        )
        RetroButtonVariant.DANGER -> Triple(
            colorScheme.surface,
            colorScheme.error,
            colorScheme.error
        )
    }

    Button(
        onClick = onClick,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        border = BorderStroke(
            width = 1.dp,
            color = if (enabled) borderColor else colorScheme.outline.copy(alpha = 0.4f)
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = colorScheme.surfaceVariant,
            disabledContentColor = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp,
            focusedElevation = 0.dp,
            hoveredElevation = 0.dp,
            disabledElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        modifier = modifier
            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = text
            }
    ) {
        Text(
            text = "[ ${text.uppercase()} ]",
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics {}
        )
    }
}
