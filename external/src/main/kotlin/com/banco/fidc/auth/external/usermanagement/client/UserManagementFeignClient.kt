package com.banco.fidc.auth.external.usermanagement.client

import com.banco.fidc.auth.external.usermanagement.dto.response.UserManagementGetUserResponse
import org.springframework.cloud.openfeign.FeignClient
import org.springframework.web.bind.annotation.*

@FeignClient(
    name = "user-management",
    url = "\${external-apis.user-management.base-url}"
)
interface UserManagementFeignClient {

    @GetMapping("/users")
    fun getUser(
        @RequestHeader("partner") partner: String,
        @RequestHeader("cpf") cpf: String
    ): UserManagementGetUserResponse?
}