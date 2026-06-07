package com.trioForce.voyage.i18n

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController

/** 前台公开：按 locale 读取 UI 文案目录 */
@RestController
class I18nCatalogController(
    private val catalogService: I18nCatalogService,
) {
    /** 目录版本与已发布 locale 列表（供前台缓存失效） */
    @GetMapping("/api/v1/i18n/catalogs/meta")
    fun meta(): ApiResponse<I18nCatalogMetaView> = ok(catalogService.getMeta())

    /**
     * 完整文案目录。
     * 库中无记录时返回空对象，前台回退到打包的静态 JSON。
     */
    @GetMapping("/api/v1/i18n/catalogs/{locale}")
    fun getCatalog(@PathVariable locale: String): ApiResponse<Map<String, Any>> {
        val content = catalogService.findCatalog(locale) ?: emptyMap()
        return ok(content)
    }
}
