package com.trioForce.voyage.product

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "t_product_media")
/**
 * 商品图片行：与商品主表一对多；删除商品时由数据库级联删除关联媒体记录。
 */
class ProductMediaEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "product_id", nullable = false)
    var productId: Long,

    @Column(name = "thumb_url", nullable = false, length = 512)
    var thumbUrl: String,

    @Column(name = "full_url", nullable = false, length = 512)
    var fullUrl: String,

    @Column(name = "sort_no", nullable = false)
    var sortNo: Int = 0,

    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
)
