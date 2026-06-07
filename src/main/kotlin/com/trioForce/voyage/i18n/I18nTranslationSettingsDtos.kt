package com.trioForce.voyage.i18n

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class I18nTranslationSettingsView(
    val provider: String,
    val apiUrl: String,
    val defaultSourceLocale: String,
    val maxTextsPerRequest: Int,
    val enabled: Boolean,
    /** 是否已配置可用 Key（库内或环境变量） */
    val apiKeyConfigured: Boolean,
    /** 脱敏展示，如 ****abcd */
    val apiKeyMask: String?,
    /** 环境变量 VOYAGE_I18N_API_KEY 是否优先生效 */
    val envApiKeyActive: Boolean,
    val updatedAt: String,
)

data class I18nTranslationSettingsUpdateRequest(
    @field:NotBlank
    @field:Size(max = 32)
    val provider: String,
    @field:NotBlank
    @field:Size(max = 512)
    val apiUrl: String,
    @field:NotBlank
    @field:Size(max = 16)
    val defaultSourceLocale: String,
    @field:Min(1)
    @field:Max(50)
    val maxTextsPerRequest: Int,
    val enabled: Boolean,
    /** 新 API Key；留空表示不修改 */
    @field:Size(max = 512)
    val apiKey: String? = null,
    /** 为 true 时清空库内 API Key（不影响环境变量） */
    val clearApiKey: Boolean? = null,
)

data class I18nTranslationTestRequest(
    @field:NotBlank val text: String,
    @field:NotBlank val sourceLocale: String = "en-US",
    @field:NotBlank val targetLocale: String = "zh-CN",
)

data class I18nTranslationTestResponse(
    val translated: String,
)
