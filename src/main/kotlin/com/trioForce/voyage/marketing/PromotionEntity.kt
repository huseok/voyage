package com.trioForce.voyage.marketing

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "t_promotions")
class PromotionEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, length = 200)
    var name: String,

    @Column(name = "threshold_amount", nullable = false, precision = 12, scale = 2)
    var thresholdAmount: BigDecimal,

    @Column(name = "amount_off", nullable = false, precision = 12, scale = 2)
    var amountOff: BigDecimal,

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
