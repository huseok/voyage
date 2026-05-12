package com.trioForce.voyage.loyalty

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime

/**
 * 会员等级与折扣：
 * - 累计维度：已支付订单金额（美金）；非 USD 币种当前版本按 1:1 数值累加（生产应接入汇率服务）。
 * - 档位与折扣：默认读取 [t_membership_tier_rules]；若库中无启用规则则回退内置阈值（与历史种子一致）。
 */
@Service
class MembershipService(
    private val membershipRepository: UserMembershipRepository,
    private val membershipTierRuleRepository: MembershipTierRuleRepository,
) {
    fun getOrCreate(userId: Long): UserMembershipEntity {
        val existing = membershipRepository.findById(userId)
        if (existing.isPresent) return existing.get()
        return membershipRepository.save(
            UserMembershipEntity(
                userId = userId,
                lifetimePaidUsd = BigDecimal.ZERO,
                tier = "NONE",
                updatedAt = OffsetDateTime.now(),
            ),
        )
    }

    /** 根据累计已支付美金计算档位编码（NONE 表示低于所有启用门槛）。 */
    fun resolveTier(lifetimeUsd: BigDecimal): String {
        val v = lifetimeUsd.setScale(2, RoundingMode.HALF_UP)
        val rules = membershipTierRuleRepository.findAllByIsActiveIsTrueOrderByMinLifetimePaidUsdDesc()
        if (rules.isNotEmpty()) {
            for (r in rules) {
                if (v >= r.minLifetimePaidUsd.setScale(2, RoundingMode.HALF_UP)) {
                    return r.tierCode.uppercase()
                }
            }
            return "NONE"
        }
        return legacyResolveTier(v)
    }

    /** 返回 0～100 的整数百分比（例如 2 表示 2%）。 */
    fun discountPercent(tier: String): Int {
        val t = tier.trim().uppercase()
        if (t == "NONE") return 0
        val row = membershipTierRuleRepository.findByTierCodeIgnoreCaseAndIsActiveIsTrue(t)
        if (row.isPresent) return row.get().discountPercent.coerceIn(0, 100)
        val any = membershipTierRuleRepository.findByTierCodeIgnoreCase(t)
        if (any.isPresent) return any.get().discountPercent.coerceIn(0, 100)
        return legacyDiscountPercent(t)
    }

    /** 对商品小计计算会员立减金额（向下取分）。 */
    fun memberDiscountAmount(tier: String, subtotal: BigDecimal): BigDecimal {
        val p = discountPercent(tier)
        if (p <= 0) return BigDecimal.ZERO
        return subtotal.multiply(BigDecimal.valueOf(p.toLong())).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
    }

    /**
     * 订单进入已支付后累加用户终身消费并刷新档位。
     * @param paidAmount 本单计入累计的金额（与订单币种数值一致；未做汇率换算）。
     */
    @Transactional
    fun accumulateAfterOrderPaid(userId: Long, paidAmount: BigDecimal) {
        val row = getOrCreate(userId)
        row.lifetimePaidUsd = row.lifetimePaidUsd.add(paidAmount).setScale(2, RoundingMode.HALF_UP)
        row.tier = resolveTier(row.lifetimePaidUsd)
        row.updatedAt = OffsetDateTime.now()
        membershipRepository.save(row)
    }

    private fun legacyResolveTier(v: BigDecimal): String =
        when {
            v >= BigDecimal("50000") -> "PLATINUM"
            v >= BigDecimal("10000") -> "GOLD"
            v >= BigDecimal("5000") -> "SILVER"
            v >= BigDecimal("1000") -> "BRONZE"
            else -> "NONE"
        }

    private fun legacyDiscountPercent(tier: String): Int =
        when (tier) {
            "BRONZE" -> 2
            "SILVER" -> 4
            "GOLD" -> 5
            "PLATINUM" -> 8
            else -> 0
        }
}
