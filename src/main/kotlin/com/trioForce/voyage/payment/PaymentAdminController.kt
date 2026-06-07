package com.trioForce.voyage.payment

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/** 后台：PayPal 等支付配置 */
@RestController
class PaymentAdminController(
    private val paymentConfigService: PaymentConfigService,
) {
    @GetMapping("/api/v1/admin/payment-settings")
    fun getSettings(): ApiResponse<PaymentSettingsView> =
        ok(paymentConfigService.getAdminView())

    @PutMapping("/api/v1/admin/payment-settings")
    fun updateSettings(@Valid @RequestBody req: PaymentSettingsUpdateRequest): ApiResponse<PaymentSettingsView> =
        ok(paymentConfigService.updateAdmin(req))
}
