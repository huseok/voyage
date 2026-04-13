package com.trioForce.voyage.cart

import jakarta.validation.constraints.Min

data class AddCartItemRequest(
    val productId: Long,
    @field:Min(1) val quantity: Int
)

data class UpdateCartItemRequest(
    @field:Min(1) val quantity: Int
)

data class CartItemView(
    val itemId: Long,
    val productId: Long,
    val title: String,
    val moq: Int,
    val quantity: Int,
    val unitPrice: String,
    val currency: String,
    val lineAmount: String
)

data class CartView(
    val items: List<CartItemView>,
    val totalAmount: String,
    val currency: String
)
