package com.trioForce.voyage.exchange

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.math.BigDecimal

data class ExchangeRateView(
    val currencyCode: String,
    val marketRate: String,
    val effectiveRate: String,
    val markupPercent: String,
    val markupAmount: String,
    val frozenRate: String?,
    val freezeUntil: String?,
    val refreshIntervalHours: Int?,
    val lastFetchedAt: String?,
    val enabled: Boolean,
)

data class ExchangeRatePublicView(
    val baseCurrency: String,
    val refreshIntervalHours: Int,
    val rates: List<ExchangeRatePublicItem>,
)

data class ExchangeRatePublicItem(
    val currencyCode: String,
    val effectiveRate: String,
    val lastFetchedAt: String?,
)

data class ExchangeRateSettingsView(
    val baseCurrency: String,
    val refreshIntervalHours: Int,
    val defaultMarkupPercent: String,
    val providerUrl: String,
    val updatedAt: String,
)

data class ExchangeRateSettingsUpdateRequest(
    @field:Min(1)
    @field:Max(168)
    val refreshIntervalHours: Int,
    @field:DecimalMin("0")
    val defaultMarkupPercent: BigDecimal,
    @field:NotBlank
    @field:Size(max = 512)
    val providerUrl: String,
)

data class ExchangeRateAdminUpdateRequest(
    @field:DecimalMin("0")
    val markupPercent: BigDecimal? = null,
    @field:DecimalMin("0")
    val markupAmount: BigDecimal? = null,
    val frozenRate: BigDecimal? = null,
    val freezeUntil: String? = null,
    @field:Min(1)
    @field:Max(168)
    val refreshIntervalHours: Int? = null,
    val enabled: Boolean? = null,
    /** 立即解除冻结并恢复按行情计算 */
    val clearFreeze: Boolean? = null,
)
