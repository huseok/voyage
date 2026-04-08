package com.trioForce.voyage.order

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "t_order_items")
class OrderItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "order_id", nullable = false)
    var orderId: Long,

    @Column(name = "product_id", nullable = false)
    var productId: Long,

    @Column(name = "title_snapshot", nullable = false, length = 255)
    var titleSnapshot: String,

    @Column(name = "price_snapshot", nullable = false, precision = 12, scale = 2)
    var priceSnapshot: BigDecimal,

    @Column(nullable = false)
    var quantity: Int,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()
)
