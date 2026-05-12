package com.trioForce.voyage.order

import jakarta.persistence.*
import org.hibernate.annotations.Where
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "t_order_items")
@Where(clause = "is_deleted = false")
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

    @Column(name = "thumb_url", length = 512)
    var thumbUrl: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "created_by")
    var createdBy: Long? = null,

    @Column(name = "updated_by")
    var updatedBy: Long? = null,

    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,

    @Column(name = "deleted_by")
    var deletedBy: Long? = null,

    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null
)
