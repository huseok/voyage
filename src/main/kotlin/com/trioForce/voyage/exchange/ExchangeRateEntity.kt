package com.trioForce.voyage.exchange

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

/** 单币种相对 USD 的汇率配置与快照 */
@Entity
@Table(name = "t_exchange_rates")
class ExchangeRateEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "currency_code", nullable = false, unique = true, length = 8)
    var currencyCode: String,

    /** 行情汇率：1 USD = marketRate 目标币 */
    @Column(name = "market_rate", nullable = false, precision = 18, scale = 8)
    var marketRate: BigDecimal,

    /** 叠加加点后的有效汇率（含冻结价） */
    @Column(name = "effective_rate", nullable = false, precision = 18, scale = 8)
    var effectiveRate: BigDecimal,

    /** 在全局加点之上额外叠加的百分比 */
    @Column(name = "markup_percent", nullable = false, precision = 8, scale = 4)
    var markupPercent: BigDecimal = BigDecimal.ZERO,

    /** 在百分比加点后额外叠加的绝对值（目标币单位） */
    @Column(name = "markup_amount", nullable = false, precision = 18, scale = 8)
    var markupAmount: BigDecimal = BigDecimal.ZERO,

    /** 冻结期内使用的固定汇率 */
    @Column(name = "frozen_rate", precision = 18, scale = 8)
    var frozenRate: BigDecimal? = null,

    /** 冻结截止时间；此前不自动刷新行情 */
    @Column(name = "freeze_until")
    var freezeUntil: OffsetDateTime? = null,

    /** 单币种刷新间隔（小时）；null 表示沿用全局 */
    @Column(name = "refresh_interval_hours")
    var refreshIntervalHours: Int? = null,

    @Column(name = "last_fetched_at")
    var lastFetchedAt: OffsetDateTime? = null,

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
