package es.ulpgc.pamn.atalaya.core.security

/**
 * Jerarquía tipada de errores criptográficos y de estado de sesión de Atalaya.
 */
sealed class CryptoException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /** La clave no coincide o los datos cifrados / AAD fueron alterados. */
    class AuthenticationFailedException(
        message: String = "Error de autenticación criptográfica",
        cause: Throwable? = null
    ) : CryptoException(message, cause)

    /** La clave hardware del Android Keystore quedó invalidada tras cambiar la biometría del sistema. */
    class KeyInvalidatedException(
        message: String = "La clave biométrica ha sido invalidada",
        cause: Throwable? = null
    ) : CryptoException(message, cause)

    /** Se intentó acceder a material criptográfico con la bóveda bloqueada. */
    class VaultLockedException(
        message: String = "La bóveda está bloqueada",
        cause: Throwable? = null
    ) : CryptoException(message, cause)

    /** Error inesperado de formato, configuración o proveedor criptográfico. */
    class CryptoOperationException(
        message: String = "Error en la operación criptográfica",
        cause: Throwable? = null
    ) : CryptoException(message, cause)
}
