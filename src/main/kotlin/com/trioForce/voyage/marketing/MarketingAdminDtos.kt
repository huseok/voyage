package com.trioForce.voyage.marketing

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.time.OffsetDateTime

data class CouponAdminView(
    val id: Long,
    val code: String,
    val name: String,
    val discountType: String,
    val discountValue: String,
    val minOrderAmount: String,
    val tagFilter: String?,
    val validFrom: OffsetDateTime,
    val validTo: OffsetDateTime,
    val isActive: Boolean,
)

data class CouponAdminUpsertRequest(
    @field:NotBlank val code: String,
    @field:NotBlank val name: String,
    /** `PERCENT` 或 `FIXED` */
    @field:NotBlank val discountType: String,
    @field:DecimalMin("0.01") val discountValue: BigDecimal,
    @field:DecimalMin("0.0") val minOrderAmount: BigDecimal = BigDecimal.ZERO,
    val tagFilter: String? = null,
    val validFrom: OffsetDateTime,
    val validTo: OffsetDateTime,
    val isActive: Boolean = true,
)

data class CouponActivePatchRequest(
    val isActive: Boolean,
)

data class PromotionAdminView(
    val id: Long,
    val name: String,
    val thresholdAmount: String,
    val amountOff: String,
    val tagFilter: String?,
    val validFrom: OffsetDateTime,
    val validTo: OffsetDateTime,
    val isActive: Boolean,
)

data class PromotionAdminUpsertRequest(
    @field:NotBlank val name: String,
    @field:DecimalMin("0.01") val thresholdAmount: BigDecimal,
    @field:DecimalMin("0.01") val amountOff: BigDecimal,
    val tagFilter: String? = null,
    val validFrom: OffsetDateTime,
    val validTo: OffsetDateTime,
    val isActive: Boolean = true,
)

data class PromotionActivePatchRequest(
    val isActive: Boolean,
)
