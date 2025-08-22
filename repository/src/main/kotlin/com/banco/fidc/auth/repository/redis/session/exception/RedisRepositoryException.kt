package com.banco.fidc.auth.repository.redis.session.exception

import com.banco.fidc.auth.shared.exception.InfrastructureException

class RedisRepositoryException(
    message: String,
    cause: Throwable? = null
) : InfrastructureException(
    component = "Redis",
    message = message,
    cause = cause
)