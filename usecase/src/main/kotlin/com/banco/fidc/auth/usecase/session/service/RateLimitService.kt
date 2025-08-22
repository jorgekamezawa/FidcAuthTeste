package com.banco.fidc.auth.usecase.session.service

import com.banco.fidc.auth.usecase.session.dto.params.RateLimitCheckParams

interface RateLimitService {
    fun checkRateLimit(params: RateLimitCheckParams)
}