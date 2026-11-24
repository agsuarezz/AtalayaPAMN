package es.ulpgc.pamn.atalaya.domain.usecase

/** AAD fijo utilizado al envolver/desenvolver la DEK con la clave derivada de la contraseña maestra. */
internal val MASTER_WRAP_AAD: ByteArray = "vault:master_wrapped_dek".toByteArray(Charsets.UTF_8)
