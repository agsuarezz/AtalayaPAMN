package es.ulpgc.pamn.atalaya.ui.state

/**
 * Estado unidireccional genérico expuesto por los ViewModels hacia la capa UI.
 */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}
