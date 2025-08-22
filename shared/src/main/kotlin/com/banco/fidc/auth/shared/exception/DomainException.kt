package com.banco.fidc.auth.shared.exception

sealed class DomainException(
    message: String,
    cause: Throwable? = null
) : BusinessException(message, cause)

class SessionValidationException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)

class SessionNotFoundException(
    message: String
) : DomainException(message)

class SessionBusinessRuleException(
    message: String
) : DomainException(message)

class UserSessionControlValidationException(
    message: String,
    cause: Throwable? = null
) : DomainException(message, cause)

class UserSessionControlNotFoundException(
    message: String
) : DomainException(message)

class UserSessionControlBusinessRuleException(
    message: String
) : DomainException(message)

class InvalidSessionEnumException(
    value: String,
    enumType: String
) : DomainException("Valor '$value' inválido para $enumType")