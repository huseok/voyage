package com.trioForce.voyage.loyalty

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.OffsetDateTime

data class MembershipTierRuleAdminView(
    val id: Long,
    val tierCode: String,
    val minLifetimePaidUsd: String,
    val discountPercent: Int,
    val sortNo: Int,
    val isActive: Boolean,
    val createdAt: OffsetDateTime,
    val updatedAt: OffsetDateTime,
)

data class MembershipTierRuleUpsertRequest(
    @field:NotBlank val tierCode: String,
    @field:DecimalMin("0.0") val minLifetimePaidUsd: BigDecimal,
    @field:Min(0) @field:Max(100) val discountPercent: Int,
    @field:Min(0) val sortNo: Int = 0,
    val isActive: Boolean = true,
)

data class MembershipTierRuleActivePatchRequest(
    val isActive: Boolean,
)
