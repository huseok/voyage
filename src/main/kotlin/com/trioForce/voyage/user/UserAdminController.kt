package com.trioForce.voyage.user

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 后台客户（注册用户）REST 接口。
 *
 * 具体业务与日志写在 [UserAdminService]；控制器保持薄封装，便于统一鉴权与路由扫描。
 */
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

    /** 更新后台备注与客户偏好文本。 */
    @PatchMapping("/api/v1/admin/customers/{id}")
    fun updateCustomer(
        @PathVariable id: Long,
        @Valid @RequestBody req: CustomerAdminUpdateRequest,
    ): ApiResponse<String> {
        userAdminService.updateCustomer(id, req)
        return ok("updated")
    }

    /**
     * 重置客户登录密码，返回一次性明文新密码（请通过安全渠道告知客户）。
     *
     * 请求体可选：[AdminResetPasswordRequest.newPassword] 有非空白值时使用该明文；否则使用客户邮箱作为新密码。
     */
    @PostMapping("/api/v1/admin/customers/{id}/reset-password")
    fun resetPassword(
        @PathVariable id: Long,
        @RequestBody(required = false) body: AdminResetPasswordRequest?,
    ): ApiResponse<AdminResetPasswordResponse> =
        ok(AdminResetPasswordResponse(temporaryPassword = userAdminService.resetPasswordReturnPlain(id, body)))
}
