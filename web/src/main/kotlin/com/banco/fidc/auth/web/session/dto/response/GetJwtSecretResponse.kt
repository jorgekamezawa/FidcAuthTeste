package com.banco.fidc.auth.web.session.dto.response

import com.banco.fidc.auth.usecase.session.dto.output.GetJwtSecretOutput
import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "Resposta contendo o segredo JWT")
data class GetJwtSecretResponse(
    @Schema(description = "Hash do segredo JWT para validação de token", example = "a1b2c3d4e5f6789012345678901234567890abcdef1234567890abcdef123456")
    @JsonProperty("secret")
    val secret: String
)

fun GetJwtSecretOutput.toResponse(): GetJwtSecretResponse =
    GetJwtSecretResponse(secret = this.secret)