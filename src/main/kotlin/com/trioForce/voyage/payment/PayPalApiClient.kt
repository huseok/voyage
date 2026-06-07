package com.trioForce.voyage.payment

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException
import java.util.Base64

data class PayPalCreateResult(
    val paypalOrderId: String,
    val approveUrl: String,
)

/** PayPal REST API v2 客户端（OAuth + 创建/捕获订单） */
@Component
class PayPalApiClient(
    private val paymentConfigService: PaymentConfigService,
    private val objectMapper: ObjectMapper,
) {
    private val log = LogUtil.logger<PayPalApiClient>()
    private val restClient: RestClient = RestClient.builder().build()

    /** 创建 PayPal Checkout 订单并返回审批链接 */
    fun createCheckoutOrder(
        ourOrderNo: String,
        currency: String,
        amount: String,
        returnUrl: String,
        cancelUrl: String,
    ): PayPalCreateResult {
        val cfg = paymentConfigService.resolveEffective()
        ensureConfigured(cfg)
        val token = fetchAccessToken(cfg)
        val body = buildCreateBody(ourOrderNo, currency, amount, returnUrl, cancelUrl, cfg.brandName)
        val resp = postJson(cfg, token, "/v2/checkout/orders", body)
        val id = resp.path("id").asText("").trim()
        if (id.isEmpty()) {
            log.warn("PayPal 创建订单响应缺少 id")
            throw BizException("PayPal 创建订单失败")
        }
        val approveUrl = resp.path("links")
            .firstOrNull { it.path("rel").asText() == "approve" }
            ?.path("href")
            ?.asText()
            ?.trim()
            .orEmpty()
        if (approveUrl.isEmpty()) {
            throw BizException("PayPal 未返回支付跳转链接")
        }
        log.info("PayPal 订单已创建 ourOrderNo={} paypalOrderId={}", ourOrderNo, id)
        return PayPalCreateResult(paypalOrderId = id, approveUrl = approveUrl)
    }

    /** 捕获已审批的 PayPal 订单 */
    fun captureCheckoutOrder(paypalOrderId: String): JsonNode {
        val cfg = paymentConfigService.resolveEffective()
        ensureConfigured(cfg)
        val token = fetchAccessToken(cfg)
        return try {
            val resp = restClient.post()
                .uri("${cfg.apiBase}/v2/checkout/orders/$paypalOrderId/capture")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(JsonNode::class.java)
            resp ?: throw BizException("PayPal 捕获无响应")
        } catch (ex: RestClientResponseException) {
            log.warn("PayPal 捕获失败 paypalOrderId={} status={} body={}", paypalOrderId, ex.statusCode, ex.responseBodyAsString)
            throw BizException("PayPal 支付捕获失败，请稍后重试或联系客服")
        }
    }

    private fun fetchAccessToken(cfg: EffectivePaymentConfig): String {
        val basic = Base64.getEncoder().encodeToString("${cfg.clientId}:${cfg.clientSecret}".toByteArray(Charsets.UTF_8))
        return try {
            val resp = restClient.post()
                .uri("${cfg.apiBase}/v1/oauth2/token")
                .header("Authorization", "Basic $basic")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body("grant_type=client_credentials")
                .retrieve()
                .body(JsonNode::class.java)
            val token = resp?.path("access_token")?.asText()?.trim().orEmpty()
            if (token.isEmpty()) throw BizException("PayPal 鉴权失败")
            token
        } catch (ex: RestClientResponseException) {
            log.warn("PayPal OAuth 失败 status={}", ex.statusCode)
            throw BizException("PayPal 鉴权失败，请检查 Client ID / Secret 与沙箱开关")
        }
    }

    private fun postJson(cfg: EffectivePaymentConfig, token: String, path: String, body: ObjectNode): JsonNode {
        return try {
            restClient.post()
                .uri("${cfg.apiBase}$path")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode::class.java)
                ?: throw BizException("PayPal 请求无响应")
        } catch (ex: RestClientResponseException) {
            log.warn("PayPal 请求失败 path={} status={} body={}", path, ex.statusCode, ex.responseBodyAsString)
            throw BizException("PayPal 请求失败")
        }
    }

    private fun buildCreateBody(
        ourOrderNo: String,
        currency: String,
        amount: String,
        returnUrl: String,
        cancelUrl: String,
        brandName: String,
    ): ObjectNode {
        val purchaseUnit = objectMapper.createObjectNode().apply {
            put("reference_id", ourOrderNo)
            set<ObjectNode>(
                "amount",
                objectMapper.createObjectNode().apply {
                    put("currency_code", currency.uppercase())
                    put("value", amount)
                },
            )
        }
        val appContext = objectMapper.createObjectNode().apply {
            put("brand_name", brandName)
            put("landing_page", "NO_PREFERENCE")
            put("user_action", "PAY_NOW")
            put("return_url", returnUrl)
            put("cancel_url", cancelUrl)
        }
        return objectMapper.createObjectNode().apply {
            put("intent", "CAPTURE")
            set<ObjectNode>("purchase_units", objectMapper.createArrayNode().add(purchaseUnit))
            set<ObjectNode>("application_context", appContext)
        }
    }

    private fun ensureConfigured(cfg: EffectivePaymentConfig) {
        if (!cfg.enabled) throw BizException("PayPal 支付未启用")
        if (cfg.clientId.isBlank() || cfg.clientSecret.isBlank()) {
            throw BizException("PayPal 凭据未配置")
        }
    }
}
