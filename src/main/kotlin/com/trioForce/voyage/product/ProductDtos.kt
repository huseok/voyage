package com.trioForce.voyage.product

import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class ProductAdminUpsertRequest(
    @field:NotBlank val title: String,
    @field:DecimalMin("0.01") val price: BigDecimal,
    @field:NotBlank val currency: String,
    @field:Min(1) val moq: Int,
    val description: String? = null,
    val skuCode: String? = null,
    val hsCode: String? = null,
    val unit: String? = null,
    val incoterm: String? = null,
    val originCountry: String? = null,
    val leadTimeDays: Int? = null,
    val isActive: Boolean = true
)

data class ProductView(
    val id: Long,
    val title: String,
    val moq: Int,
    val description: String?,
    val skuCode: String?,
    val hsCode: String?,
    val unit: String?,
    val incoterm: String?,
    val originCountry: String?,
    val leadTimeDays: Int?,
    val isActive: Boolean,
    // 未登录时该字段为 null，用于前端控制“登录后看价格”
    val price: BigDecimal?,
    val currency: String?
)

/**
 * 分页商品列表（前台 / 管理端列表共用结构）。
 */
data class PagedProducts(
    val items: List<ProductView>,
    val total: Long,
    val page: Int,
    val size: Int
)
