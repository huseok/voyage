package com.trioForce.voyage.marketing

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CouponRepository : JpaRepository<CouponEntity, Long> {
    fun findByCodeIgnoreCaseAndIsActiveIsTrue(code: String): Optional<CouponEntity>
    fun findByCodeIgnoreCase(code: String): Optional<CouponEntity>
}
