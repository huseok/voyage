package com.trioForce.voyage.loyalty

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface MembershipTierRuleRepository : JpaRepository<MembershipTierRuleEntity, Long> {
    fun findAllByOrderBySortNoAsc(): List<MembershipTierRuleEntity>

    fun findAllByIsActiveIsTrueOrderByMinLifetimePaidUsdDesc(): List<MembershipTierRuleEntity>

    fun findByTierCodeIgnoreCase(tierCode: String): Optional<MembershipTierRuleEntity>

    fun findByTierCodeIgnoreCaseAndIsActiveIsTrue(tierCode: String): Optional<MembershipTierRuleEntity>
}
