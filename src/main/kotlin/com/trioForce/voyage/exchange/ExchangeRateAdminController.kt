package com.trioForce.voyage.exchange

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
class ExchangeRateAdminController(
    private val exchangeRateService: ExchangeRateService,
) {
    @GetMapping("/api/v1/admin/exchange-rates")
    fun list(): ApiResponse<List<ExchangeRateView>> =
        ok(exchangeRateService.listAdmin())

    @GetMapping("/api/v1/admin/exchange-rate-settings")
    fun getSettings(): ApiResponse<ExchangeRateSettingsView> {
        val s = exchangeRateService.getSettings()
        return ok(
            ExchangeRateSettingsView(
                baseCurrency = s.baseCurrency,
                refreshIntervalHours = s.refreshIntervalHours,
                defaultMarkupPercent = s.defaultMarkupPercent.toPlainString(),
                providerUrl = s.providerUrl,
                updatedAt = s.updatedAt.toString(),
            ),
        )
    }

    @PutMapping("/api/v1/admin/exchange-rate-settings")
    fun updateSettings(@Valid @RequestBody req: ExchangeRateSettingsUpdateRequest): ApiResponse<ExchangeRateSettingsView> =
        ok(exchangeRateService.updateSettings(req))

    @PutMapping("/api/v1/admin/exchange-rates/{code}")
    fun updateCurrency(
        @PathVariable code: String,
        @Valid @RequestBody req: ExchangeRateAdminUpdateRequest,
    ): ApiResponse<ExchangeRateView> =
        ok(exchangeRateService.updateCurrency(code, req))

    @PostMapping("/api/v1/admin/exchange-rates/refresh")
    fun refresh(): ApiResponse<Map<String, Int>> =
        ok(mapOf("updated" to exchangeRateService.refreshMarketRates(force = true)))
}
