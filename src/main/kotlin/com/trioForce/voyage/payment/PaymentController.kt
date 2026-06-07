package com.trioForce.voyage.payment

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** 前台公开：支付开关与 PayPal Client ID（不含密钥） */
@RestController
class PaymentController(
    private val paymentConfigService: PaymentConfigService,
) {
    @GetMapping("/api/v1/payments/config")
    fun publicConfig(): ApiResponse<PaymentPublicConfigView> =
        ok(paymentConfigService.getPublicConfig())
}
