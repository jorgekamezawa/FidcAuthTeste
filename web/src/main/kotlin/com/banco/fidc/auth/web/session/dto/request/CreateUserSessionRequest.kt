package com.banco.fidc.auth.web.session.dto.request

import com.banco.fidc.auth.usecase.session.dto.input.CreateUserSessionInput
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Request to create a new user session")
data class CreateUserSessionRequest(
    @field:NotBlank(message = "signedData is required")
    @Schema(
        description = "JWT token with user data",
        example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJjcGYiOiIxMjM0NTY3ODkwMSJ9.signature",
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