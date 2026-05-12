package com.trioForce.voyage.loyalty

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * 用户会员累计表：一行 per 用户，记录累计已支付美金与当前档位。
 * 与订单支付成功（后台将订单置为 PAID）时累加金额并刷新 [tier]。
 */
@Entity
@Table(name = "t_user_membership")
class UserMembershipEntity(
    @Id
    @Column(name = "user_id")
    var userId: Long,

    @Column(name = "lifetime_paid_usd", nullable = false, precision = 14, scale = 2)
    var lifetimePaidUsd: BigDecimal = BigDecimal.ZERO,

    @Column(nullable = false, length = 20)
    var tier: String = "NONE",

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
