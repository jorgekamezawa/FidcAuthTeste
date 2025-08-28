package com.banco.fidc.auth.usecase.session

import com.banco.fidc.auth.usecase.session.dto.output.GetJwtSecretOutput

interface GetJwtSecretUseCase {
    fun execute(): GetJwtSecretOutput
}