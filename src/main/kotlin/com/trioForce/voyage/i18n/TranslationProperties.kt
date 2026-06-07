package com.trioForce.voyage.i18n

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "voyage.i18n.translation")
data class TranslationProperties(
    /** deepl | google | stub */
    var provider: String = "deepl",
    var apiKey: String = "",
    /** DeepL API 根地址，默认官方免费版 */
    var apiUrl: String = "https://api-free.deepl.com",
    var defaultSourceLocale: String = "en-US",
    /** 单次请求最大条数 */
    var maxTextsPerRequest: Int = 50,
)
