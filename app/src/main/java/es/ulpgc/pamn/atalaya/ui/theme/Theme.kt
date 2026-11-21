package es.ulpgc.pamn.atalaya.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val AtalayaDarkColorScheme = darkColorScheme(
    primary = PhosphorGreen,
    onPrimary = TerminalOnPrimary,
    secondary = TerminalAmber,
    onSecondary = TerminalOnSecondary,
    tertiary = PhosphorGreenDim,
    onTertiary = TerminalOnPrimary,
    background = TerminalBlack,
    onBackground = PhosphorGreen,
    surface = TerminalSurface,
    onSurface = PhosphorGreen,
    surfaceVariant = TerminalSurfaceVariant,
    onSurfaceVariant = PhosphorGreenDim,
    outline = TerminalOutline,
    outlineVariant = TerminalOutline,
    error = TerminalRed,
    onError = TerminalOnError
)

/**
 * Tema visual único de Atalaya con estética terminal retro de los años 90.
 * Desactiva deliberadamente el color dinámico (Material You) para preservar
 * la identidad visual y garantizar el contraste WCAG AA.
 */
@Composable
fun AtalayaTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AtalayaDarkColorScheme,
        typography = AtalayaTypography,
        shapes = AtalayaShapes,
        content = content
    )
}
