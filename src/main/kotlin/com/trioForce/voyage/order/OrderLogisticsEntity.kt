package com.trioForce.voyage.order

import jakarta.persistence.*
import java.time.OffsetDateTime

/**
 * 订单物流轨迹子表：一条订单可有多条记录（补录、换单号、承运商变更等），与 [OrderEntity.orderNo] 关联。
 * 订单主表上的 [OrderEntity.trackingNo] / [OrderEntity.logisticsCompany] 仍表示「当前对外展示」的最新快照。
 */
@Entity
@Table(name = "t_order_logistics")
class OrderLogisticsEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "order_no", nullable = false, length = 64)
    var orderNo: String,

    @Column(length = 120)
    var carrier: String? = null,

    @Column(name = "tracking_no", nullable = false, length = 128)
    var trackingNo: String,

    @Column(length = 500)
    var remark: String? = null,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),

    @Column(name = "created_by")
    var createdBy: Long? = null,
)
