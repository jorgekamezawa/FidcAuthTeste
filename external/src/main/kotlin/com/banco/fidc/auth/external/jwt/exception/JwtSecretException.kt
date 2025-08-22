package com.banco.fidc.auth.external.jwt.exception

import com.banco.fidc.auth.shared.exception.InfrastructureException

class JwtSecretException(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "JwtSecret",
    message = message,
    cause = cause
)