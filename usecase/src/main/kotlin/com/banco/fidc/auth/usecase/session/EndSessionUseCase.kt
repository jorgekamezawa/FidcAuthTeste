package com.banco.fidc.auth.usecase.session

import com.banco.fidc.auth.usecase.session.dto.input.EndSessionInput

interface EndSessionUseCase {
    fun execute(input: EndSessionInput)
}