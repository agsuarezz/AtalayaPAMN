package es.ulpgc.pamn.atalaya.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import es.ulpgc.pamn.atalaya.ui.screens.auth.AuthScreen
import es.ulpgc.pamn.atalaya.ui.screens.boot.BootScreen
import es.ulpgc.pamn.atalaya.ui.screens.generator.GeneratorPlaceholderScreen
import es.ulpgc.pamn.atalaya.ui.screens.settings.SettingsPlaceholderScreen
import es.ulpgc.pamn.atalaya.ui.screens.vault.VaultPlaceholderScreen

/** Grafo principal de navegación de Atalaya. */
@Composable
fun AtalayaNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = AtalayaDestination.Boot.route,
        modifier = modifier
    ) {
        composable(route = AtalayaDestination.Boot.route) {
            BootScreen(
                onBootFinished = {
                    navController.navigate(AtalayaDestination.Unlock.route) {
                        popUpTo(AtalayaDestination.Boot.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = AtalayaDestination.Unlock.route) {
            AuthScreen(
                onUnlockSuccess = {
                    navController.navigate(AtalayaDestination.VaultList.route) {
                        popUpTo(AtalayaDestination.Unlock.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = AtalayaDestination.VaultList.route) {
            VaultPlaceholderScreen(
                onOpenGenerator = {
                    navController.navigate(AtalayaDestination.Generator.route)
                },
                onOpenSettings = {
                    navController.navigate(AtalayaDestination.Settings.route)
                },
                onLockVault = {
                    navController.navigate(AtalayaDestination.Unlock.route) {
                        popUpTo(AtalayaDestination.VaultList.route) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(route = AtalayaDestination.Generator.route) {
            GeneratorPlaceholderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(route = AtalayaDestination.Settings.route) {
            SettingsPlaceholderScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
