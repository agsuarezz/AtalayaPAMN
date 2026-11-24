package es.ulpgc.pamn.atalaya.domain.model

/** Longitud mínima exigida para la contraseña maestra de Atalaya. */
const val MIN_MASTER_PASSWORD_LENGTH = 10

/** Nivel de fortaleza estimado para la contraseña maestra. */
enum class PasswordStrength {
    VERY_WEAK,
    WEAK,
    MEDIUM,
    STRONG,
    VERY_STRONG
}

/** Resultado de la evaluación de fortaleza de una contraseña. */
data class PasswordStrengthResult(
    val strength: PasswordStrength,
    val score: Int,
    val estimatedEntropyBits: Double,
    val meetsMinimumLength: Boolean
)
