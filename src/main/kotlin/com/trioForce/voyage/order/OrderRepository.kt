package com.trioForce.voyage.order

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import java.util.Optional

interface OrderRepository : JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<OrderEntity>
    fun findByOrderNo(orderNo: String): Optional<OrderEntity>
    fun existsByOrderNo(orderNo: String): Boolean
}
