package es.ulpgc.pamn.atalaya.domain.model

/**
 * Metadatos criptográficos de la bóveda para derivar la clave desde la contraseña maestra
 * y recuperar la `DEK` mediante contraseña o biometría.
 */
data class VaultMetadata(
    val saltBase64: String,
    val kdfIterations: Int,
    val masterWrappedDek: String,
    val biometricWrappedDek: String? = null,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
) {
    override fun toString(): String {
        return "VaultMetadata(kdfIterations=$kdfIterations, hasBiometricKey=${biometricWrappedDek != null}, updatedAtEpochMillis=$updatedAtEpochMillis)"
    }
}
