package com.trioForce.voyage.i18n

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

data class TranslateRequest(
    @field:NotBlank val sourceLocale: String = "en-US",
    @field:NotEmpty val targetLocales: List<String>,
    @field:NotEmpty val texts: List<String>,
    val glossary: String? = null,
)

data class TranslateResponse(
    val translations: Map<String, List<String>>,
)

data class TranslateEntityRequest(
    @field:NotBlank val entityType: String,
    @field:NotBlank val sourceLocale: String = "en-US",
    @field:NotEmpty val targetLocales: List<String>,
    /** 实体主键或编码，由 entityType 解释 */
    val entityId: Long? = null,
    val fields: List<String> = listOf("name"),
)

@RestController
class I18nAdminController(
    private val translationService: TranslationService,
    private val translationConfigService: I18nTranslationConfigService,
) {
    /** 返回系统支持的前台 locale 列表 */
    @GetMapping("/api/v1/admin/i18n/supported-locales")
    fun supportedLocales(): ApiResponse<Map<String, List<String>>> =
        ok(mapOf("locales" to I18nLocaleSupport.STOREFRONT_LOCALES))

    /** 机器翻译配置（DeepL 等） */
    @GetMapping("/api/v1/admin/i18n/translation-settings")
    fun getTranslationSettings(): ApiResponse<I18nTranslationSettingsView> =
        ok(translationConfigService.getAdminView())

    @PutMapping("/api/v1/admin/i18n/translation-settings")
    fun updateTranslationSettings(
        @Valid @RequestBody req: I18nTranslationSettingsUpdateRequest,
    ): ApiResponse<I18nTranslationSettingsView> =
        ok(translationConfigService.updateAdmin(req))

    /** 测试翻译连通性 */
    @PostMapping("/api/v1/admin/i18n/translation-settings/test")
    fun testTranslation(@Valid @RequestBody req: I18nTranslationTestRequest): ApiResponse<I18nTranslationTestResponse> {
        val translated = translationService.testTranslation(req.text, req.sourceLocale, req.targetLocale)
        return ok(I18nTranslationTestResponse(translated = translated))
    }

    /** 批量文本翻译（管理端一键翻译、i18n-sync 脚本共用） */
    @PostMapping("/api/v1/admin/i18n/translate")
    fun translate(@Valid @RequestBody req: TranslateRequest): ApiResponse<TranslateResponse> {
        val map = translationService.translateBatch(req.texts, req.sourceLocale, req.targetLocales)
        return ok(TranslateResponse(translations = map))
    }

    /**
     * 按实体类型翻译缺失语言（当前实现为通用文本翻译入口，实体回填由前端完成）。
     */
    @PostMapping("/api/v1/admin/i18n/translate-entity")
    fun translateEntity(@Valid @RequestBody req: TranslateEntityRequest): ApiResponse<TranslateResponse> {
        val map = translationService.translateBatch(
            texts = req.fields,
            sourceLocale = req.sourceLocale,
            targetLocales = req.targetLocales,
        )
        return ok(TranslateResponse(translations = map))
    }
}
