package es.ulpgc.pamn.atalaya.ui.navigation

/** Destinos principales del grafo de navegación de Atalaya. */
enum class AtalayaDestination(val route: String) {
    Boot("boot"),
    Unlock("unlock"),
    VaultList("vault_list"),
    Generator("generator"),
    Settings("settings")
}
