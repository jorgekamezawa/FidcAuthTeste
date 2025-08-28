package com.banco.fidc.auth.web.session.dto.response

import com.banco.fidc.auth.usecase.session.dto.input.GetJwtSecretInput
import com.banco.fidc.auth.usecase.session.dto.output.GetJwtSecretOutput
import com.fasterxml.jackson.annotation.JsonProperty

data class GetJwtSecretResponse(
    @JsonProperty("secret")
    val secret: String
)

fun GetJwtSecretOutput.toResponse(): GetJwtSecretResponse =
    GetJwtSecretResponse(secret = this.secret)

fun createGetJwtSecretInput(
    userAgent: String,
    clientIpAddress: String
): GetJwtSecretInput =
    GetJwtSecretInput(
        userAgent = userAgent,
        clientIpAddress = clientIpAddress
    )