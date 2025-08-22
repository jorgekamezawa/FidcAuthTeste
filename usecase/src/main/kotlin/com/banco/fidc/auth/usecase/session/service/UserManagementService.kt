package com.banco.fidc.auth.usecase.session.service

import com.banco.fidc.auth.usecase.session.dto.params.UserManagementGetUserParams
import com.banco.fidc.auth.usecase.session.dto.result.UserManagementGetUserResult

interface UserManagementService {
    fun getUser(params: UserManagementGetUserParams): UserManagementGetUserResult
}