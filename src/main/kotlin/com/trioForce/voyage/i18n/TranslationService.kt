package com.trioForce.voyage.i18n

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Service
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

interface MachineTranslationProvider {
    fun translate(texts: List<String>, sourceLocale: String, targetLocale: String, config: EffectiveTranslationConfig): List<String>
}

/** API Key 未配置时拒绝翻译，避免静默返回空文案 */
@Service
class StubTranslationProvider : MachineTranslationProvider {
    override fun translate(
        texts: List<String>,
        sourceLocale: String,
        targetLocale: String,
        config: EffectiveTranslationConfig,
    ): List<String> {
        throw BizException("翻译服务未配置，请在后台「翻译配置」填写 DeepL API Key 或设置环境变量 VOYAGE_I18N_API_KEY")
    }
}

@Service
class DeepLTranslationProvider(
    private val objectMapper: ObjectMapper,
) : MachineTranslationProvider {
    private val log = LogUtil.logger<DeepLTranslationProvider>()
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(15)).build()

    override fun translate(
        texts: List<String>,
        sourceLocale: String,
        targetLocale: String,
        config: EffectiveTranslationConfig,
    ): List<String> {
        val key = config.apiKey.trim()
        if (key.isEmpty()) {
            log.warn("DeepL API Key 未配置，无法翻译")
            throw BizException("翻译服务未配置，请在后台「翻译配置」填写 DeepL API Key 或设置环境变量 VOYAGE_I18N_API_KEY")
        }
        if (texts.isEmpty()) return emptyList()
        val base = config.apiUrl.trimEnd('/')
        val body = buildString {
            append("auth_key=").append(URLEncoder.encode(key, StandardCharsets.UTF_8))
            append("&source_lang=").append(I18nLocaleSupport.toDeepLSource(sourceLocale))
            append("&target_lang=").append(I18nLocaleSupport.toDeepLTarget(targetLocale))
            for (text in texts) {
                append("&text=").append(URLEncoder.encode(text, StandardCharsets.UTF_8))
            }
        }
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$base/v2/translate"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .timeout(Duration.ofSeconds(60))
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build()
        val response = try {
            http.send(request, HttpResponse.BodyHandlers.ofString())
        } catch (ex: Exception) {
            log.error("调用 DeepL 翻译接口失败", ex)
            throw BizException("翻译服务暂时不可用，请稍后重试")
        }
        if (response.statusCode() !in 200..299) {
            log.warn("DeepL 返回非成功状态码 status={} body={}", response.statusCode(), response.body().take(200))
            throw BizException("翻译服务返回错误（HTTP ${response.statusCode()}）")
        }
        val root: JsonNode = objectMapper.readTree(response.body())
        val translations = root.get("translations") ?: throw BizException("翻译响应格式异常")
        if (!translations.isArray || translations.size() != texts.size) {
            throw BizException("翻译结果条数与请求不一致")
        }
        return translations.map { it.get("text")?.asText().orEmpty() }
    }
}

@Service
@EnableConfigurationProperties(TranslationProperties::class)
class TranslationService(
    private val configService: I18nTranslationConfigService,
    private val deepL: DeepLTranslationProvider,
    private val stub: StubTranslationProvider,
) {
    private val log = LogUtil.logger<TranslationService>()

    private fun provider(name: String): MachineTranslationProvider =
        when (name.trim().lowercase()) {
            "deepl" -> deepL
            "stub" -> stub
            else -> deepL
        }

    /**
     * 将多条文本翻译到多个目标语言。
     * 返回：targetLocale → 与 texts 等长的译文列表。
     */
    fun translateBatch(
        texts: List<String>,
        sourceLocale: String,
        targetLocales: List<String>,
    ): Map<String, List<String>> {
        val config = configService.resolveEffective()
        if (!config.enabled) {
            throw BizException("翻译功能已在后台关闭，请在「翻译配置」中启用")
        }
        if (texts.isEmpty()) return emptyMap()
        if (texts.size > config.maxTextsPerRequest) {
            throw BizException("单次最多翻译 ${config.maxTextsPerRequest} 条文本")
        }
        val src = sourceLocale.ifBlank { config.defaultSourceLocale }
        val result = linkedMapOf<String, List<String>>()
        val mt = provider(config.provider)
        for (target in targetLocales.distinct()) {
            if (target == src) {
                result[target] = texts
                continue
            }
            if (!I18nLocaleSupport.STOREFRONT_LOCALES.contains(target)) {
                log.warn("跳过不支持的目标语言 locale={}", target)
                continue
            }
            log.info("开始批量翻译 source={} target={} count={}", src, target, texts.size)
            result[target] = mt.translate(texts, src, target, config)
        }
        return result
    }

    /** 管理端连通性测试：翻译单条短文本 */
    fun testTranslation(text: String, sourceLocale: String, targetLocale: String): String {
        val map = translateBatch(listOf(text), sourceLocale, listOf(targetLocale))
        return map[targetLocale]?.firstOrNull()?.takeIf { it.isNotBlank() }
            ?: throw BizException("翻译测试未返回结果")
    }
}
