package com.trioForce.voyage.payment

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

/** 用户侧：为业务订单创建 / 捕获 PayPal 支付 */
@RestController
class PayPalOrderController(
    private val payPalPaymentService: PayPalPaymentService,
) {
    /** 创建 PayPal Checkout 订单，返回审批跳转链接 */
    @PostMapping("/api/v1/orders/{orderNo}/paypal/create")
    fun create(@PathVariable orderNo: String): ApiResponse<PayPalCreateResponse> =
        ok(payPalPaymentService.createPayment(orderNo))

    /** 用户在 PayPal 审批后回跳，前端携带 paypalOrderId 完成捕获 */
    @PostMapping("/api/v1/orders/{orderNo}/paypal/capture")
    fun capture(
        @PathVariable orderNo: String,
        @Valid @RequestBody req: PayPalCaptureRequest,
    ): ApiResponse<PayPalCaptureResponse> =
        ok(payPalPaymentService.capturePayment(orderNo, req.paypalOrderId))
}
