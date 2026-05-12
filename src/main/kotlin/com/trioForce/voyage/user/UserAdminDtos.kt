package com.trioForce.voyage.user

import java.time.OffsetDateTime

data class CustomerAdminView(
    val id: Long,
    val email: String,
    val name: String,
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
)

data class PagedCustomers(
    val items: List<CustomerAdminView>,
    val total: Long,
    val page: Int,
    val size: Int,
)
