package com.banco.fidc.auth.external.fidcpermission.exception

import com.banco.fidc.auth.shared.exception.InfrastructureException

class FidcPermissionException(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "FidcPermission",
    message = message,
    cause = cause
)