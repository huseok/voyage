package com.trioForce.voyage.marketing

import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.tag.ProductTagRepository
import com.trioForce.voyage.tag.TagRepository
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.OffsetDateTime

/**
 * 优惠券与满减活动的统一试算：
 * - 标签过滤字段为逗号分隔的 **标签编码**（大写比较）；为空则不对商品范围做限制。
 * - 与会员折扣的叠加顺序由 [com.trioForce.voyage.order.OrderService] 控制：**先会员、再满减、再券**。
 */
@Service
class MarketingService(
    private val couponRepository: CouponRepository,
    private val promotionRepository: PromotionRepository,
    private val productTagRepository: ProductTagRepository,
    private val tagRepository: TagRepository,
) {
    private fun productTagCodes(productIds: Collection<Long>): Set<String> {
        if (productIds.isEmpty()) return emptySet()
        val links = productTagRepository.findAllByProductIdIn(productIds)
        if (links.isEmpty()) return emptySet()
        val tagIds = links.map { it.tagId }.toSet()
        return tagRepository.findAllById(tagIds).map { it.code.uppercase() }.toSet()
    }

    private fun matchesTagFilter(filter: String?, productCodes: Set<String>): Boolean {
        if (filter.isNullOrBlank()) return true
        val req = filter.split(',').map { it.trim().uppercase() }.filter { it.isNotBlank() }.toSet()
        return req.any { it in productCodes }
    }

    /**
     * @param goodsSubtotalForMin 用于校验「最低消费」的**商品折前小计**（一般为选中行原价合计）。
     * @param applyBase 实际立减计算基数（一般为「小计 − 会员 − 满减」之后的金额）。
     * @return Pair(立减金额, 券码快照)；无券码时返回 (0, null)。
     */
    fun computeCouponOff(
        code: String?,
        goodsSubtotalForMin: BigDecimal,
        applyBase: BigDecimal,
        productIds: Collection<Long>,
    ): Pair<BigDecimal, String?> {
        if (code.isNullOrBlank()) return Pair(BigDecimal.ZERO, null)
        val c = couponRepository.findByCodeIgnoreCaseAndIsActiveIsTrue(code.trim())
            .orElseThrow { BizException("invalid coupon") }
        val now = OffsetDateTime.now()
        if (now.isBefore(c.validFrom) || now.isAfter(c.validTo)) throw BizException("coupon expired")
        if (goodsSubtotalForMin < c.minOrderAmount) throw BizException("below coupon minimum")
        val pc = productTagCodes(productIds)
        if (!matchesTagFilter(c.tagFilter, pc)) throw BizException("coupon not applicable")
        val base = applyBase.max(BigDecimal.ZERO)
        val off = when (c.discountType.uppercase()) {
            "PERCENT" -> base.multiply(c.discountValue).divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
            "FIXED" -> c.discountValue.min(base)
            else -> BigDecimal.ZERO
        }
        return Pair(off, c.code.uppercase())
    }

    /** 在满足门槛的活动里取 **最大** 立减额（且受标签过滤约束）。 */
    fun computeBestPromotionOff(subtotal: BigDecimal, productIds: Collection<Long>): BigDecimal {
        val now = OffsetDateTime.now()
        val pc = productTagCodes(productIds)
        var best = BigDecimal.ZERO
        for (p in promotionRepository.findAllByIsActiveIsTrueOrderByIdAsc()) {
            if (now.isBefore(p.validFrom) || now.isAfter(p.validTo)) continue
            if (subtotal < p.thresholdAmount) continue
            if (!matchesTagFilter(p.tagFilter, pc)) continue
            if (p.amountOff > best) best = p.amountOff
        }
        return best.min(subtotal)
    }
}
