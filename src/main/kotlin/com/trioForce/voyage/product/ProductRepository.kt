package com.trioForce.voyage.product

import org.springframework.data.jpa.repository.JpaRepository

interface ProductRepository : JpaRepository<ProductEntity, Long> {
    fun findAllByIsActiveTrue(): List<ProductEntity>
}
