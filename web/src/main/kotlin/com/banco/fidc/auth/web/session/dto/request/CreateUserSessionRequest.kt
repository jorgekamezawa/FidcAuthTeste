package com.banco.fidc.auth.web.session.dto.request

import com.banco.fidc.auth.usecase.session.dto.input.CreateUserSessionInput
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Requisição para criar uma nova sessão de usuário")
data class CreateUserSessionRequest(
    @field:NotBlank(message = "signedData é obrigatório")
    @Schema(
        description = "Token JWT com dados do usuário",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcGYiOiIyNjYzNTE0NjUyOSJ9.9hFb4YPD2mWHOiV28loMArSVVYjI5oN38-yUqLRwKmQ",
        required = true
    )
    val signedData: String
)

fun CreateUserSessionRequest.toInput(
    partner: String,
    userAgent: String,
    channel: String,
    fingerprint: String,
    latitude: String?,
    longitude: String?,
    locationAccuracy: String?,
    locationTimestamp: String?,
    clientIpAddress: String
): CreateUserSessionInput {
    return CreateUserSessionInput(
        signedData = this.signedData,
        partner = partner,
        userAgent = userAgent,
        channel = channel,
        fingerprint = fingerprint,
        latitude = latitude,
        longitude = longitude,
        locationAccuracy = locationAccuracy,
        locationTimestamp = locationTimestamp,
        clientIpAddress = clientIpAddress
    )
}