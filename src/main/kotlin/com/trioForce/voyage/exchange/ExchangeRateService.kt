package com.trioForce.voyage.exchange

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.client.RestClient
import java.math.BigDecimal
import java.math.RoundingMode
import java.net.URI
import java.time.Duration
import java.time.OffsetDateTime
import java.time.format.DateTimeParseException

@Service
class ExchangeRateService(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val settingsRepository: ExchangeRateSettingsRepository,
) {
    private val log = LogUtil.logger<ExchangeRateService>()
    private val objectMapper = ObjectMapper()
    /** Frankfurter 等对无 Accept/User-Agent 的 Java 客户端可能返回 HTML，需显式声明 JSON */
    private val restClient = RestClient.builder()
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .defaultHeader(HttpHeaders.USER_AGENT, "globuy-voyage-exchange/1.0")
        .build()

    /** 获取或初始化全局配置 */
    @Transactional(readOnly = true)
    fun getSettings(): ExchangeRateSettingsEntity =
        settingsRepository.findById(1L).orElseGet {
            ExchangeRateSettingsEntity()
        }

    @Transactional(readOnly = true)
    fun listAdmin(): List<ExchangeRateView> {
        val settings = getSettings()
        return exchangeRateRepository.findAll().sortedBy { it.currencyCode }.map { toAdminView(it, settings) }
    }

    @Transactional(readOnly = true)
    fun getPublicRates(): ExchangeRatePublicView {
        val settings = getSettings()
        val items = exchangeRateRepository.findAllByEnabledIsTrueOrderByCurrencyCodeAsc().map {
            ExchangeRatePublicItem(
                currencyCode = it.currencyCode,
                effectiveRate = it.effectiveRate.toPlainString(),
                lastFetchedAt = it.lastFetchedAt?.toString(),
            )
        }
        return ExchangeRatePublicView(
            baseCurrency = settings.baseCurrency,
            refreshIntervalHours = settings.refreshIntervalHours,
            rates = items,
        )
    }

    @Transactional
    fun updateSettings(req: ExchangeRateSettingsUpdateRequest): ExchangeRateSettingsView {
        val entity = settingsRepository.findById(1L).orElse(ExchangeRateSettingsEntity())
        entity.refreshIntervalHours = req.refreshIntervalHours
        entity.defaultMarkupPercent = req.defaultMarkupPercent
        entity.providerUrl = req.providerUrl.trim()
        entity.updatedAt = OffsetDateTime.now()
        settingsRepository.save(entity)
        recomputeAllEffectiveRates(entity)
        log.info("汇率全局配置已更新，刷新间隔 {} 小时，全局加点 {}%", req.refreshIntervalHours, req.defaultMarkupPercent)
        return toSettingsView(entity)
    }

    @Transactional
    fun updateCurrency(code: String, req: ExchangeRateAdminUpdateRequest): ExchangeRateView {
        val normalized = code.trim().uppercase()
        val entity = exchangeRateRepository.findByCurrencyCodeIgnoreCase(normalized)
            ?: throw BizException("币种不存在: $normalized")
        val settings = getSettings()
        req.markupPercent?.let { entity.markupPercent = it }
        req.markupAmount?.let { entity.markupAmount = it }
        req.refreshIntervalHours?.let { entity.refreshIntervalHours = it }
        req.enabled?.let { entity.enabled = it }
        if (req.clearFreeze == true) {
            entity.frozenRate = null
            entity.freezeUntil = null
        } else {
            req.frozenRate?.let { entity.frozenRate = it }
            req.freezeUntil?.let {
                entity.freezeUntil = try {
                    OffsetDateTime.parse(it)
                } catch (e: DateTimeParseException) {
                    throw BizException("freezeUntil 格式无效")
                }
            }
        }
        entity.effectiveRate = computeEffectiveRate(entity, settings)
        entity.updatedAt = OffsetDateTime.now()
        exchangeRateRepository.save(entity)
        log.info("币种 {} 汇率配置已更新，有效汇率 {}", normalized, entity.effectiveRate.toPlainString())
        return toAdminView(entity, settings)
    }

    /** 手动触发全量行情刷新 */
    @Transactional
    fun refreshMarketRates(force: Boolean = false): Int {
        val settings = getSettings()
        val now = OffsetDateTime.now()
        val entities = exchangeRateRepository.findAll().filter { it.enabled && it.currencyCode != settings.baseCurrency }
        val due = entities.filter { shouldRefresh(it, settings, now, force) }
        if (due.isEmpty()) {
            log.debug("暂无到期需刷新的汇率币种")
            return 0
        }
        val market = fetchMarketRates(settings, due.map { it.currencyCode })
        var updated = 0
        for (entity in due) {
            val rate = market[entity.currencyCode] ?: continue
            entity.marketRate = rate
            entity.lastFetchedAt = now
            entity.effectiveRate = computeEffectiveRate(entity, settings)
            entity.updatedAt = now
            exchangeRateRepository.save(entity)
            updated++
        }
        if (updated == 0) {
            val missing = due.map { it.currencyCode }.filter { !market.containsKey(it) }
            log.warn("汇率刷新未更新任何币种，行情缺失: {}", missing.joinToString())
            throw BizException("未能从行情源获取汇率，部分币种可能不受支持（如 RUB/SAR/VND），请稍后重试或手动设置冻结汇率")
        }
        log.info("汇率行情刷新完成，更新 {} 个币种", updated)
        return updated
    }

    /** 定时任务入口：按全局间隔检查并刷新 */
    @Transactional
    fun scheduledRefresh() {
        val count = refreshMarketRates(force = false)
        if (count > 0) {
            log.info("定时汇率刷新：已更新 {} 个币种", count)
        }
    }

    private fun shouldRefresh(
        entity: ExchangeRateEntity,
        settings: ExchangeRateSettingsEntity,
        now: OffsetDateTime,
        force: Boolean,
    ): Boolean {
        if (force) return true
        if (entity.freezeUntil != null && entity.freezeUntil!!.isAfter(now)) {
            return false
        }
        val hours = entity.refreshIntervalHours ?: settings.refreshIntervalHours
        val last = entity.lastFetchedAt ?: return true
        return Duration.between(last, now).toHours() >= hours
    }

    private fun fetchMarketRates(
        settings: ExchangeRateSettingsEntity,
        codes: List<String>,
    ): Map<String, BigDecimal> {
        val base = settings.baseCurrency.uppercase()
        val targets = codes.map { it.uppercase() }.filter { it != base }.distinct()
        if (targets.isEmpty()) return emptyMap()
        val merged = mutableMapOf<String, BigDecimal>()
        var failedBatches = 0
        // 分批请求，避免 URL 过长；单批失败不阻断其它批次
        for (batch in targets.chunked(8)) {
            val url = buildProviderUrl(settings.providerUrl, base, batch)
            try {
                val raw = restClient.get()
                    .uri(URI.create(url))
                    .retrieve()
                    .body(String::class.java)
                if (raw.isNullOrBlank() || !raw.trimStart().startsWith("{")) {
                    log.warn("汇率接口返回非 JSON，url={} snippet={}", url, raw?.take(120))
                    failedBatches++
                    continue
                }
                val body = objectMapper.readTree(raw)
                merged.putAll(parseFrankfurterResponse(body))
            } catch (e: Exception) {
                log.warn("汇率接口批次失败 url={} err={}", url, e.message)
                failedBatches++
            }
        }
        if (merged.isEmpty()) {
            log.error("拉取汇率行情全部失败，批次数={}", targets.chunked(8).size)
            throw BizException("拉取汇率行情失败，请检查 provider_url 或服务器外网访问")
        }
        if (failedBatches > 0) {
            log.warn("部分汇率批次拉取失败，已成功 {} 个币种", merged.size)
        }
        return merged
    }

    /** 构造 Frankfurter 风格 URL，去掉已有 query 避免重复 from/to 参数 */
    private fun buildProviderUrl(providerUrl: String, base: String, targets: List<String>): String {
        val trimmed = providerUrl.trim().trimEnd('/').substringBefore('?')
        val endpoint = when {
            trimmed.endsWith("/latest") -> trimmed
            trimmed.contains("frankfurter.app") -> "$trimmed/latest"
            else -> "$trimmed/latest"
        }
        val joined = targets.joinToString(",")
        return "$endpoint?from=$base&to=$joined"
    }

    private fun parseFrankfurterResponse(body: JsonNode): Map<String, BigDecimal> {
        val rates = body.get("rates") ?: return emptyMap()
        val out = mutableMapOf<String, BigDecimal>()
        rates.fields().forEach { (code, node) ->
            if (node.isNumber) {
                out[code.uppercase()] = node.decimalValue().setScale(8, RoundingMode.HALF_UP)
            }
        }
        return out
    }

    private fun recomputeAllEffectiveRates(settings: ExchangeRateSettingsEntity) {
        val now = OffsetDateTime.now()
        exchangeRateRepository.findAll().forEach { entity ->
            entity.effectiveRate = computeEffectiveRate(entity, settings)
            entity.updatedAt = now
            exchangeRateRepository.save(entity)
        }
    }

    private fun computeEffectiveRate(
        entity: ExchangeRateEntity,
        settings: ExchangeRateSettingsEntity,
    ): BigDecimal {
        val now = OffsetDateTime.now()
        if (entity.freezeUntil != null && entity.freezeUntil!!.isAfter(now) && entity.frozenRate != null) {
            return entity.frozenRate!!.setScale(8, RoundingMode.HALF_UP)
        }
        if (entity.currencyCode.equals(settings.baseCurrency, ignoreCase = true)) {
            return BigDecimal.ONE.setScale(8, RoundingMode.HALF_UP)
        }
        val totalMarkup = settings.defaultMarkupPercent.add(entity.markupPercent)
        val multiplier = BigDecimal.ONE.add(
            totalMarkup.divide(BigDecimal(100), 8, RoundingMode.HALF_UP),
        )
        return entity.marketRate
            .multiply(multiplier)
            .add(entity.markupAmount)
            .setScale(8, RoundingMode.HALF_UP)
    }

    private fun toAdminView(entity: ExchangeRateEntity, settings: ExchangeRateSettingsEntity): ExchangeRateView {
        val effective = computeEffectiveRate(entity, settings)
        return ExchangeRateView(
            currencyCode = entity.currencyCode,
            marketRate = entity.marketRate.toPlainString(),
            effectiveRate = effective.toPlainString(),
            markupPercent = entity.markupPercent.toPlainString(),
            markupAmount = entity.markupAmount.toPlainString(),
            frozenRate = entity.frozenRate?.toPlainString(),
            freezeUntil = entity.freezeUntil?.toString(),
            refreshIntervalHours = entity.refreshIntervalHours,
            lastFetchedAt = entity.lastFetchedAt?.toString(),
            enabled = entity.enabled,
        )
    }

    private fun toSettingsView(entity: ExchangeRateSettingsEntity) = ExchangeRateSettingsView(
        baseCurrency = entity.baseCurrency,
        refreshIntervalHours = entity.refreshIntervalHours,
        defaultMarkupPercent = entity.defaultMarkupPercent.toPlainString(),
        providerUrl = entity.providerUrl,
        updatedAt = entity.updatedAt.toString(),
    )
}
