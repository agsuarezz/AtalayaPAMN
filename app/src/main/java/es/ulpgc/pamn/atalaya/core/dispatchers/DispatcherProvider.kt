package es.ulpgc.pamn.atalaya.core.dispatchers

import kotlinx.coroutines.CoroutineDispatcher

/** Abstracción de dispatchers de corrutinas para facilitar pruebas unitarias deterministas. */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val io: CoroutineDispatcher
    val default: CoroutineDispatcher
}
