package com.trioForce.voyage.i18n

import jakarta.persistence.*
import java.time.OffsetDateTime

/** 机器翻译全局配置（单行 id=1） */
@Entity
@Table(name = "t_i18n_translation_settings")
class I18nTranslationSettingsEntity(
    @Id
    var id: Long = 1L,

    @Column(name = "provider", nullable = false, length = 32)
    var provider: String = "deepl",

    @Column(name = "api_key", length = 512)
    var apiKey: String? = null,

    @Column(name = "api_url", nullable = false, length = 512)
    var apiUrl: String = "https://api-free.deepl.com",

    @Column(name = "default_source_locale", nullable = false, length = 16)
    var defaultSourceLocale: String = "en-US",

    @Column(name = "max_texts_per_request", nullable = false)
    var maxTextsPerRequest: Int = 50,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
