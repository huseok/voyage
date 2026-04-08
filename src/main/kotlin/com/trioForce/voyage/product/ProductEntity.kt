package com.trioForce.voyage.product

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "t_products")
/**
 * 商品实体：
 * 包含外贸常用字段（币种、SKU、HS 编码、贸易条款、单位等）。
 */
class ProductEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(nullable = false, precision = 12, scale = 2)
    var price: BigDecimal,

    @Column(nullable = false, length = 8)
    var currency: String = "USD",

    // MOQ: Minimum Order Quantity，最小起订量
    @Column(nullable = false)
    var moq: Int = 1,

    @Column(columnDefinition = "text")
    var description: String? = null,

    @Column(name = "sku_code", length = 64)
    var skuCode: String? = null,

    @Column(name = "hs_code", length = 32)
    var hsCode: String? = null,

    @Column(length = 20)
    var unit: String? = null,

    @Column(length = 20)
    var incoterm: String? = null,

    @Column(name = "origin_country", length = 60)
    var originCountry: String? = null,

    @Column(name = "lead_time_days")
    var leadTimeDays: Int? = null,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
