package com.trioForce.voyage.product

import com.trioForce.voyage.tag.TagView
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal

data class ProductImageView(
    val thumbUrl: String,
    val fullUrl: String,
)

data class ProductImageRef(
    val thumbUrl: String,
    val fullUrl: String,
)

data class ProductAdminUpsertRequest(
    @field:NotBlank val title: String,
    @field:DecimalMin("0.01") val price: BigDecimal,
    /** 划线原价；须 ≥ [price]；null 表示清空划线价 */
    @field:DecimalMin("0.01") val listPrice: BigDecimal? = null,
    /** 成本价；可选；null 表示清空 */
    @field:DecimalMin("0.0") val costPrice: BigDecimal? = null,
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
    val isActive: Boolean = true,
    /** null：不改图片；[]：清空；非空：覆盖排序 */
    val images: List<ProductImageRef>? = null,
    /** null：不改标签关联；[]：清空；非空：覆盖为指定标签 id 集合 */
    val tagIds: List<Long>? = null,
)

data class ProductView(
    /** 对外雪花 ID（十进制字符串） */
    val id: String,
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
    /** 前台列表优先展示首张 thumbUrl */
    val images: List<ProductImageView> = emptyList(),
    /** 商品标签（来自 t_product_tags + t_tags） */
    val tags: List<TagView> = emptyList(),
    /** 主档单价（活动/现价）；前台接口亦返回（不要求登录） */
    val price: BigDecimal?,
    /** 划线原价；非空且大于 [price] 时可供促销展示 */
    val listPrice: BigDecimal?,
    /** 成本价；仅管理端接口返回，前台为 null */
    val costPrice: BigDecimal? = null,
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
    @field:DecimalMin("0.0") val salePrice: BigDecimal,
    @field:Min(0) val stockQty: Int,
    @field:DecimalMin("0.0") val weightKg: BigDecimal? = null,
    val isActive: Boolean = true
)

data class ProductSkuMatrixUpsertRequest(
    val options: List<ProductOptionInput>,
    val skus: List<ProductSkuInput>
)

data class ProductSkuMatrixView(
    val productId: String,
    val options: List<ProductOptionView>,
    val skus: List<ProductSkuView>
)

data class ProductBulkStatusRequest(
    val ids: List<String>,
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
