package com.banco.fidc.auth.usecase.session

import com.banco.fidc.auth.usecase.session.dto.input.SelectRelationshipInput
import com.banco.fidc.auth.usecase.session.dto.output.SelectRelationshipOutput

interface SelectRelationshipUseCase {
    fun execute(input: SelectRelationshipInput): SelectRelationshipOutput
}