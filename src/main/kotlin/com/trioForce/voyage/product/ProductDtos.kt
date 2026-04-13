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
    @field:DecimalMin("0.0") val weightKg: BigDecimal? = null,
    val categoryId: Long? = null,
    val shippingTemplateId: Long? = null,
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
    val weightKg: BigDecimal?,
    val categoryId: Long?,
    val shippingTemplateId: Long?,
    val isActive: Boolean,
    val options: List<ProductOptionView> = emptyList(),
    val skus: List<ProductSkuView> = emptyList(),
    // 未登录时该字段为 null，用于前端控制“登录后看价格”
    val price: BigDecimal?,
    val currency: String?
)

data class ProductOptionView(
    val optionName: String,
    val optionValue: String,
    val sortNo: Int
)

data class ProductSkuView(
    val id: Long,
    val skuCode: String,
    val attrJson: String,
    val salePrice: BigDecimal,
    val stockQty: Int,
    val weightKg: BigDecimal?,
    val isActive: Boolean
)

data class ProductOptionInput(
    @field:NotBlank val optionName: String,
    @field:NotBlank val optionValue: String,
    val sortNo: Int = 0
)

data class ProductSkuInput(
    @field:NotBlank val skuCode: String,
    @field:NotBlank val attrJson: String,
    @field:DecimalMin("0.01") val salePrice: BigDecimal,
    @field:Min(0) val stockQty: Int,
    @field:DecimalMin("0.0") val weightKg: BigDecimal? = null,
    val isActive: Boolean = true
)

data class ProductSkuMatrixUpsertRequest(
    val options: List<ProductOptionInput>,
    val skus: List<ProductSkuInput>
)

data class ProductSkuMatrixView(
    val productId: Long,
    val options: List<ProductOptionView>,
    val skus: List<ProductSkuView>
)

data class ProductBulkStatusRequest(
    val ids: List<Long>,
    val isActive: Boolean
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
