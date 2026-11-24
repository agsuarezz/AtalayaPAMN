package es.ulpgc.pamn.atalaya.domain.usecase

import es.ulpgc.pamn.atalaya.domain.model.MIN_MASTER_PASSWORD_LENGTH
import es.ulpgc.pamn.atalaya.domain.model.PasswordStrength
import es.ulpgc.pamn.atalaya.domain.model.PasswordStrengthResult
import javax.inject.Inject
import kotlin.math.log2

/** Evalúa la fortaleza y entropía estimada de la contraseña introducida. */
class EvaluatePasswordStrengthUseCase @Inject constructor() {

    operator fun invoke(password: CharSequence): PasswordStrengthResult {
        if (password.isEmpty()) {
            return PasswordStrengthResult(
                strength = PasswordStrength.VERY_WEAK,
                score = 0,
                estimatedEntropyBits = 0.0,
                meetsMinimumLength = false
            )
        }

        var hasLower = false
        var hasUpper = false
        var hasDigit = false
        var hasSymbol = false

        for (i in 0 until password.length) {
            val ch = password[i]
            when {
                ch.isLowerCase() -> hasLower = true
                ch.isUpperCase() -> hasUpper = true
                ch.isDigit() -> hasDigit = true
                else -> hasSymbol = true
            }
        }

        var poolSize = 0
        if (hasLower) poolSize += LOWERCASE_POOL
        if (hasUpper) poolSize += UPPERCASE_POOL
        if (hasDigit) poolSize += DIGIT_POOL
        if (hasSymbol) poolSize += SYMBOL_POOL

        val entropy = if (poolSize > 0) password.length * log2(poolSize.toDouble()) else 0.0
        val meetsMinimumLength = password.length >= MIN_MASTER_PASSWORD_LENGTH

        val (strength, score) = if (!meetsMinimumLength) {
            if (password.length < SHORT_LENGTH_THRESHOLD || entropy < VERY_WEAK_ENTROPY_BITS) {
                PasswordStrength.VERY_WEAK to 0
            } else {
                PasswordStrength.WEAK to 1
            }
        } else {
            when {
                entropy < WEAK_ENTROPY_BITS -> PasswordStrength.WEAK to 1
                entropy < MEDIUM_ENTROPY_BITS -> PasswordStrength.MEDIUM to 2
                entropy < STRONG_ENTROPY_BITS -> PasswordStrength.STRONG to 3
                else -> PasswordStrength.VERY_STRONG to 4
            }
        }

        return PasswordStrengthResult(
            strength = strength,
            score = score,
            estimatedEntropyBits = entropy,
            meetsMinimumLength = meetsMinimumLength
        )
    }

    private companion object {
        const val LOWERCASE_POOL = 26
        const val UPPERCASE_POOL = 26
        const val DIGIT_POOL = 10
        const val SYMBOL_POOL = 32
        const val SHORT_LENGTH_THRESHOLD = 6
        const val VERY_WEAK_ENTROPY_BITS = 28.0
        const val WEAK_ENTROPY_BITS = 40.0
        const val MEDIUM_ENTROPY_BITS = 60.0
        const val STRONG_ENTROPY_BITS = 80.0
    }
}
