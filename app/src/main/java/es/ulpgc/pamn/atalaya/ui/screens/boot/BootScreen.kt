package es.ulpgc.pamn.atalaya.ui.screens.boot

import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import es.ulpgc.pamn.atalaya.R
import es.ulpgc.pamn.atalaya.ui.components.RetroCard
import kotlinx.coroutines.delay

/**
 * Pantalla de arranque retro tipo consola. Si el usuario tiene desactivadas
 * las animaciones en el sistema (`ANIMATOR_DURATION_SCALE == 0f`), salta
 * directamente a la pantalla de desbloqueo sin retardos.
 */
@Composable
fun BootScreen(
    onBootFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lines = listOf(
        stringResource(R.string.boot_line_1),
        stringResource(R.string.boot_line_2),
        stringResource(R.string.boot_line_3),
        stringResource(R.string.boot_line_4)
    )

    val reduceAnimations = remember(context) {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) == 0f
    }

    var visibleLines by remember {
        mutableIntStateOf(if (reduceAnimations) lines.size else 0)
    }

    LaunchedEffect(reduceAnimations) {
        if (reduceAnimations) {
            onBootFinished()
            return@LaunchedEffect
        }
        for (index in 1..lines.size) {
            delay(180L)
            visibleLines = index
        }
        delay(220L)
        onBootFinished()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            RetroCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.boot_header),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                lines.take(visibleLines).forEach { line ->
                    Text(
                        text = line,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
