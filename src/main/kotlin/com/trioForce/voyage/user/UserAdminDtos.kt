package com.trioForce.voyage.user

import java.time.OffsetDateTime

data class CustomerAdminView(
    val id: Long,
    val email: String,
    val name: String,
    /** 用户注册时填写的称呼 */
    val salutation: String,
    val phone: String?,
    val country: String?,
    val role: String,
    val status: String,
    val createdAt: OffsetDateTime,
    /** 会员档位编码，如 NONE、BRONZE；无累计记录时为 NONE */
    val tier: String,
    /** 累计已支付金额（与 [com.trioForce.voyage.loyalty.MembershipService] 说明一致，当前为数值累加未做真实汇率） */
    val lifetimePaidUsd: String,
    /** 当前档位商品小计折扣百分比 0～8 */
    val memberDiscountPercent: Int,
    /** 后台运营备注 */
    val adminNote: String?,
    /** 偏好/标签等自由文本（可为 JSON 字符串） */
    val preferences: String?,
)

data class CustomerAdminUpdateRequest(
    val adminNote: String? = null,
    val preferences: String? = null,
)

/** 重置密码接口一次性返回临时密码，由管理员线下告知客户；请勿写入持久化日志正文。 */
data class AdminResetPasswordResponse(
    val temporaryPassword: String,
)

data class PagedCustomers(
    val items: List<CustomerAdminView>,
    val total: Long,
    val page: Int,
    val size: Int,
)
