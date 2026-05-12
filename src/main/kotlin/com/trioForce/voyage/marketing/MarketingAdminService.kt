package com.trioForce.voyage.marketing

import com.trioForce.voyage.common.BizException
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
class MarketingAdminService(
    private val couponRepository: CouponRepository,
    private val promotionRepository: PromotionRepository,
) {
    fun listCoupons(): List<CouponAdminView> =
        couponRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).map { toCouponView(it) }

    fun listPromotions(): List<PromotionAdminView> =
        promotionRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).map { toPromotionView(it) }

    @Transactional
    fun createCoupon(req: CouponAdminUpsertRequest): Long {
        validateCouponUpsert(req)
        val codeNorm = req.code.trim().uppercase()
        if (couponRepository.findByCodeIgnoreCase(codeNorm).isPresent) {
            throw BizException("duplicate coupon code")
        }
        val now = OffsetDateTime.now()
        val e = CouponEntity(
            id = null,
            code = codeNorm,
            name = req.name.trim(),
            discountType = req.discountType.trim().uppercase(),
            discountValue = req.discountValue,
            minOrderAmount = req.minOrderAmount,
            tagFilter = normalizeTagFilter(req.tagFilter),
            validFrom = req.validFrom,
            validTo = req.validTo,
            isActive = req.isActive,
            createdAt = now,
            updatedAt = now,
        )
        return couponRepository.save(e).id!!
    }

    @Transactional
    fun updateCoupon(id: Long, req: CouponAdminUpsertRequest) {
        validateCouponUpsert(req)
        val e = couponRepository.findById(id).orElseThrow { BizException("coupon not found") }
        val codeNorm = req.code.trim().uppercase()
        val other = couponRepository.findByCodeIgnoreCase(codeNorm)
        if (other.isPresent && other.get().id != e.id) {
            throw BizException("duplicate coupon code")
        }
        e.code = codeNorm
        e.name = req.name.trim()
        e.discountType = req.discountType.trim().uppercase()
        e.discountValue = req.discountValue
        e.minOrderAmount = req.minOrderAmount
        e.tagFilter = normalizeTagFilter(req.tagFilter)
        e.validFrom = req.validFrom
        e.validTo = req.validTo
        e.isActive = req.isActive
        e.updatedAt = OffsetDateTime.now()
    }

    @Transactional
    fun patchCouponActive(id: Long, req: CouponActivePatchRequest) {
        val e = couponRepository.findById(id).orElseThrow { BizException("coupon not found") }
        e.isActive = req.isActive
        e.updatedAt = OffsetDateTime.now()
    }

    @Transactional
    fun createPromotion(req: PromotionAdminUpsertRequest): Long {
        validatePromotionUpsert(req)
        val now = OffsetDateTime.now()
        val e = PromotionEntity(
            id = null,
            name = req.name.trim(),
            thresholdAmount = req.thresholdAmount,
            amountOff = req.amountOff,
            tagFilter = normalizeTagFilter(req.tagFilter),
            validFrom = req.validFrom,
            validTo = req.validTo,
            isActive = req.isActive,
            createdAt = now,
            updatedAt = now,
        )
        return promotionRepository.save(e).id!!
    }

    @Transactional
    fun updatePromotion(id: Long, req: PromotionAdminUpsertRequest) {
        validatePromotionUpsert(req)
        val e = promotionRepository.findById(id).orElseThrow { BizException("promotion not found") }
        e.name = req.name.trim()
        e.thresholdAmount = req.thresholdAmount
        e.amountOff = req.amountOff
        e.tagFilter = normalizeTagFilter(req.tagFilter)
        e.validFrom = req.validFrom
        e.validTo = req.validTo
        e.isActive = req.isActive
        e.updatedAt = OffsetDateTime.now()
    }

    @Transactional
    fun patchPromotionActive(id: Long, req: PromotionActivePatchRequest) {
        val e = promotionRepository.findById(id).orElseThrow { BizException("promotion not found") }
        e.isActive = req.isActive
        e.updatedAt = OffsetDateTime.now()
    }

    private fun validateCouponUpsert(req: CouponAdminUpsertRequest) {
        if (!req.validFrom.isBefore(req.validTo)) throw BizException("invalid coupon validity range")
        val dt = req.discountType.trim().uppercase()
        if (dt != "PERCENT" && dt != "FIXED") throw BizException("invalid discount type")
        when (dt) {
            "PERCENT" -> {
                if (req.discountValue > BigDecimal(100) || req.discountValue <= BigDecimal.ZERO) {
                    throw BizException("percent discount must be between 0 and 100")
                }
            }
            "FIXED" -> {
                if (req.discountValue <= BigDecimal.ZERO) throw BizException("fixed discount must be positive")
            }
        }
    }

    private fun validatePromotionUpsert(req: PromotionAdminUpsertRequest) {
        if (!req.validFrom.isBefore(req.validTo)) throw BizException("invalid promotion validity range")
        if (req.amountOff > req.thresholdAmount) {
            throw BizException("amount off cannot exceed threshold")
        }
    }

    private fun normalizeTagFilter(raw: String?): String? {
        val t = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return t
    }

    private fun toCouponView(e: CouponEntity) = CouponAdminView(
        id = e.id!!,
        code = e.code,
        name = e.name,
        discountType = e.discountType,
        discountValue = e.discountValue.stripTrailingZeros().toPlainString(),
        minOrderAmount = e.minOrderAmount.stripTrailingZeros().toPlainString(),
        tagFilter = e.tagFilter,
        validFrom = e.validFrom,
        validTo = e.validTo,
        isActive = e.isActive,
    )

    private fun toPromotionView(e: PromotionEntity) = PromotionAdminView(
        id = e.id!!,
        name = e.name,
        thresholdAmount = e.thresholdAmount.stripTrailingZeros().toPlainString(),
        amountOff = e.amountOff.stripTrailingZeros().toPlainString(),
        tagFilter = e.tagFilter,
        validFrom = e.validFrom,
        validTo = e.validTo,
        isActive = e.isActive,
    )
}
