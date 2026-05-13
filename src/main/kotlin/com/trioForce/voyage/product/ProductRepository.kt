package com.trioForce.voyage.product

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.Optional

interface ProductRepository : JpaRepository<ProductEntity, Long>, JpaSpecificationExecutor<ProductEntity> {
    fun countByIsActiveIsTrue(): Long

    fun findByPublicId(publicId: String): Optional<ProductEntity>
}
