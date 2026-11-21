package es.ulpgc.pamn.atalaya.core.security

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Mantiene la clave de cifrado de datos (`DEK`) únicamente en memoria volátil mientras
 * la bóveda está desbloqueada y sobreescribe su contenido con ceros al bloquear.
 */
@Singleton
class VaultSessionManager @Inject constructor() {

    private val lockObj = Any()
    private var activeDek: ByteArray? = null

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    fun unlock(dek: ByteArray) {
        require(dek.size == DEK_SIZE_BYTES) {
            "La DEK debe tener exactamente $DEK_SIZE_BYTES bytes"
        }
        synchronized(lockObj) {
            activeDek?.fill(0)
            activeDek = dek.copyOf()
            _isUnlocked.value = true
        }
    }

    fun <T> withDek(block: (ByteArray) -> T): T {
        val dekCopy = synchronized(lockObj) {
            val current = activeDek ?: throw CryptoException.VaultLockedException()
            current.copyOf()
        }
        return try {
            block(dekCopy)
        } finally {
            dekCopy.fill(0)
        }
    }

    fun lock() {
        synchronized(lockObj) {
            activeDek?.fill(0)
            activeDek = null
            _isUnlocked.value = false
        }
    }

    private companion object {
        const val DEK_SIZE_BYTES = 32
    }
}
