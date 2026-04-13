package com.trioForce.voyage.cart

import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.product.ProductRepository
import com.trioForce.voyage.security.CurrentUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * 购物车服务：
 * 负责当前登录用户购物车的增删改查与金额汇总。
 */
@Service
class CartService(
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository
) {
    /**
     * 获取当前用户购物车详情。
     *
     * @return 购物车视图（行项目与汇总金额）
     */
    fun getCart(): CartView {
        val userId = CurrentUser.userId()
        val rows = cartItemRepository.findAllByUserId(userId)
        if (rows.isEmpty()) return CartView(emptyList(), "0.00", "USD")

        var total = BigDecimal.ZERO
        var currency = "USD"
        val items = rows.map { row ->
            val product = productRepository.findById(row.productId).orElseThrow { BizException("product not found: ${row.productId}") }
            val lineAmount = product.price.multiply(BigDecimal.valueOf(row.quantity.toLong()))
            total = total.add(lineAmount)
            currency = product.currency
            CartItemView(
                itemId = row.id!!,
                productId = row.productId,
                title = product.title,
                moq = product.moq,
                quantity = row.quantity,
                unitPrice = product.price.toPlainString(),
                currency = product.currency,
                lineAmount = lineAmount.toPlainString()
            )
        }
        return CartView(items, total.toPlainString(), currency)
    }

    /**
     * 添加商品到购物车。
     *
     * @param req 商品 ID 与数量
     */
    @Transactional
    fun addItem(req: AddCartItemRequest) {
        val userId = CurrentUser.userId()
        val product = productRepository.findById(req.productId).orElseThrow { BizException("product not found") }
        if (!product.isActive) throw BizException("product inactive")
        if (req.quantity < product.moq) throw BizException("quantity must >= moq ${product.moq}")

        val now = OffsetDateTime.now()
        val exists = cartItemRepository.findByUserIdAndProductId(userId, req.productId).orElse(null)
        if (exists != null) {
            exists.quantity += req.quantity
            exists.updatedAt = now
            cartItemRepository.save(exists)
            return
        }

        cartItemRepository.save(
            CartItemEntity(
                userId = userId,
                productId = req.productId,
                quantity = req.quantity,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    /**
     * 更新购物车行数量。
     *
     * @param itemId 购物车行 ID
     * @param req 新数量
     */
    @Transactional
    fun updateItem(itemId: Long, req: UpdateCartItemRequest) {
        val userId = CurrentUser.userId()
        val item = cartItemRepository.findById(itemId).orElseThrow { BizException("cart item not found") }
        if (item.userId != userId) throw BizException("forbidden cart item")
        val product = productRepository.findById(item.productId).orElseThrow { BizException("product not found") }
        if (req.quantity < product.moq) throw BizException("quantity must >= moq ${product.moq}")
        item.quantity = req.quantity
        item.updatedAt = OffsetDateTime.now()
        cartItemRepository.save(item)
    }

    /**
     * 删除购物车行。
     *
     * @param itemId 购物车行 ID
     */
    @Transactional
    fun removeItem(itemId: Long) {
        val userId = CurrentUser.userId()
        val item = cartItemRepository.findById(itemId).orElseThrow { BizException("cart item not found") }
        if (item.userId != userId) throw BizException("forbidden cart item")
        cartItemRepository.delete(item)
    }
}
