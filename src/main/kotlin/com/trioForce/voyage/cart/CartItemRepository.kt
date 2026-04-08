package com.trioForce.voyage.cart

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface CartItemRepository : JpaRepository<CartItemEntity, Long> {
    fun findAllByUserId(userId: Long): List<CartItemEntity>
    fun findByUserIdAndProductId(userId: Long, productId: Long): Optional<CartItemEntity>
}
