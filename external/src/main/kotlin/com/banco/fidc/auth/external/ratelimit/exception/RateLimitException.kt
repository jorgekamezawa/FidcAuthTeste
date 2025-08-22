package com.banco.fidc.auth.external.ratelimit.exception

import com.banco.fidc.auth.shared.exception.InfrastructureException

class RateLimitException(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "RateLimit",
    message = message,
    cause = cause
)