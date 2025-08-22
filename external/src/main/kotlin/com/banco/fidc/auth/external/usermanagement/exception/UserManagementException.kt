package com.banco.fidc.auth.external.usermanagement.exception

import com.banco.fidc.auth.shared.exception.InfrastructureException

class UserManagementException(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "UserManagement",
    message = message,
    cause = cause
)