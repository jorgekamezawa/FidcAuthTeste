package com.banco.fidc.auth.external.fidcpermission.client

import com.banco.fidc.auth.external.fidcpermission.dto.response.FidcPermissionGetPermissionsResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(
    name = "fidc-permission",
    url = "\${external-apis.fidc-permission.base-url}"
)
interface FidcPermissionFeignClient {

    @GetMapping("/permissions")
    fun getPermissions(
        @RequestHeader("partner") partner: String,
        @RequestHeader("cpf") cpf: String,
        @RequestHeader("relationshipId") relationshipId: String?
    ): FidcPermissionGetPermissionsResponse?
}