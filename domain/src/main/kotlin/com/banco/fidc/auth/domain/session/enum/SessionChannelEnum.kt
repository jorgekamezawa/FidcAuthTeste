package com.banco.fidc.auth.domain.session.enum

import com.banco.fidc.auth.shared.exception.InvalidSessionEnumException

enum class SessionChannelEnum(
    val code: String,
    val description: String
) {
    WEB("W", "Web"),
    MOBILE("M", "Mobile");
    
    companion object {
        fun fromValue(value: String): SessionChannelEnum {
            return fromValueOrNull(value)
                ?: throw InvalidSessionEnumException(value, "SessionChannelEnum")
        }
        
        fun fromValueOrNull(value: String): SessionChannelEnum? {
            return values().firstOrNull { 
                it.name.equals(value, ignoreCase = true) || 
                it.code.equals(value, ignoreCase = true)
            }
        }
        
        fun fromCode(code: String): SessionChannelEnum {
            return values().firstOrNull { it.code == code }
                ?: throw InvalidSessionEnumException(code, "SessionChannelEnum")
        }
    }
    
    fun isWeb() = this == WEB
    fun isMobile() = this == MOBILE
}