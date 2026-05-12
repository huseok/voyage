package com.trioForce.voyage.stats

import com.trioForce.voyage.aftersale.AfterSaleRepository
import com.trioForce.voyage.order.OrderRepository
import com.trioForce.voyage.product.ProductRepository
import com.trioForce.voyage.user.UserRepository
import org.springframework.stereotype.Service
import java.math.RoundingMode
import java.time.OffsetDateTime

@Service
class StatsAdminService(
    private val userRepository: UserRepository,
    private val productRepository: ProductRepository,
    private val orderRepository: OrderRepository,
    private val afterSaleRepository: AfterSaleRepository,
) {
    fun summary(): AdminStatsSummaryView {
        val since = OffsetDateTime.now().minusDays(7)
        val paidSum = orderRepository.sumTotalAmountPaid()
        val paidStr = paidSum.setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return AdminStatsSummaryView(
            totalUsers = userRepository.count(),
            newUsersLast7Days = userRepository.countCreatedSince(since),
            totalProducts = productRepository.count(),
            activeProducts = productRepository.countByIsActiveIsTrue(),
            totalOrders = orderRepository.count(),
            ordersLast7Days = orderRepository.countCreatedSince(since),
            pendingPaymentOrders = orderRepository.countByStatus("PENDING_PAYMENT"),
            paidOrders = orderRepository.countByPaymentStatus("PAID"),
            paidRevenueTotal = paidStr,
            totalAfterSales = afterSaleRepository.count(),
        )
    }
}
