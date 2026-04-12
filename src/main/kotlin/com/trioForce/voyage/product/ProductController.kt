package com.trioForce.voyage.product

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
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
     * @param q 可选，标题 / SKU / ID 模糊或精确匹配
     */
    @GetMapping("/api/v1/products")
    fun list(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) country: String?,
        @RequestParam(required = false) q: String?,
        authentication: Authentication?
    ): ApiResponse<PagedProducts> =
        ok(
            productService.listPublicPage(
                page,
                size,
                country,
                q,
                authentication?.isAuthenticated == true
            )
        )

    /**
     * 前台商品详情（仅上架）。
     */
    @GetMapping("/api/v1/products/{id}")
    fun detail(@PathVariable id: Long, authentication: Authentication?): ApiResponse<ProductView> =
        ok(productService.detailPublic(id, authentication?.isAuthenticated == true))

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
        @RequestParam(required = false) active: String?
    ): ApiResponse<PagedProducts> =
        ok(productService.listAdminPage(page, size, q, parseActiveFilter(active)))

    /**
     * 管理端商品详情（含下架），用于编辑页加载。
     */
    @GetMapping("/api/v1/admin/products/{id}")
    fun adminDetail(@PathVariable id: Long): ApiResponse<ProductView> =
        ok(productService.adminDetail(id))

    /**
     * 后台创建商品。
     */
    @PostMapping("/api/v1/admin/products")
    fun create(@Valid @RequestBody req: ProductAdminUpsertRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to productService.create(req)))

    /**
     * 后台更新商品。
     */
    @PutMapping("/api/v1/admin/products/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: ProductAdminUpsertRequest): ApiResponse<String> {
        productService.update(id, req)
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
