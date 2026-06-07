package com.trioForce.voyage.i18n

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty

data class I18nCatalogSummaryView(
    val locale: String,
    val keyCount: Int,
    val updatedAt: String,
)

data class I18nCatalogMetaView(
    val version: String,
    val locales: List<String>,
)

data class I18nCatalogEntryView(
    val key: String,
    val value: String,
)

data class I18nCatalogEntriesPage(
    val items: List<I18nCatalogEntryView>,
    val total: Int,
    val page: Int,
    val size: Int,
)

data class I18nCatalogPatchRequest(
    @field:NotEmpty val entries: Map<String, String>,
)

data class I18nCatalogImportRequest(
    @field:NotBlank val locale: String,
    val content: Map<String, Any>,
    /** true：导入内容覆盖已有 key；false：仅填充缺失 key */
    val overwrite: Boolean = true,
)

data class I18nCatalogSeedRequest(
    val locales: List<String> = listOf("en-US", "zh-CN"),
    /** true：已存在的 locale 也会被种子文件覆盖 */
    val force: Boolean = false,
)
