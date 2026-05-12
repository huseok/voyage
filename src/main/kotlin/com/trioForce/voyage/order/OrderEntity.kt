package com.trioForce.voyage.order

import jakarta.persistence.*
import org.hibernate.annotations.Where
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "t_orders")
@Where(clause = "is_deleted = false")
/**
 * 订单主表：
 * 存储订单状态、收货信息与外贸交易补充字段。
 */
class OrderEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(name = "order_no", nullable = false, unique = true, length = 64)
    var orderNo: String,

    @Column(name = "user_id", nullable = false)
    var userId: Long,

    @Column(nullable = false, length = 32)
    var status: String,

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    var totalAmount: BigDecimal,

    /** 选中商品**折前**小计（不含会员/券/满减/运费）。 */
    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    var subtotalAmount: BigDecimal = BigDecimal.ZERO,

    @Column(name = "discount_member", nullable = false, precision = 12, scale = 2)
    var discountMember: BigDecimal = BigDecimal.ZERO,

    @Column(name = "discount_coupon", nullable = false, precision = 12, scale = 2)
    var discountCoupon: BigDecimal = BigDecimal.ZERO,

    @Column(name = "discount_promo", nullable = false, precision = 12, scale = 2)
    var discountPromo: BigDecimal = BigDecimal.ZERO,

    @Column(name = "shipping_fee", nullable = false, precision = 12, scale = 2)
    var shippingFee: BigDecimal = BigDecimal.ZERO,

    @Column(name = "coupon_code_snapshot", length = 64)
    var couponCodeSnapshot: String? = null,

    @Column(name = "paypal_order_id", length = 128)
    var paypalOrderId: String? = null,

    @Column(name = "payment_status", nullable = false, length = 32)
    var paymentStatus: String = "UNPAID",

    @Column(nullable = false, length = 8)
    var currency: String = "USD",

    @Column(name = "receiver_name", nullable = false, length = 100)
    var receiverName: String,

    @Column(name = "receiver_phone", nullable = false, length = 30)
    var receiverPhone: String,

    // 外贸场景常见：公司名与税号
    @Column(name = "receiver_company", length = 120)
    var receiverCompany: String? = null,

    @Column(name = "tax_no", length = 60)
    var taxNo: String? = null,

    @Column(nullable = false, length = 60)
    var country: String,

    @Column(name = "address_line", nullable = false, length = 255)
    var addressLine: String,

    @Column(name = "receiver_province", length = 100)
    var receiverProvince: String? = null,

    @Column(name = "receiver_city", length = 100)
    var receiverCity: String? = null,

    @Column(name = "postal_code", length = 20)
    var postalCode: String? = null,

    @Column(name = "incoterm", length = 20)
    var incoterm: String? = null,

    @Column(name = "shipping_method", length = 30)
    var shippingMethod: String? = null,

    @Column(name = "logistics_company", length = 100)
    var logisticsCompany: String? = null,

    // 后台发货时填写
    @Column(name = "tracking_no", length = 100)
    var trackingNo: String? = null,

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
