package com.banco.fidc.auth.shared.exception

abstract class InfrastructureException(
    val component: String,
    message: String,
    cause: Throwable? = null
) : RuntimeException("[$component] $message", cause)