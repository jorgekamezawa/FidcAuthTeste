package com.banco.fidc.auth.usecase.session.service

import com.banco.fidc.auth.usecase.session.dto.params.FidcPermissionGetPermissionsParams
import com.banco.fidc.auth.usecase.session.dto.result.FidcPermissionGetPermissionsResult

interface FidcPermissionService {
    fun getPermissions(params: FidcPermissionGetPermissionsParams): FidcPermissionGetPermissionsResult
}