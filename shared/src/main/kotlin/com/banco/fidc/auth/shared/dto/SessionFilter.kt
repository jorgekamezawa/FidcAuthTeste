package com.banco.fidc.auth.shared.dto

import java.time.LocalDate

data class SessionFilter(
    val cpf: String? = null,
    val partner: String? = null,
    val isActive: Boolean? = null,
    val channel: String? = null,
    val createdFrom: LocalDate? = null,
    val createdTo: LocalDate? = null
)

data class UserSessionControlFilter(
    val cpf: String? = null,
    val partner: String? = null,
    val isActive: Boolean? = null,
    val firstAccessFrom: LocalDate? = null,
    val firstAccessTo: LocalDate? = null,
    val lastAccessFrom: LocalDate? = null,
    val lastAccessTo: LocalDate? = null
)