package com.trioForce.voyage.order

import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface OrderRepository : JpaRepository<OrderEntity, Long> {
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<OrderEntity>
    fun findByOrderNo(orderNo: String): Optional<OrderEntity>
}
