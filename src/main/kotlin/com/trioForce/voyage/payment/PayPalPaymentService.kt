package com.trioForce.voyage.payment

import com.trioForce.voyage.common.logging.LogUtil
import com.trioForce.voyage.order.OrderService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** 将业务订单与 PayPal Checkout 对接 */
@Service
class PayPalPaymentService(
    private val paymentConfigService: PaymentConfigService,
    private val payPalApiClient: PayPalApiClient,
    private val orderService: OrderService,
) {
    private val log = LogUtil.logger<PayPalPaymentService>()

    /** 创建 PayPal 支付并返回跳转链接 */
    @Transactional
    fun createPayment(orderNo: String): PayPalCreateResponse {
        val order = orderService.getOrderEntityForPayment(orderNo)
        val amount = order.totalAmount.setScale(2).toPlainString()
        val base = paymentConfigService.resolveReturnUrlBase().trimEnd('/')
        val encodedNo = URLEncoder.encode(orderNo, StandardCharsets.UTF_8)
        val returnUrl = "$base/orders/$encodedNo?paypal=return"
        val cancelUrl = "$base/orders/$encodedNo?paypal=cancel"
        val created = payPalApiClient.createCheckoutOrder(
            ourOrderNo = orderNo,
            currency = order.currency,
            amount = amount,
            returnUrl = returnUrl,
            cancelUrl = cancelUrl,
        )
        orderService.attachPayPalOrderId(orderNo, created.paypalOrderId)
        return PayPalCreateResponse(
            paypalOrderId = created.paypalOrderId,
            approveUrl = created.approveUrl,
        )
    }

    /** 捕获 PayPal 支付并将业务订单标为已付（服务端校验金额与订单号） */
    @Transactional
    fun capturePayment(orderNo: String, paypalOrderId: String): PayPalCaptureResponse {
        val order = orderService.getOrderEntityForPayment(orderNo)
        val capture = payPalApiClient.captureCheckoutOrder(paypalOrderId)
        PayPalCaptureVerifier.verify(capture, orderNo, order.currency, order.totalAmount)
        val view = orderService.markPaidFromPayPal(orderNo, paypalOrderId)
        log.info("PayPal 支付完成 orderNo={} paypalOrderId={}", orderNo, paypalOrderId)
        return PayPalCaptureResponse(
            orderNo = view.orderNo,
            paymentStatus = view.paymentStatus,
            status = view.status,
        )
    }
}
