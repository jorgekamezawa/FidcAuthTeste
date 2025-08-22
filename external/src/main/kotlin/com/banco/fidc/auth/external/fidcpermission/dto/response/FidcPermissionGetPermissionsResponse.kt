package com.banco.fidc.auth.external.fidcpermission.dto.response

import com.banco.fidc.auth.usecase.session.dto.result.FidcPermissionGetPermissionsResult

data class FidcPermissionGetPermissionsResponse(
    val permissions: List<String>
)

// Mapper to result
fun FidcPermissionGetPermissionsResponse.toResult(): FidcPermissionGetPermissionsResult {
    return FidcPermissionGetPermissionsResult(
        permissions = this.permissions
    )
}