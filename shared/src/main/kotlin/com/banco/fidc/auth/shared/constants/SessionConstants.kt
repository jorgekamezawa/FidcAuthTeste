package com.banco.fidc.auth.shared.constants

object SessionConstants {
    const val MAX_CPF_LENGTH = 11
    const val MAX_PARTNER_LENGTH = 100
    const val MAX_USER_AGENT_LENGTH = 2000
    const val MAX_FINGERPRINT_LENGTH = 255
    const val MAX_FULL_NAME_LENGTH = 200
    const val MAX_EMAIL_LENGTH = 255
    const val MAX_PHONE_LENGTH = 20
    const val MAX_FUND_ID_LENGTH = 50
    const val MAX_FUND_NAME_LENGTH = 200
    const val MAX_RELATIONSHIP_ID_LENGTH = 50
    const val MAX_RELATIONSHIP_NAME_LENGTH = 200
    const val MAX_CONTRACT_NUMBER_LENGTH = 100
    const val MAX_PERMISSION_LENGTH = 100
    const val SESSION_SECRET_LENGTH = 36
    const val DEFAULT_SESSION_TTL_MINUTES = 30
}