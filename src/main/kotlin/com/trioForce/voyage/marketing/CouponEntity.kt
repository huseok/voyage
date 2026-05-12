package com.trioForce.voyage.marketing

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "t_coupons")
class CouponEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 64)
    var code: String,

    @Column(nullable = false, length = 200)
    var name: String,

    /** `PERCENT` 按百分比；`FIXED` 为固定金额立减。 */
    @Column(name = "discount_type", nullable = false, length = 16)
    var discountType: String,

    @Column(name = "discount_value", nullable = false, precision = 12, scale = 2)
    var discountValue: BigDecimal,

    @Column(name = "min_order_amount", nullable = false, precision = 12, scale = 2)
    var minOrderAmount: BigDecimal = BigDecimal.ZERO,

    /** 逗号分隔的标签 **编码**（与 [com.trioForce.voyage.tag.TagEntity.code] 一致）；为空表示全站可用。 */
    @Column(name = "tag_filter", length = 400)
    var tagFilter: String? = null,

    @Column(name = "valid_from", nullable = false)
    var validFrom: OffsetDateTime,

    @Column(name = "valid_to", nullable = false)
    var validTo: OffsetDateTime,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
