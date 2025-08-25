package com.banco.fidc.auth.usecase.session.exception

import com.banco.fidc.auth.shared.exception.BusinessException

sealed class SessionApplicationException(
    message: String,
    cause: Throwable? = null
) : BusinessException(message, cause)

class SessionValidationException(
    message: String
) : SessionApplicationException(message)

class UserManagementIntegrationException(
    message: String,
    cause: Throwable? = null
) : SessionApplicationException(
    "UserManagement integration failed: $message", cause
)

class FidcPermissionIntegrationException(
    message: String,
    cause: Throwable? = null
) : SessionApplicationException(
    "FidcPermission integration failed: $message", cause
)

class SessionProcessingException(
    message: String,
    cause: Throwable? = null
) : SessionApplicationException(message, cause)

class SessionNotFoundException(
    message: String
) : SessionApplicationException(message)