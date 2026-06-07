package com.trioForce.voyage.i18n

import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/** 运行时生效的翻译配置 */
data class EffectiveTranslationConfig(
    val provider: String,
    val apiKey: String,
    val apiUrl: String,
    val defaultSourceLocale: String,
    val maxTextsPerRequest: Int,
    val enabled: Boolean,
)

/**
 * 机器翻译配置：库内可维护；环境变量 `VOYAGE_I18N_API_KEY` 非空时优先于库内 Key。
 */
@Service
class I18nTranslationConfigService(
    private val repository: I18nTranslationSettingsRepository,
    private val envProps: TranslationProperties,
) {
    private val log = LogUtil.logger<I18nTranslationConfigService>()

    /** 获取或初始化库内配置行 */
    @Transactional(readOnly = true)
    fun getEntity(): I18nTranslationSettingsEntity =
        repository.findById(1L).orElse(I18nTranslationSettingsEntity())

    /** 解析当前生效的翻译参数 */
    @Transactional(readOnly = true)
    fun resolveEffective(): EffectiveTranslationConfig {
        val db = getEntity()
        val envKey = envProps.apiKey.trim()
        val dbKey = db.apiKey?.trim().orEmpty()
        val effectiveKey = if (envKey.isNotEmpty()) envKey else dbKey
        return EffectiveTranslationConfig(
            provider = db.provider.ifBlank { envProps.provider }.ifBlank { "deepl" },
            apiKey = effectiveKey,
            apiUrl = db.apiUrl.ifBlank { envProps.apiUrl }.ifBlank { "https://api-free.deepl.com" },
            defaultSourceLocale = db.defaultSourceLocale.ifBlank { envProps.defaultSourceLocale },
            maxTextsPerRequest = db.maxTextsPerRequest.coerceIn(1, 50),
            enabled = db.enabled,
        )
    }

    @Transactional(readOnly = true)
    fun getAdminView(): I18nTranslationSettingsView {
        val db = getEntity()
        val envKey = envProps.apiKey.trim()
        val dbKey = db.apiKey?.trim().orEmpty()
        val envActive = envKey.isNotEmpty()
        val configured = envActive || dbKey.isNotEmpty()
        val maskSource = if (envActive) envKey else dbKey
        return I18nTranslationSettingsView(
            provider = db.provider,
            apiUrl = db.apiUrl,
            defaultSourceLocale = db.defaultSourceLocale,
            maxTextsPerRequest = db.maxTextsPerRequest,
            enabled = db.enabled,
            apiKeyConfigured = configured,
            apiKeyMask = maskApiKey(maskSource),
            envApiKeyActive = envActive,
            updatedAt = db.updatedAt.toString(),
        )
    }

    @Transactional
    fun updateAdmin(req: I18nTranslationSettingsUpdateRequest): I18nTranslationSettingsView {
        val entity = repository.findById(1L).orElse(I18nTranslationSettingsEntity())
        val provider = req.provider.trim().lowercase()
        if (provider !in setOf("deepl", "stub")) {
            throw BizException("不支持的翻译提供商: ${req.provider}")
        }
        entity.provider = provider
        entity.apiUrl = req.apiUrl.trim()
        entity.defaultSourceLocale = req.defaultSourceLocale.trim()
        entity.maxTextsPerRequest = req.maxTextsPerRequest.coerceIn(1, 50)
        entity.enabled = req.enabled
        if (req.clearApiKey == true) {
            entity.apiKey = null
            log.info("已清空库内翻译 API Key")
        } else if (!req.apiKey.isNullOrBlank()) {
            entity.apiKey = req.apiKey.trim()
            log.info("已更新库内翻译 API Key")
        }
        entity.updatedAt = OffsetDateTime.now()
        repository.save(entity)
        return getAdminView()
    }

    private fun maskApiKey(key: String): String? {
        val k = key.trim()
        if (k.isEmpty()) return null
        if (k.length <= 4) return "****"
        return "****${k.takeLast(4)}"
    }
}
