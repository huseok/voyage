package com.trioForce.voyage.i18n

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.time.OffsetDateTime

/** 单 locale 的 UI 文案目录（整份嵌套 JSON） */
@Entity
@Table(name = "t_i18n_locale_catalog")
class I18nLocaleCatalogEntity(
    @Id
    @Column(name = "locale", nullable = false, length = 16)
    var locale: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", columnDefinition = "jsonb", nullable = false)
    var content: Map<String, Any> = emptyMap(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
