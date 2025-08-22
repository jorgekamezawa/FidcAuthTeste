package com.banco.fidc.auth.usecase.session.dto.params

data class UserManagementGetUserParams(
    val partner: String,
    val cpf: String
)