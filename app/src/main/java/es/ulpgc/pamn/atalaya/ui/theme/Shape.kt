package es.ulpgc.pamn.atalaya.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

private val TerminalCornerShape = RoundedCornerShape(0.dp)

/** Formas rectangulares (radio 0 dp) para mantener la estética de terminal de los años 90. */
val AtalayaShapes = Shapes(
    extraSmall = TerminalCornerShape,
    small = TerminalCornerShape,
    medium = TerminalCornerShape,
    large = TerminalCornerShape,
    extraLarge = TerminalCornerShape
)
