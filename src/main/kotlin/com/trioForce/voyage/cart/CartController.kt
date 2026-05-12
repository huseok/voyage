package com.trioForce.voyage.cart

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 购物车控制器：登录用户维护购物车与勾选状态。
 */
@RestController
@RequestMapping("/api/v1/cart")
@Tag(name = "Cart", description = "购物车")
class CartController(
    private val cartService: CartService,
) {
    @GetMapping
    fun getCart(): ApiResponse<CartView> = ok(cartService.getCart())

    @PostMapping("/items")
    fun addItem(@Valid @RequestBody req: AddCartItemRequest): ApiResponse<String> {
        cartService.addItem(req)
        return ok("added")
    }

    @PatchMapping("/items/{itemId}")
    fun updateItem(@PathVariable itemId: Long, @Valid @RequestBody req: UpdateCartItemRequest): ApiResponse<String> {
        cartService.updateItem(itemId, req)
        return ok("updated")
    }

    /** 批量更新勾选：用于全选/反选/单选同步。 */
    @PatchMapping("/selection")
    fun updateSelection(@Valid @RequestBody req: UpdateCartSelectionRequest): ApiResponse<String> {
        cartService.updateSelection(req)
        return ok("selection updated")
    }

    /** 批量删除行。 */
    @PostMapping("/bulk-delete")
    fun bulkDelete(@Valid @RequestBody req: BulkDeleteCartRequest): ApiResponse<String> {
        cartService.bulkRemove(req)
        return ok("deleted")
    }

    /** 清空当前用户购物车。 */
    @DeleteMapping("/clear")
    fun clear(): ApiResponse<String> {
        cartService.clear()
        return ok("cleared")
    }

    /** 将指定历史订单中仍上架的商品加回购物车。 */
    @PostMapping("/reorder-from-order")
    fun reorderFromOrder(@Valid @RequestBody req: ReorderToCartRequest): ApiResponse<String> {
        cartService.reorderFromOrder(req)
        return ok("reordered")
    }

    @DeleteMapping("/items/{itemId}")
    fun removeItem(@PathVariable itemId: Long): ApiResponse<String> {
        cartService.removeItem(itemId)
        return ok("removed")
    }
}
