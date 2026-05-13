package com.trioForce.voyage.order

import org.springframework.data.jpa.repository.JpaRepository

interface OrderLogisticsRepository : JpaRepository<OrderLogisticsEntity, Long> {
    fun findAllByOrderNoOrderByCreatedAtDesc(orderNo: String): List<OrderLogisticsEntity>
}
