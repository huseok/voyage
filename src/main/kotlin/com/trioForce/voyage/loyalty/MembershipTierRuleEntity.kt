package com.trioForce.voyage.loyalty

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "t_membership_tier_rules")
class MembershipTierRuleEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "tier_code", nullable = false, unique = true, length = 32)
    var tierCode: String,

    @Column(name = "min_lifetime_paid_usd", nullable = false, precision = 14, scale = 2)
    var minLifetimePaidUsd: BigDecimal,

    @Column(name = "discount_percent", nullable = false)
    var discountPercent: Int,

    @Column(name = "sort_no", nullable = false)
    var sortNo: Int = 0,

    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
