package es.ulpgc.pamn.atalaya.core.security

import android.content.Context
import android.content.ContextWrapper
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import javax.crypto.Cipher

/** Desenvuelve un [Context] hasta encontrar su [FragmentActivity] anfitriona, si existe. */
tailrec fun Context.findFragmentActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findFragmentActivity()
    else -> null
}

/**
 * Lanza `BiometricPrompt` autenticado con `BIOMETRIC_STRONG` y un [BiometricPrompt.CryptoObject]
 * ligado a la clave del Android Keystore.
 */
object BiometricPromptHelper {

    fun authenticateWithCrypto(
        activity: FragmentActivity,
        cipher: Cipher,
        title: String,
        subtitle: String,
        negativeButtonText: String,
        onSuccess: (Cipher) -> Unit,
        onCancelled: () -> Unit,
        onError: (errorCode: Int, errorMessage: String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                val authenticatedCipher = result.cryptoObject?.cipher
                if (authenticatedCipher != null) {
                    onSuccess(authenticatedCipher)
                } else {
                    onError(
                        BiometricPrompt.ERROR_UNABLE_TO_PROCESS,
                        "No se recibió el objeto criptográfico autenticado"
                    )
                }
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                when (errorCode) {
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                    BiometricPrompt.ERROR_CANCELED -> onCancelled()
                    else -> onError(errorCode, errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                // El diálogo del sistema informa al usuario y permite reintentar hasta que cancele o bloquee.
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .setConfirmationRequired(true)
            .build()

        BiometricPrompt(activity, executor, callback)
            .authenticate(promptInfo, BiometricPrompt.CryptoObject(cipher))
    }
}
