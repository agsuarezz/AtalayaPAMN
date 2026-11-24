package es.ulpgc.pamn.atalaya.domain.model

/**
 * Modelo de dominio de una credencial almacenada en la bóveda.
 * Sobrescribe `toString()` para impedir que secretos o identificadores se filtren en logs.
 */
data class Credential(
    val id: Long = 0L,
    val title: String,
    val username: String,
    val password: String,
    val url: String = "",
    val notes: String = "",
    val category: CredentialCategory = CredentialCategory.GENERAL,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
) {
    override fun toString(): String {
        return "Credential(id=$id, category=$category, updatedAtEpochMillis=$updatedAtEpochMillis)"
    }
}
