package com.trioForce.voyage.aftersale

import org.springframework.data.jpa.repository.JpaRepository

interface AfterSaleRepository : JpaRepository<AfterSaleEntity, Long> {
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<AfterSaleEntity>
}
