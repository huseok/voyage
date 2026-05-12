package com.trioForce.voyage.marketing

import org.springframework.data.jpa.repository.JpaRepository

interface PromotionRepository : JpaRepository<PromotionEntity, Long> {
    fun findAllByIsActiveIsTrueOrderByIdAsc(): List<PromotionEntity>
}
