package com.trioForce.voyage.payment

import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/** 运行时生效的 PayPal 配置 */
data class EffectivePaymentConfig(
    val provider: String,
    val enabled: Boolean,
    val sandbox: Boolean,
    val clientId: String,
    val clientSecret: String,
    val webhookId: String,
    val brandName: String,
    val returnUrlBase: String,
    val apiBase: String,
)

/**
 * 支付配置：库内可维护；环境变量 `VOYAGE_PAYPAL_CLIENT_*` 非空时优先于库内凭据。
 */
@Service
class PaymentConfigService(
    private val repository: PaymentSettingsRepository,
    private val envProps: PaymentProperties,
    @Value("\${app.cors.allowed-origins}") private val corsOrigins: String,
) {
    private val log = LogUtil.logger<PaymentConfigService>()

    /** 获取或初始化库内配置行 */
    @Transactional(readOnly = true)
    fun getEntity(): PaymentSettingsEntity =
        repository.findById(1L).orElse(PaymentSettingsEntity())

    /** 解析当前生效的 PayPal 参数 */
    @Transactional(readOnly = true)
    fun resolveEffective(): EffectivePaymentConfig {
        val db = getEntity()
        val envId = envProps.clientId.trim()
        val envSecret = envProps.clientSecret.trim()
        val envWebhook = envProps.webhookId.trim()
        val dbId = db.clientId?.trim().orEmpty()
        val dbSecret = db.clientSecret?.trim().orEmpty()
        val clientId = if (envId.isNotEmpty()) envId else dbId
        val clientSecret = if (envSecret.isNotEmpty()) envSecret else dbSecret
        val webhookId = if (envWebhook.isNotEmpty()) envWebhook else db.webhookId?.trim().orEmpty()
        val returnBase = db.returnUrlBase?.trim().orEmpty().ifEmpty { defaultFrontendBase() }
        val sandbox = db.sandbox
        return EffectivePaymentConfig(
            provider = db.provider.ifBlank { "paypal" },
            enabled = db.enabled,
            sandbox = sandbox,
            clientId = clientId,
            clientSecret = clientSecret,
            webhookId = webhookId,
            brandName = db.brandName.ifBlank { "Globuy" },
            returnUrlBase = returnBase,
            apiBase = if (sandbox) "https://api-m.sandbox.paypal.com" else "https://api-m.paypal.com",
        )
    }

    @Transactional(readOnly = true)
    fun getPublicConfig(): PaymentPublicConfigView {
        val eff = resolveEffective()
        return PaymentPublicConfigView(
            provider = eff.provider,
            enabled = eff.enabled && eff.clientId.isNotBlank(),
            sandbox = eff.sandbox,
            clientId = eff.clientId.ifBlank { null },
            brandName = eff.brandName,
        )
    }

    @Transactional(readOnly = true)
    fun getAdminView(): PaymentSettingsView {
        val db = getEntity()
        val envActive = envProps.clientId.trim().isNotEmpty() || envProps.clientSecret.trim().isNotEmpty()
        val dbId = db.clientId?.trim().orEmpty()
        val dbSecret = db.clientSecret?.trim().orEmpty()
        val idSource = if (envProps.clientId.trim().isNotEmpty()) envProps.clientId.trim() else dbId
        val secretSource = if (envProps.clientSecret.trim().isNotEmpty()) envProps.clientSecret.trim() else dbSecret
        return PaymentSettingsView(
            provider = db.provider,
            enabled = db.enabled,
            sandbox = db.sandbox,
            brandName = db.brandName,
            returnUrlBase = db.returnUrlBase,
            clientIdConfigured = idSource.isNotEmpty(),
            clientIdMask = maskSecret(idSource),
            clientSecretConfigured = secretSource.isNotEmpty(),
            clientSecretMask = maskSecret(secretSource),
            webhookIdConfigured = db.webhookId?.trim().orEmpty().isNotEmpty() || envProps.webhookId.trim().isNotEmpty(),
            envCredentialsActive = envActive,
            updatedAt = db.updatedAt.toString(),
        )
    }

    @Transactional
    fun updateAdmin(req: PaymentSettingsUpdateRequest): PaymentSettingsView {
        val entity = repository.findById(1L).orElse(PaymentSettingsEntity())
        if (entity.provider != "paypal") {
            entity.provider = "paypal"
        }
        entity.enabled = req.enabled
        entity.sandbox = req.sandbox
        entity.brandName = req.brandName.trim().ifEmpty { "Globuy" }
        entity.returnUrlBase = req.returnUrlBase?.trim()?.ifEmpty { null }
        if (!req.clientId.isNullOrBlank()) {
            entity.clientId = req.clientId.trim()
            log.info("已更新库内 PayPal Client ID")
        }
        if (req.clearClientSecret == true) {
            entity.clientSecret = null
            log.info("已清空库内 PayPal Client Secret")
        } else if (!req.clientSecret.isNullOrBlank()) {
            entity.clientSecret = req.clientSecret.trim()
            log.info("已更新库内 PayPal Client Secret")
        }
        if (!req.webhookId.isNullOrBlank()) {
            entity.webhookId = req.webhookId.trim()
        }
        entity.updatedAt = OffsetDateTime.now()
        repository.save(entity)
        return getAdminView()
    }

    /** 支付回跳 URL 前缀：库内配置 > CORS 首域名 */
    fun resolveReturnUrlBase(): String = resolveEffective().returnUrlBase

    private fun defaultFrontendBase(): String =
        corsOrigins.split(",")
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?: throw BizException("未配置支付回跳地址，请在后台填写 return_url_base 或设置 APP_CORS_ALLOWED_ORIGINS")

    private fun maskSecret(value: String): String? {
        val v = value.trim()
        if (v.isEmpty()) return null
        if (v.length <= 4) return "****"
        return "****${v.takeLast(4)}"
    }
}
