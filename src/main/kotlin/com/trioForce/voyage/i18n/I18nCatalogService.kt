package com.trioForce.voyage.i18n

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/**
 * UI 文案目录：按 locale 存整份 JSON，支持分页检索、批量修改与导入导出。
 */
@Service
class I18nCatalogService(
    private val repository: I18nLocaleCatalogRepository,
    private val objectMapper: ObjectMapper,
) {
    private val log = LogUtil.logger<I18nCatalogService>()

    /** 前台/后台读取目录元信息（缓存版本号） */
    @Transactional(readOnly = true)
    fun getMeta(): I18nCatalogMetaView {
        val version = repository.maxUpdatedAt().toString()
        val locales = repository.findAll().map { it.locale }.sorted()
        return I18nCatalogMetaView(version = version, locales = locales)
    }

    /** 列出已入库的 locale 摘要 */
    @Transactional(readOnly = true)
    fun listSummaries(): List<I18nCatalogSummaryView> =
        repository.findAll()
            .sortedBy { it.locale }
            .map { toSummary(it) }

    /** 获取完整目录 JSON；不存在时抛业务异常 */
    @Transactional(readOnly = true)
    fun getCatalog(locale: String): Map<String, Any> {
        val normalized = normalizeLocale(locale)
        val entity = repository.findById(normalized).orElseThrow {
            BizException("文案目录不存在：$normalized")
        }
        return entity.content
    }

    /** 尝试读取目录；不存在返回 null（供公开接口回退静态资源） */
    @Transactional(readOnly = true)
    fun findCatalog(locale: String): Map<String, Any>? {
        val normalized = normalizeLocale(locale)
        return repository.findById(normalized).map { it.content }.orElse(null)
    }

    /** 分页检索扁平化后的文案条目（支持对照语言文案与 key 前缀筛选） */
    @Transactional(readOnly = true)
    fun listEntries(
        locale: String,
        page: Int,
        size: Int,
        q: String?,
        refLocale: String?,
        prefix: String?,
    ): I18nCatalogEntriesPage {
        val normalized = normalizeLocale(locale)
        val content = getCatalog(normalized)
        val keyword = q?.trim().orEmpty().lowercase()
        val prefixNorm = prefix?.trim().orEmpty()
        val refFlat: Map<String, String> = refLocale?.trim()?.takeIf { it.isNotEmpty() }?.let { ref ->
            findCatalog(ref)?.let { cat ->
                I18nCatalogUtils.flatten(cat).toMap()
            }
        } ?: emptyMap()
        val all = I18nCatalogUtils.flatten(content)
            .map { (key, value) -> I18nCatalogEntryView(key = key, value = value) }
            .filter { row ->
                val prefixOk = prefixNorm.isEmpty() ||
                    row.key == prefixNorm ||
                    row.key.startsWith("$prefixNorm.")
                if (!prefixOk) return@filter false
                if (keyword.isEmpty()) return@filter true
                val refText = refFlat[row.key].orEmpty().lowercase()
                row.key.lowercase().contains(keyword) ||
                    row.value.lowercase().contains(keyword) ||
                    refText.contains(keyword)
            }
            .sortedBy { it.key }
        val safePage = page.coerceAtLeast(1)
        val safeSize = size.coerceIn(1, 500)
        val from = (safePage - 1) * safeSize
        val items = if (from >= all.size) emptyList() else all.subList(from, minOf(from + safeSize, all.size))
        return I18nCatalogEntriesPage(
            items = items,
            total = all.size,
            page = safePage,
            size = safeSize,
        )
    }

    /** 批量按 key 路径更新文案 */
    @Transactional
    fun patchEntries(locale: String, updates: Map<String, String>): I18nCatalogSummaryView {
        val normalized = normalizeLocale(locale)
        val entity = repository.findById(normalized).orElseThrow {
            BizException("文案目录不存在：$normalized")
        }
        entity.content = I18nCatalogUtils.patchContent(entity.content, updates)
        entity.updatedAt = OffsetDateTime.now()
        log.info("更新 UI 文案目录 locale={} 条目数={}", normalized, updates.size)
        return toSummary(repository.save(entity))
    }

    /** 导入整份目录（合并或覆盖） */
    @Transactional
    fun importCatalog(locale: String, incoming: Map<String, Any>, overwrite: Boolean): I18nCatalogSummaryView {
        val normalized = normalizeLocale(locale)
        val existing = repository.findById(normalized).orElse(null)
        val merged = if (existing == null) {
            incoming
        } else {
            I18nCatalogUtils.mergeContent(existing.content, incoming, overwrite)
        }
        val saved = if (existing == null) {
            repository.save(
                I18nLocaleCatalogEntity(
                    locale = normalized,
                    content = merged,
                    updatedAt = OffsetDateTime.now(),
                ),
            )
        } else {
            existing.content = merged
            existing.updatedAt = OffsetDateTime.now()
            repository.save(existing)
        }
        log.info("导入 UI 文案目录 locale={} overwrite={} keyCount={}", normalized, overwrite, I18nCatalogUtils.countLeaves(merged))
        return toSummary(saved)
    }

    /** 删除指定 locale 目录 */
    @Transactional
    fun deleteCatalog(locale: String) {
        val normalized = normalizeLocale(locale)
        if (!repository.existsById(normalized)) {
            throw BizException("文案目录不存在：$normalized")
        }
        repository.deleteById(normalized)
        log.info("删除 UI 文案目录 locale={}", normalized)
    }

    /** 从 classpath 种子文件初始化目录 */
    @Transactional
    fun seedFromClasspath(locales: List<String>, force: Boolean): List<I18nCatalogSummaryView> {
        val results = mutableListOf<I18nCatalogSummaryView>()
        for (raw in locales) {
            val locale = normalizeLocale(raw)
            if (!force && repository.existsById(locale)) {
                log.info("跳过文案种子 locale={}（已存在）", locale)
                continue
            }
            val content = loadSeedContent(locale)
                ?: throw BizException("未找到种子文件：i18n-seed/$locale.json")
            val entity = repository.findById(locale).orElse(
                I18nLocaleCatalogEntity(locale = locale),
            )
            entity.content = content
            entity.updatedAt = OffsetDateTime.now()
            results.add(toSummary(repository.save(entity)))
            log.info("种子初始化 UI 文案目录 locale={} keyCount={}", locale, I18nCatalogUtils.countLeaves(content))
        }
        return results
    }

    /** 启动时若库为空则自动种子中英目录 */
    @Transactional
    fun bootstrapIfEmpty() {
        if (repository.count() > 0L) return
        log.info("文案目录为空，开始从 classpath 种子初始化 en-US / zh-CN")
        seedFromClasspath(listOf("en-US", "zh-CN"), force = false)
    }

    private fun toSummary(entity: I18nLocaleCatalogEntity): I18nCatalogSummaryView =
        I18nCatalogSummaryView(
            locale = entity.locale,
            keyCount = I18nCatalogUtils.countLeaves(entity.content),
            updatedAt = entity.updatedAt.toString(),
        )

    private fun normalizeLocale(locale: String): String {
        val trimmed = locale.trim()
        if (trimmed.isEmpty()) throw BizException("locale 不能为空")
        if (!I18nLocaleSupport.STOREFRONT_LOCALES.contains(trimmed) && trimmed != "en-US" && trimmed != "zh-CN") {
            // 后台仅中英，但允许导入其他前台 locale
            if (!trimmed.matches(Regex("[a-z]{2}-[A-Z]{2}"))) {
                throw BizException("不支持的 locale：$trimmed")
            }
        }
        return trimmed
    }

    /** 读取种子 JSON 并合并 legal 子树 */
    private fun loadSeedContent(locale: String): Map<String, Any>? {
        val main = readJsonMap("i18n-seed/$locale.json") ?: return null
        val short = locale.substringBefore('-')
        val legal = readJsonMap("i18n-seed/legal-$short.json")
        return if (legal != null) {
            main.toMutableMap().apply { put("legal", legal) }
        } else {
            main
        }
    }

    private fun readJsonMap(path: String): Map<String, Any>? {
        val resource = ClassPathResource(path)
        if (!resource.exists()) return null
        return resource.inputStream.use { input ->
            objectMapper.readValue(input, object : TypeReference<Map<String, Any>>() {})
        }
    }
}
