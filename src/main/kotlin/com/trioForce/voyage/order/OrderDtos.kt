package com.trioForce.voyage.order

import jakarta.validation.constraints.NotBlank

/**
 * 创建订单：
 * - 传 [savedAddressId] 时，收货人/电话/国家/地址/省/市/邮编从 **已保存地址** 快照写入（须属于当前用户）；
 * - 否则须提供 [receiverName]、[receiverPhone]、[country]、[addressLine]（由服务层校验）。
 */
data class CreateOrderRequest(
    val savedAddressId: Long? = null,
    val receiverName: String? = null,
    val receiverPhone: String? = null,
    val country: String? = null,
    val addressLine: String? = null,
    val postalCode: String? = null,
    val receiverCompany: String? = null,
    val taxNo: String? = null,
    val incoterm: String? = null,
    val shippingMethod: String? = null,
    /** 结构化省/市（手动填单时使用；与 savedAddress 二选一优先地址库）。 */
    val receiverProvince: String? = null,
    val receiverCity: String? = null,
    /**
     * 仅结算这些购物车行 id（须属于当前用户且已勾选）；为空则结算**全部已勾选**行。
     */
    val cartItemIds: List<Long>? = null,
    val couponCode: String? = null,
)

data class UpdateTrackingRequest(
    @field:NotBlank val trackingNo: String,
    val logisticsCompany: String? = null,
)

data class UpdateOrderStatusRequest(
    @field:NotBlank val status: String,
    val remark: String? = null,
)

data class FlowNextOrderStatusRequest(
    val remark: String? = null,
)

data class ConfirmDeliveredRequest(
    @field:NotBlank val status: String = "COMPLETED",
)

data class OrderItemView(
    val productId: Long,
    val titleSnapshot: String,
    val priceSnapshot: String,
    val quantity: Int,
    val thumbUrl: String?,
)

data class PagedOrders(
    val items: List<OrderView>,
    val total: Long,
    val page: Int,
    val size: Int,
)

data class OrderView(
    val orderNo: String,
    val status: String,
    val paymentStatus: String,
    val totalAmount: String,
    val subtotalAmount: String,
    val discountMember: String,
    val discountPromo: String,
    val discountCoupon: String,
    val shippingFee: String,
    val couponCodeSnapshot: String?,
    val paypalOrderId: String?,
    val currency: String,
    val receiverName: String,
    val receiverPhone: String,
    val receiverCompany: String?,
    val taxNo: String?,
    val country: String,
    val addressLine: String,
    val receiverProvince: String?,
    val receiverCity: String?,
    val postalCode: String?,
    val incoterm: String?,
    val shippingMethod: String?,
    val logisticsCompany: String?,
    val trackingNo: String?,
    val items: List<OrderItemView>,
)
