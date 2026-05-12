package com.trioForce.voyage.loyalty

import com.trioForce.voyage.common.BizException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
class MembershipAdminService(
    private val membershipTierRuleRepository: MembershipTierRuleRepository,
) {
    fun listTierRules(): List<MembershipTierRuleAdminView> =
        membershipTierRuleRepository.findAllByOrderBySortNoAsc().map { toView(it) }

    @Transactional
    fun createTierRule(req: MembershipTierRuleUpsertRequest): Long {
        validateUpsert(req, excludeId = null)
        val now = OffsetDateTime.now()
        val code = req.tierCode.trim().uppercase()
        val e = MembershipTierRuleEntity(
            id = null,
            tierCode = code,
            minLifetimePaidUsd = req.minLifetimePaidUsd,
            discountPercent = req.discountPercent,
            sortNo = req.sortNo,
            isActive = req.isActive,
            createdAt = now,
            updatedAt = now,
        )
        return membershipTierRuleRepository.save(e).id!!
    }

    @Transactional
    fun updateTierRule(id: Long, req: MembershipTierRuleUpsertRequest) {
        val e = membershipTierRuleRepository.findById(id).orElseThrow { BizException("tier rule not found") }
        validateUpsert(req, excludeId = e.id!!)
        val code = req.tierCode.trim().uppercase()
        e.tierCode = code
        e.minLifetimePaidUsd = req.minLifetimePaidUsd
        e.discountPercent = req.discountPercent
        e.sortNo = req.sortNo
        e.isActive = req.isActive
        e.updatedAt = OffsetDateTime.now()
    }

    @Transactional
    fun patchTierRuleActive(id: Long, req: MembershipTierRuleActivePatchRequest) {
        val e = membershipTierRuleRepository.findById(id).orElseThrow { BizException("tier rule not found") }
        e.isActive = req.isActive
        e.updatedAt = OffsetDateTime.now()
    }

    private fun validateUpsert(req: MembershipTierRuleUpsertRequest, excludeId: Long?) {
        val code = req.tierCode.trim().uppercase()
        if (code == "NONE") throw BizException("tier code NONE is reserved")
        if (code.isEmpty()) throw BizException("invalid tier code")
        val dup = membershipTierRuleRepository.findByTierCodeIgnoreCase(code)
        if (dup.isPresent) {
            val ex = dup.get()
            if (excludeId == null || ex.id != excludeId) {
                throw BizException("duplicate tier code")
            }
        }
    }

    private fun toView(e: MembershipTierRuleEntity) = MembershipTierRuleAdminView(
        id = e.id!!,
        tierCode = e.tierCode,
        minLifetimePaidUsd = e.minLifetimePaidUsd.stripTrailingZeros().toPlainString(),
        discountPercent = e.discountPercent,
        sortNo = e.sortNo,
        isActive = e.isActive,
        createdAt = e.createdAt,
        updatedAt = e.updatedAt,
    )
}
