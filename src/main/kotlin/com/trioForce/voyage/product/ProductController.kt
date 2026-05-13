package com.trioForce.voyage.product

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 商品接口控制器：
 * 对前台开放查询，对后台开放维护接口。
 */
@RestController
@Tag(name = "Products", description = "商品查询与后台维护")
class ProductController(
    private val productService: ProductService
) {
    /**
     * 前台分页商品列表（仅上架）。
     *
     * @param page 页码，从 0 开始
     * @param size 每页条数，最大 100
     * @param country 可选，按原产国精确匹配（不区分大小写）
     * @param q 可选，标题 / SKU / publicId / 旧数字 id 模糊或精确匹配
     * @param categoryId 可选，按后台分类 ID 精确筛选（与 Header 类目横条联动）
     * @param tagId 可选，按标签 ID 筛选（已废弃，请用 tagCode）
     * @param tagCode 可选，按标签编码筛选（与标签管理 code 一致）
     * @param promo `true` 时仅返回「活动商品」：已设置划线价且划线价高于主档售价
     * @param minPrice 可选，主档售价 [price] 下限（含）；与币种一致的业务数据由运营保证，接口不做币种换算。
     * @param maxPrice 可选，主档售价上限（含）；若同时传 min 与 max 且 min 大于 max，服务层返回业务错误。
     */
    @GetMapping("/api/v1/products")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) country: String?,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) tagId: Long?,
        @RequestParam(required = false) tagCode: String?,
        @RequestParam(required = false) promo: Boolean?,
        @RequestParam(required = false) minPrice: java.math.BigDecimal?,
        @RequestParam(required = false) maxPrice: java.math.BigDecimal?,
    ): ApiResponse<PagedProducts> =
        ok(
            productService.listPublicPage(
                page,
                size,
                country,
                q,
                categoryId,
                tagId,
                tagCode,
                promo == true,
                minPrice,
                maxPrice,
            )
        )

    /**
     * 前台商品详情（仅上架）。路径 id 为 [ProductEntity.publicId] 雪花字符串。
     */
    @GetMapping("/api/v1/products/{id}")
    fun detail(@PathVariable id: String): ApiResponse<ProductView> =
        ok(productService.detailPublic(id))

    /**
     * 管理端分页商品列表（含下架）。
     *
     * @param active 可选：`true` 仅上架、`false` 仅下架；不传则全部
     */
    @GetMapping("/api/v1/admin/products")
    fun listAdmin(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) active: String?,
        @RequestParam(required = false) categoryId: Long?,
        @RequestParam(required = false) tagId: Long?,
        @RequestParam(required = false) tagCode: String?,
        @RequestParam(required = false) currency: String?,
    ): ApiResponse<PagedProducts> =
        ok(
            productService.listAdminPage(
                page,
                size,
                q,
                parseActiveFilter(active),
                categoryId,
                tagId,
                tagCode,
                currency,
            )
        )

    /**
     * 管理端商品详情（含下架），用于编辑页加载。
     */
    @GetMapping("/api/v1/admin/products/{id}")
    fun adminDetail(@PathVariable id: String): ApiResponse<ProductView> =
        ok(productService.adminDetail(id))

    /**
     * 后台创建商品。
     */
    @PostMapping("/api/v1/admin/products")
    fun create(@Valid @RequestBody req: ProductAdminUpsertRequest): ApiResponse<Map<String, String>> =
        ok(mapOf("id" to productService.create(req)))

    /**
     * 后台更新商品。
     */
    @PutMapping("/api/v1/admin/products/{id}")
    fun update(@PathVariable id: String, @Valid @RequestBody req: ProductAdminUpsertRequest): ApiResponse<String> {
        productService.update(id, req)
        return ok("updated")
    }

    /**
     * 后台批量上下架。
     */
    @PatchMapping("/api/v1/admin/products/bulk-status")
    fun bulkStatus(@Valid @RequestBody req: ProductBulkStatusRequest): ApiResponse<Map<String, Int>> =
        ok(mapOf("updated" to productService.bulkUpdateStatus(req.ids, req.isActive)))

    /**
     * 后台获取商品 SKU 规格矩阵。
     */
    @GetMapping("/api/v1/admin/products/{id}/sku-matrix")
    fun getSkuMatrix(@PathVariable id: String): ApiResponse<ProductSkuMatrixView> = ok(productService.getSkuMatrix(id))

    /**
     * 后台保存商品 SKU 规格矩阵（全量覆盖）。
     */
    @PutMapping("/api/v1/admin/products/{id}/sku-matrix")
    fun upsertSkuMatrix(@PathVariable id: String, @Valid @RequestBody req: ProductSkuMatrixUpsertRequest): ApiResponse<String> {
        productService.upsertSkuMatrix(id, req)
        return ok("updated")
    }

    private fun parseActiveFilter(raw: String?): Boolean? {
        if (raw.isNullOrBlank()) return null
        return when (raw.trim().lowercase()) {
            "true", "1", "yes" -> true
            "false", "0", "no" -> false
            else -> null
        }
    }
}
