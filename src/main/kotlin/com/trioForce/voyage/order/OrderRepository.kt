package com.trioForce.voyage.order

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.util.Optional

interface OrderRepository : JpaRepository<OrderEntity, Long>, JpaSpecificationExecutor<OrderEntity> {
    fun findAllByUserIdOrderByIdDesc(userId: Long): List<OrderEntity>
    fun findByOrderNo(orderNo: String): Optional<OrderEntity>
    fun existsByOrderNo(orderNo: String): Boolean

    fun countByStatus(status: String): Long

    fun countByPaymentStatus(paymentStatus: String): Long

    @Query("SELECT COUNT(o) FROM OrderEntity o WHERE o.createdAt >= :since")
    fun countCreatedSince(@Param("since") since: OffsetDateTime): Long

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM OrderEntity o WHERE o.paymentStatus = 'PAID'")
    fun sumTotalAmountPaid(): BigDecimal
}
