package es.ulpgc.pamn.atalaya

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.ui.graphics.toArgb
import dagger.hilt.android.AndroidEntryPoint
import es.ulpgc.pamn.atalaya.ui.navigation.AtalayaNavGraph
import es.ulpgc.pamn.atalaya.ui.theme.AtalayaTheme
import es.ulpgc.pamn.atalaya.ui.theme.TerminalBlack

/**
 * Actividad única de Atalaya. Activa `FLAG_SECURE` desde el arranque para impedir
 * capturas de pantalla y ocultar el contenido en la vista de aplicaciones recientes.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        val darkBarStyle = SystemBarStyle.dark(TerminalBlack.toArgb())
        enableEdgeToEdge(
            statusBarStyle = darkBarStyle,
            navigationBarStyle = darkBarStyle
        )
        setContent {
            AtalayaTheme {
                AtalayaNavGraph()
            }
        }
    }
}
