package com.banco.fidc.auth.repository.jpa.session.exception

import com.banco.fidc.auth.shared.exception.InfrastructureException

class SessionRepositoryException(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "PostgreSQLRepository",
    message = message,
    cause = cause
)