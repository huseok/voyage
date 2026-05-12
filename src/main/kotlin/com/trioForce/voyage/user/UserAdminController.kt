package com.trioForce.voyage.user

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class UserAdminController(
    private val userAdminService: UserAdminService,
) {
    /**
     * 后台分页查询注册用户（客户），含会员档位与累计消费（无会员行时视为 NONE / 0）。
     */
    @GetMapping("/api/v1/admin/customers")
    fun listCustomers(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) q: String?,
    ): ApiResponse<PagedCustomers> = ok(userAdminService.listCustomers(page, size, q))
}
