package com.trioForce.voyage.order

import org.springframework.data.jpa.repository.JpaRepository

interface OrderItemRepository : JpaRepository<OrderItemEntity, Long> {
    fun findAllByOrderId(orderId: Long): List<OrderItemEntity>
}
