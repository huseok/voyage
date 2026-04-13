package com.trioForce.voyage.order

import jakarta.validation.constraints.NotBlank

data class CreateOrderRequest(
    @field:NotBlank val receiverName: String,
    @field:NotBlank val receiverPhone: String,
    @field:NotBlank val country: String,
    @field:NotBlank val addressLine: String,
    val postalCode: String? = null,
    val receiverCompany: String? = null,
    val taxNo: String? = null,
    val incoterm: String? = null,
    val shippingMethod: String? = null
)

data class UpdateTrackingRequest(
    @field:NotBlank val trackingNo: String,
    val logisticsCompany: String? = null
)

data class UpdateOrderStatusRequest(
    @field:NotBlank val status: String,
    val remark: String? = null
)

data class FlowNextOrderStatusRequest(
    val remark: String? = null
)

data class ConfirmDeliveredRequest(
    @field:NotBlank val status: String = "COMPLETED"
)

data class OrderItemView(
    val productId: Long,
    val titleSnapshot: String,
    val priceSnapshot: String,
    val quantity: Int
)

data class OrderView(
    val orderNo: String,
    val status: String,
    val totalAmount: String,
    val currency: String,
    val receiverName: String,
    val receiverPhone: String,
    val receiverCompany: String?,
    val taxNo: String?,
    val country: String,
    val addressLine: String,
    val postalCode: String?,
    val incoterm: String?,
    val shippingMethod: String?,
    val logisticsCompany: String?,
    val trackingNo: String?,
    val items: List<OrderItemView>
)
