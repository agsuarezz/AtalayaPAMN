package es.ulpgc.pamn.atalaya.core.security

import android.content.Context
import androidx.biometric.BiometricManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** Estado del soporte hardware biométrico fuerte (`BIOMETRIC_STRONG`) en el dispositivo. */
enum class BiometricAvailability {
    AVAILABLE,
    NOT_ENROLLED,
    UNAVAILABLE
}

/** Consulta la disponibilidad de biometría hardware fuerte en el dispositivo. */
interface BiometricAvailabilityChecker {
    fun checkAvailability(): BiometricAvailability
}

/** Implementación AndroidX respaldada por [BiometricManager] con `BIOMETRIC_STRONG`. */
class AndroidBiometricAvailabilityChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : BiometricAvailabilityChecker {

    override fun checkAvailability(): BiometricAvailability {
        val status = BiometricManager.from(context)
            .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return when (status) {
            BiometricManager.BIOMETRIC_SUCCESS -> BiometricAvailability.AVAILABLE
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricAvailability.NOT_ENROLLED
            else -> BiometricAvailability.UNAVAILABLE
        }
    }
}
