package com.trioForce.voyage.i18n

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/** 后台：UI 文案目录 CRUD、导入导出与种子初始化 */
@RestController
class I18nCatalogAdminController(
    private val catalogService: I18nCatalogService,
) {
    @GetMapping("/api/v1/admin/i18n/catalogs")
    fun list(): ApiResponse<List<I18nCatalogSummaryView>> = ok(catalogService.listSummaries())

    @GetMapping("/api/v1/admin/i18n/catalogs/{locale}")
    fun getCatalog(@PathVariable locale: String): ApiResponse<Map<String, Any>> =
        ok(catalogService.getCatalog(locale))

    @GetMapping("/api/v1/admin/i18n/catalogs/{locale}/entries")
    fun listEntries(
        @PathVariable locale: String,
        @RequestParam(defaultValue = "1") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) q: String?,
    ): ApiResponse<I18nCatalogEntriesPage> =
        ok(catalogService.listEntries(locale, page, size, q))

    @PutMapping("/api/v1/admin/i18n/catalogs/{locale}/entries")
    fun patchEntries(
        @PathVariable locale: String,
        @Valid @RequestBody req: I18nCatalogPatchRequest,
    ): ApiResponse<I18nCatalogSummaryView> =
        ok(catalogService.patchEntries(locale, req.entries))

    @PostMapping("/api/v1/admin/i18n/catalogs/import")
    fun importCatalog(@Valid @RequestBody req: I18nCatalogImportRequest): ApiResponse<I18nCatalogSummaryView> =
        ok(catalogService.importCatalog(req.locale, req.content, req.overwrite))

    /** 导出与 GET catalog 相同，语义上供下载使用 */
    @GetMapping("/api/v1/admin/i18n/catalogs/{locale}/export")
    fun exportCatalog(@PathVariable locale: String): ApiResponse<Map<String, Any>> =
        ok(catalogService.getCatalog(locale))

    @PostMapping("/api/v1/admin/i18n/catalogs/seed")
    fun seed(@Valid @RequestBody req: I18nCatalogSeedRequest): ApiResponse<List<I18nCatalogSummaryView>> =
        ok(catalogService.seedFromClasspath(req.locales, req.force))

    @DeleteMapping("/api/v1/admin/i18n/catalogs/{locale}")
    fun delete(@PathVariable locale: String): ApiResponse<Map<String, String>> {
        catalogService.deleteCatalog(locale)
        return ok(mapOf("locale" to locale))
    }
}
