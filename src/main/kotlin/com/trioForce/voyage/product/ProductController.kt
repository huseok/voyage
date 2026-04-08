package com.trioForce.voyage.product

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.validation.Valid
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

/**
 * 商品接口控制器：
 * 对前台开放查询，对后台开放维护接口。
 */
@RestController
class ProductController(
    private val productService: ProductService
) {
    /**
     * 前台商品列表。
     *
     * @param authentication 当前认证信息，用于判断是否登录
     * @return 商品列表（未登录不返回价格）
     */
    @GetMapping("/api/v1/products")
    fun list(authentication: Authentication?): ApiResponse<List<ProductView>> =
        ok(productService.listPublic(authentication?.isAuthenticated == true))

    /**
     * 前台商品详情。
     *
     * @param id 商品 ID
     * @param authentication 当前认证信息
     * @return 商品详情（未登录不返回价格）
     */
    @GetMapping("/api/v1/products/{id}")
    fun detail(@PathVariable id: Long, authentication: Authentication?): ApiResponse<ProductView> =
        ok(productService.detailPublic(id, authentication?.isAuthenticated == true))

    /**
     * 后台创建商品。
     *
     * @param req 商品参数
     * @return 新商品 ID
     */
    @PostMapping("/api/v1/admin/products")
    fun create(@Valid @RequestBody req: ProductAdminUpsertRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to productService.create(req)))

    /**
     * 后台更新商品。
     *
     * @param id 商品 ID
     * @param req 商品参数
     * @return 更新结果
     */
    @PutMapping("/api/v1/admin/products/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: ProductAdminUpsertRequest): ApiResponse<String> {
        productService.update(id, req)
        return ok("updated")
    }
}
