package com.trioForce.voyage.cart

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank

data class AddCartItemRequest(
    val productId: Long,
    @field:Min(1) val quantity: Int
)

data class UpdateCartItemRequest(
    @field:Min(1) val quantity: Int
)

data class UpdateCartSelectionRequest(
    val itemIds: List<Long>,
    val selected: Boolean
)

data class BulkDeleteCartRequest(
    val itemIds: List<Long>
)

/** 将历史订单商品按行加回购物车（仅上架 SKU；数量不低于当前 MOQ）。 */
data class ReorderToCartRequest(
    @field:NotBlank val orderNo: String,
)

data class CartItemView(
    val itemId: Long,
    val productId: Long,
    val title: String,
    val moq: Int,
    val quantity: Int,
    val unitPrice: String,
    val currency: String,
    val lineAmount: String,
    /** 是否勾选参与结算 */
    val selected: Boolean,
    /** 商品是否上架；下架后前台应置灰并禁止勾选。 */
    val productActive: Boolean,
    /** 与 [unitPrice] 比较可发现改价；仅展示用。 */
    val listPrice: String?,
    /** 主图缩略 URL，便于购物车展示。 */
    val thumbUrl: String?,
)

data class CartView(
    val items: List<CartItemView>,
    /** 购物车内全部行的小计（不按勾选）。 */
    val totalAmount: String,
    /** 仅 [CartItemView.selected]==true 的行小计。 */
    val selectedSubtotal: String,
    val currency: String,
)
