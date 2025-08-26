package com.banco.fidc.auth.web.session.controller

import com.banco.fidc.auth.usecase.session.CreateUserSessionUseCase
import com.banco.fidc.auth.usecase.session.SelectRelationshipUseCase
import com.banco.fidc.auth.usecase.session.dto.input.SelectRelationshipInput
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
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class SessionController(
    private val createUserSessionUseCase: CreateUserSessionUseCase,
    private val selectRelationshipUseCase: SelectRelationshipUseCase
) : SessionApiDoc {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping("/session")
    @ResponseStatus(HttpStatus.OK)
    override fun createUserSession(
        @Valid @RequestBody request: CreateUserSessionRequest,
        @RequestHeader("partner") partner: String,
        @RequestHeader("user-agent") userAgent: String,
        @RequestHeader("channel") channel: String,
        @RequestHeader("fingerprint") fingerprint: String,
        @RequestHeader("latitude") latitude: String,
        @RequestHeader("longitude") longitude: String,
        @RequestHeader("location-accuracy") locationAccuracy: String,
        @RequestHeader("location-timestamp") locationTimestamp: String,
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,
        httpRequest: HttpServletRequest
    ): CreateUserSessionResponse {
        val finalCorrelationId = correlationId ?: "not-provided"
        logger.info(
            "Received createUserSession request: partner=${partner}, channel=${channel}, correlationId=${finalCorrelationId}"
        )

        // Additional validation - Spring doesn't validate empty strings
        require(partner.isNotBlank()) { "Header 'partner' cannot be empty" }
        require(userAgent.isNotBlank()) { "Header 'user-agent' cannot be empty" }
        require(channel.isNotBlank()) { "Header 'channel' cannot be empty" }
        require(fingerprint.isNotBlank()) { "Header 'fingerprint' cannot be empty" }
        require(latitude.isNotBlank()) { "Header 'latitude' cannot be empty" }
        require(longitude.isNotBlank()) { "Header 'longitude' cannot be empty" }
        require(locationAccuracy.isNotBlank()) { "Header 'location-accuracy' cannot be empty" }
        require(locationTimestamp.isNotBlank()) { "Header 'location-timestamp' cannot be empty" }

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
            correlationId = finalCorrelationId,
            clientIpAddress = clientIpAddress
        )
        val output = createUserSessionUseCase.execute(input)
        val response = output.toResponse()

        logger.info("User session created successfully: correlationId=${finalCorrelationId}")
        return response
    }

    @PatchMapping("/sessions/relationship")
    @ResponseStatus(HttpStatus.OK)
    override fun selectRelationship(
        @RequestHeader("authorization") authorization: String,
        @RequestHeader("relationshipId") relationshipId: String,
        @RequestHeader("x-correlation-id", required = false) correlationId: String?,
        httpRequest: HttpServletRequest
    ): SelectRelationshipResponse {
        val finalCorrelationId = correlationId ?: "not-provided"
        val userAgent = httpRequest.getHeader("user-agent") ?: "unknown"
        
        logger.info(
            "Received selectRelationship request: relationshipId=${relationshipId}, correlationId=${finalCorrelationId}"
        )

        // Validações de header obrigatórios
        require(authorization.isNotBlank()) { "Header 'authorization' cannot be empty" }
        require(relationshipId.isNotBlank()) { "Header 'relationshipId' cannot be empty" }

        val clientIpAddress = httpRequest.getClientIp()
        val input = SelectRelationshipInput(
            accessToken = authorization,
            relationshipId = relationshipId,
            clientIpAddress = clientIpAddress,
            userAgent = userAgent,
            correlationId = finalCorrelationId
        )
        
        val output = selectRelationshipUseCase.execute(input)
        val response = output.toResponse()

        logger.info("Relationship selected successfully: relationshipId=${relationshipId}, correlationId=${finalCorrelationId}")
        return response
    }
}