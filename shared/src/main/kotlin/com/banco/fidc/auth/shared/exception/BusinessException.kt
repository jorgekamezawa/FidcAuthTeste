package com.banco.fidc.auth.shared.exception

abstract class BusinessException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)