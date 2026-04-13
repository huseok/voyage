package com.trioForce.voyage.cart

import jakarta.persistence.*
import org.hibernate.annotations.Where
import java.time.OffsetDateTime

@Entity
@Table(
    name = "t_cart_items",
    uniqueConstraints = [UniqueConstraint(name = "uk_t_cart_items_user_product", columnNames = ["user_id", "product_id"])]
)
@Where(clause = "is_deleted = false")
class CartItemEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "product_id", nullable = false)
    var productId: Long,

    @Column(nullable = false)
    var quantity: Int,

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
