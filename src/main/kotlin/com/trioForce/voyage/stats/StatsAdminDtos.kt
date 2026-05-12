package com.trioForce.voyage.stats

/**
 * 管理端首页级统计摘要（实时聚合，轻量缓存可后续加）。
 */
data class AdminStatsSummaryView(
    val totalUsers: Long,
    val newUsersLast7Days: Long,
    val totalProducts: Long,
    val activeProducts: Long,
    val totalOrders: Long,
    val ordersLast7Days: Long,
    val pendingPaymentOrders: Long,
    val paidOrders: Long,
    /** 已支付订单（payment_status=PAID）的 total_amount 合计，两位小数字符串 */
    val paidRevenueTotal: String,
    val totalAfterSales: Long,
)
