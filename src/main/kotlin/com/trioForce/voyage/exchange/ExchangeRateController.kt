package com.trioForce.voyage.exchange

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** 前台匿名可读：展示币种换算用有效汇率 */
@RestController
class ExchangeRateController(
    private val exchangeRateService: ExchangeRateService,
) {
    @GetMapping("/api/v1/exchange-rates")
    fun listPublic(): ApiResponse<ExchangeRatePublicView> =
        ok(exchangeRateService.getPublicRates())
}
