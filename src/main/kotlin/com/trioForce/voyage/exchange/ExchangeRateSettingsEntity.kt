package com.trioForce.voyage.exchange

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

/** 汇率全局配置（单行 id=1） */
@Entity
@Table(name = "t_exchange_rate_settings")
class ExchangeRateSettingsEntity(
    @Id
    var id: Long = 1L,

    @Column(name = "base_currency", nullable = false, length = 8)
    var baseCurrency: String = "USD",

    /** 默认自动刷新间隔（小时），例如 12 表示每半天 */
    @Column(name = "refresh_interval_hours", nullable = false)
    var refreshIntervalHours: Int = 12,

    /** 全局加点百分比，叠加到各币种行情上 */
    @Column(name = "default_markup_percent", nullable = false, precision = 8, scale = 4)
    var defaultMarkupPercent: BigDecimal = BigDecimal.ZERO,

    @Column(name = "provider_url", nullable = false, length = 512)
    var providerUrl: String = "https://api.frankfurter.app/latest",

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
