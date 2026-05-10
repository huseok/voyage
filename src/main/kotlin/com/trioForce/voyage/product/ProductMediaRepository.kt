package com.trioForce.voyage.product

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductMediaRepository : JpaRepository<ProductMediaEntity, Long> {
    fun findAllByProductIdOrderBySortNoAscIdAsc(productId: Long): List<ProductMediaEntity>

    fun findAllByProductIdIn(productIds: Collection<Long>): List<ProductMediaEntity>

    @Modifying(clearAutomatically = true)
    @Query("delete from ProductMediaEntity m where m.productId = :productId")
    fun deleteAllByProductId(@Param("productId") productId: Long): Int
}
