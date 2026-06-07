package com.trioForce.voyage.i18n

/**
 * 系统支持的 BCP 47 locale 与翻译服务目标语言映射。
 */
object I18nLocaleSupport {
    val STOREFRONT_LOCALES: List<String> = listOf(
        "en-US", "zh-CN", "es-ES", "ru-RU", "fr-FR", "de-DE", "pt-BR",
        "ja-JP", "ko-KR", "ar-SA", "it-IT", "nl-NL", "tr-TR", "pl-PL",
        "vi-VN", "th-TH", "id-ID", "hi-IN",
    )

    private val DEEPL_TARGET: Map<String, String> = mapOf(
        "en-US" to "EN-US",
        "zh-CN" to "ZH",
        "es-ES" to "ES",
        "ru-RU" to "RU",
        "fr-FR" to "FR",
        "de-DE" to "DE",
        "pt-BR" to "PT-BR",
        "ja-JP" to "JA",
        "ko-KR" to "KO",
        "ar-SA" to "AR",
        "it-IT" to "IT",
        "nl-NL" to "NL",
        "tr-TR" to "TR",
        "pl-PL" to "PL",
        "vi-VN" to "VI",
        "id-ID" to "ID",
        "hi-IN" to "HI",
    )

    private val DEEPL_SOURCE: Map<String, String> = mapOf(
        "en-US" to "EN",
        "zh-CN" to "ZH",
    )

    /** DeepL 目标语言代码；th-TH 等无映射时回退 EN */
    fun toDeepLTarget(locale: String): String = DEEPL_TARGET[locale] ?: "EN-US"

    /** DeepL 源语言代码 */
    fun toDeepLSource(locale: String): String = DEEPL_SOURCE[locale] ?: "EN"

    /** 从 i18n 映射解析展示名，回退链：locale → en-US → zh-CN → 任意非空 */
    fun resolveText(i18n: Map<String, String>, locale: String): String {
        val direct = i18n[locale]?.trim().orEmpty()
        if (direct.isNotEmpty()) return direct
        val en = i18n["en-US"]?.trim().orEmpty()
        if (en.isNotEmpty()) return en
        val zh = i18n["zh-CN"]?.trim().orEmpty()
        if (zh.isNotEmpty()) return zh
        return i18n.values.firstOrNull { it.isNotBlank() }?.trim().orEmpty()
    }

    /** 从 nameZh/nameEn 构建 i18n 映射（兼容旧列） */
    fun fromBilingual(nameZh: String, nameEn: String): Map<String, String> {
        val map = linkedMapOf<String, String>()
        val zh = nameZh.trim()
        val en = nameEn.trim()
        if (en.isNotEmpty()) map["en-US"] = en
        if (zh.isNotEmpty()) map["zh-CN"] = zh
        return map
    }

    /** 同步 nameZh/nameEn 列与 i18n JSON */
    fun syncBilingualColumns(i18n: Map<String, String>): Pair<String, String> {
        val zh = i18n["zh-CN"]?.trim().orEmpty().ifEmpty { i18n.values.firstOrNull().orEmpty() }
        val en = i18n["en-US"]?.trim().orEmpty().ifEmpty { zh }
        return zh to en
    }

    /** 商品 i18n：locale → { title, description } */
    fun resolveProductFields(
        i18n: Map<String, Map<String, String>>,
        locale: String,
    ): Pair<String, String?> {
        fun pick(loc: String): Map<String, String>? = i18n[loc]
        val chain = listOf(locale, "en-US", "zh-CN").distinct()
        for (loc in chain) {
            val bucket = pick(loc) ?: continue
            val title = bucket["title"]?.trim().orEmpty()
            if (title.isNotEmpty()) {
                val desc = bucket["description"]?.trim()?.ifEmpty { null }
                return title to desc
            }
        }
        for (bucket in i18n.values) {
            val title = bucket["title"]?.trim().orEmpty()
            if (title.isNotEmpty()) {
                return title to bucket["description"]?.trim()?.ifEmpty { null }
            }
        }
        return "" to null
    }
}
