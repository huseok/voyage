package com.trioForce.voyage.cart

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 购物车控制器：
 * 对登录用户提供购物车管理接口。
 */
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "购物车")
class CartController(
    private val cartService: CartService
) {
    /**
     * 获取当前用户购物车。
     *
     * @return 购物车明细和总金额
     */
    @GetMapping
    fun getCart(): ApiResponse<CartView> = ok(cartService.getCart())

    /**
     * 添加商品到购物车。
     *
     * @param req 商品 ID 与数量
     * @return 添加结果
     */
    @PostMapping("/items")
    fun addItem(@Valid @RequestBody req: AddCartItemRequest): ApiResponse<String> {
        cartService.addItem(req)
        return ok("added")
    }

    /**
     * 修改购物车商品数量。
     *
     * @param itemId 购物车行 ID
     * @param req 新数量
     * @return 更新结果
     */
    @PatchMapping("/items/{itemId}")
    fun updateItem(@PathVariable itemId: Long, @Valid @RequestBody req: UpdateCartItemRequest): ApiResponse<String> {
        cartService.updateItem(itemId, req)
        return ok("updated")
    }

    /**
     * 删除购物车商品。
     *
     * @param itemId 购物车行 ID
     * @return 删除结果
     */
    @DeleteMapping("/items/{itemId}")
    fun removeItem(@PathVariable itemId: Long): ApiResponse<String> {
        cartService.removeItem(itemId)
        return ok("removed")
    }
}
