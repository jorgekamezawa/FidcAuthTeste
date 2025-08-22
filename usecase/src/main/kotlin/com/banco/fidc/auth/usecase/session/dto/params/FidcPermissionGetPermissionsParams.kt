package com.banco.fidc.auth.usecase.session.dto.params

data class FidcPermissionGetPermissionsParams(
    val partner: String,
    val cpf: String,
    val relationshipId: String?
)