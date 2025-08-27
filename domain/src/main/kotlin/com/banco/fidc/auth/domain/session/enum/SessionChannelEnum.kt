package com.banco.fidc.auth.domain.session.enum

import com.banco.fidc.auth.shared.exception.InvalidSessionEnumException

enum class SessionChannelEnum(
    val description: String
) {
    WEB("Web"),
    MOBILE("Mobile"),
    HELP_DESK("Help Desk");
    
    companion object {
        fun fromValue(value: String): SessionChannelEnum {
            return fromValueOrNull(value)
                ?: throw InvalidSessionEnumException(value, "SessionChannelEnum")
        }
        
        fun fromValueOrNull(value: String): SessionChannelEnum? {
            return SessionChannelEnum.entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            }
        }
        
        fun getAcceptedValues(): String {
            return SessionChannelEnum.entries.joinToString(", ") { it.name }
        }
    }
    
    fun isWeb() = this == WEB
    fun isMobile() = this == MOBILE
}