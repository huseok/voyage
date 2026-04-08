package com.trioForce.voyage.aftersale

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "t_after_sales")
class AfterSaleEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(name = "order_no", nullable = false, length = 64)
    var orderNo: String,

    @Column(nullable = false, length = 32)
    var status: String = "OPEN",

    @Column(nullable = false, columnDefinition = "text")
    var content: String,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
