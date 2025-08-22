package com.banco.fidc.auth.usecase.session

import com.banco.fidc.auth.usecase.session.dto.input.CreateUserSessionInput
import com.banco.fidc.auth.usecase.session.dto.output.CreateUserSessionOutput

interface CreateUserSessionUseCase {
    fun execute(input: CreateUserSessionInput): CreateUserSessionOutput
}