package com.banco.fidc.auth.web.session.controller

import com.banco.fidc.auth.usecase.session.CreateUserSessionUseCase
import com.banco.fidc.auth.usecase.session.SelectRelationshipUseCase
import com.banco.fidc.auth.usecase.session.EndSessionUseCase
import com.banco.fidc.auth.usecase.session.dto.input.SelectRelationshipInput
import com.banco.fidc.auth.usecase.session.dto.input.EndSessionInput
import com.banco.fidc.auth.web.common.extension.getClientIp
import com.banco.fidc.auth.web.session.documentation.SessionApiDoc
import com.banco.fidc.auth.web.session.dto.request.CreateUserSessionRequest
import com.banco.fidc.auth.web.session.dto.request.toInput
import com.banco.fidc.auth.web.session.dto.response.CreateUserSessionResponse
import com.banco.fidc.auth.web.session.dto.response.SelectRelationshipResponse
import com.banco.fidc.auth.web.session.dto.response.toResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/v1/sessions")
class SessionController(
    private val createUserSessionUseCase: CreateUserSessionUseCase,
    private val selectRelationshipUseCase: SelectRelationshipUseCase,
    private val endSessionUseCase: EndSessionUseCase
) : SessionApiDoc {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    override fun createUserSession(
        @Valid @RequestBody request: CreateUserSessionRequest,
        @RequestHeader("partner") partner: String,
        @RequestHeader("user-agent") userAgent: String,
        @RequestHeader("channel") channel: String,
        @RequestHeader("fingerprint") fingerprint: String,
        @RequestHeader("latitude", required = false) latitude: String?,
        @RequestHeader("longitude", required = false) longitude: String?,
        @RequestHeader("location-accuracy", required = false) locationAccuracy: String?,
        @RequestHeader("location-timestamp", required = false) locationTimestamp: String?,
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,
        httpRequest: HttpServletRequest
    ): CreateUserSessionResponse {
        logger.info("Received createUserSession request: partner=${partner}, channel=${channel}")

        // Additional validation - Spring doesn't validate empty strings
        require(partner.isNotBlank()) { "Header 'partner' cannot be empty" }
        require(userAgent.isNotBlank()) { "Header 'user-agent' cannot be empty" }
        require(channel.isNotBlank()) { "Header 'channel' cannot be empty" }
        require(fingerprint.isNotBlank()) { "Header 'fingerprint' cannot be empty" }

        val clientIpAddress = httpRequest.getClientIp()
        val input = request.toInput(
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
        val output = createUserSessionUseCase.execute(input)
        val response = output.toResponse()

        logger.info("User session created successfully")
        return response
    }

    @PatchMapping("/relationship")
    @ResponseStatus(HttpStatus.OK)
    override fun selectRelationship(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("partner") partner: String,
        @RequestHeader("relationshipId") relationshipId: String,
        @RequestHeader("user-agent") userAgent: String,
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,
        httpRequest: HttpServletRequest
    ): SelectRelationshipResponse {
        logger.info("Received selectRelationship request: relationshipId=${relationshipId}, partner=${partner}")

        // Validações de header obrigatórios
        require(authorization.isNotBlank()) { "Header 'authorization' cannot be empty" }
        require(partner.isNotBlank()) { "Header 'partner' cannot be empty" }
        require(relationshipId.isNotBlank()) { "Header 'relationshipId' cannot be empty" }
        require(userAgent.isNotBlank()) { "Header 'user-agent' cannot be empty" }

        val clientIpAddress = httpRequest.getClientIp()
        val input = SelectRelationshipInput(
            accessToken = authorization,
            relationshipId = relationshipId,
            partner = partner,
            clientIpAddress = clientIpAddress,
            userAgent = userAgent
        )
        
        val output = selectRelationshipUseCase.execute(input)
        val response = output.toResponse()

        logger.info("Relationship selected successfully: relationshipId=${relationshipId}")
        return response
    }

    @DeleteMapping
    override fun endSession(
        @RequestHeader("Authorization") authorization: String,
        @RequestHeader("partner") partner: String,
        @RequestHeader("user-agent") userAgent: String,
        @RequestHeader(value = "x-correlation-id", required = false) correlationId: String?,
        httpRequest: HttpServletRequest
    ): ResponseEntity<Void> {
        logger.info("Received endSession request: partner=${partner}")

        // Validações de header obrigatórios
        require(authorization.isNotBlank()) { "Header 'Authorization' cannot be empty" }
        require(partner.isNotBlank()) { "Header 'partner' cannot be empty" }
        require(userAgent.isNotBlank()) { "Header 'user-agent' cannot be empty" }

        val clientIpAddress = httpRequest.getClientIp()
        val input = EndSessionInput(
            accessToken = authorization,
            partner = partner,
            clientIpAddress = clientIpAddress,
            userAgent = userAgent
        )

        endSessionUseCase.execute(input)

        logger.info("Session ended successfully")
        return ResponseEntity.noContent().build()
    }
}