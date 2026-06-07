package com.trioForce.voyage.payment

import com.fasterxml.jackson.databind.JsonNode
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
import java.math.BigDecimal
import java.math.RoundingMode

/** 校验 PayPal 捕获结果与业务订单一致，防止伪造或金额篡改 */
object PayPalCaptureVerifier {
    private val log = LogUtil.logger<PayPalCaptureVerifier>()

    /**
     * 捕获响应须为 COMPLETED，且 reference_id、币种、金额与业务订单一致。
     */
    fun verify(
        capture: JsonNode,
        expectedOrderNo: String,
        expectedCurrency: String,
        expectedAmount: BigDecimal,
    ) {
        val status = capture.path("status").asText("").trim()
        if (status != "COMPLETED") {
            log.warn("PayPal 捕获状态异常 orderNo={} status={}", expectedOrderNo, status)
            throw BizException("PayPal 支付未完成")
        }
        val unit = capture.path("purchase_units").firstOrNull()
            ?: throw BizException("PayPal 捕获数据无效")
        val referenceId = unit.path("reference_id").asText("").trim()
        if (referenceId.isNotEmpty() && referenceId != expectedOrderNo) {
            log.warn("PayPal reference_id 不匹配 expected={} actual={}", expectedOrderNo, referenceId)
            throw BizException("PayPal 订单与业务订单不匹配")
        }
        val captureNode = unit.path("payments").path("captures").firstOrNull()
            ?: throw BizException("PayPal 捕获金额缺失")
        val currency = captureNode.path("amount").path("currency_code").asText("").trim()
        val valueText = captureNode.path("amount").path("value").asText("").trim()
        if (currency.isEmpty() || valueText.isEmpty()) {
            throw BizException("PayPal 捕获金额无效")
        }
        val paid = try {
            BigDecimal(valueText).setScale(2, RoundingMode.HALF_UP)
        } catch (_: NumberFormatException) {
            throw BizException("PayPal 捕获金额无效")
        }
        val expected = expectedAmount.setScale(2, RoundingMode.HALF_UP)
        if (!currency.equals(expectedCurrency, ignoreCase = true)) {
            log.warn("PayPal 币种不匹配 orderNo={} expected={} actual={}", expectedOrderNo, expectedCurrency, currency)
            throw BizException("PayPal 支付币种与订单不符")
        }
        if (paid.compareTo(expected) != 0) {
            log.warn("PayPal 金额不匹配 orderNo={} expected={} actual={}", expectedOrderNo, expected, paid)
            throw BizException("PayPal 支付金额与订单不符")
        }
    }
}
