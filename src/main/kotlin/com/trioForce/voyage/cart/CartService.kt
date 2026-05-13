package com.trioForce.voyage.cart

import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.order.OrderItemRepository
import com.trioForce.voyage.order.OrderRepository
import com.trioForce.voyage.product.ProductLookup
import com.trioForce.voyage.product.ProductMediaRepository
import com.trioForce.voyage.product.ProductRepository
import com.trioForce.voyage.security.CurrentUser
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * 购物车服务：
 * - 增删改查、行勾选 [CartItemEntity.selected]、批量删除与清空。
 * - 金额汇总：同时返回 **全车小计** 与 **仅勾选行小计**，供前台结算条展示。
 */
@Service
class CartService(
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val productLookup: ProductLookup,
    private val productMediaRepository: ProductMediaRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
) {
    fun getCart(): CartView {
        val userId = CurrentUser.userId()
        val rows = cartItemRepository.findAllByUserId(userId)
        if (rows.isEmpty()) return CartView(emptyList(), "0.00", "0.00", "USD")

        val thumbsByProduct = productMediaRepository.findAllByProductIdIn(rows.map { it.productId }.toSet())
            .groupBy { it.productId }
            .mapValues { (_, list) -> list.minByOrNull { it.sortNo } ?: list.first() }

        var totalAll = BigDecimal.ZERO
        var totalSelected = BigDecimal.ZERO
        var currency = "USD"
        val items = rows.map { row ->
            val product = productRepository.findById(row.productId).orElseThrow { BizException("product not found: ${row.productId}") }
            val lineAmount = product.price.multiply(BigDecimal.valueOf(row.quantity.toLong()))
            totalAll = totalAll.add(lineAmount)
            currency = product.currency
            if (row.selected) {
                totalSelected = totalSelected.add(lineAmount)
            }
            val media = thumbsByProduct[row.productId]
            CartItemView(
                itemId = row.id!!,
                productId = product.publicId,
                title = product.title,
                moq = product.moq,
                quantity = row.quantity,
                unitPrice = product.price.toPlainString(),
                currency = product.currency,
                lineAmount = lineAmount.toPlainString(),
                selected = row.selected,
                productActive = product.isActive,
                listPrice = product.listPrice?.toPlainString(),
                thumbUrl = media?.thumbUrl,
            )
        }
        return CartView(items, totalAll.toPlainString(), totalSelected.toPlainString(), currency)
    }

    @Transactional
    fun addItem(req: AddCartItemRequest) {
        val userId = CurrentUser.userId()
        val product = productLookup.requireEntityByClientKey(req.productId)
        val internalPid = product.id!!
        if (!product.isActive) throw BizException("product inactive")
        if (req.quantity < product.moq) throw BizException("quantity must >= moq ${product.moq}")

        val now = OffsetDateTime.now()
        val exists = cartItemRepository.findByUserIdAndProductId(userId, internalPid).orElse(null)
        if (exists != null) {
            exists.quantity += req.quantity
            exists.updatedAt = now
            cartItemRepository.save(exists)
            return
        }

        cartItemRepository.save(
            CartItemEntity(
                userId = userId,
                productId = internalPid,
                quantity = req.quantity,
                selected = true,
                createdAt = now,
                updatedAt = now,
            ),
        )
    }

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

    @Transactional
    fun updateSelection(req: UpdateCartSelectionRequest) {
        val userId = CurrentUser.userId()
        val ids = req.itemIds.toSet()
        if (ids.isEmpty()) return
        val rows = cartItemRepository.findAllByUserId(userId).filter { it.id != null && ids.contains(it.id) }
        val now = OffsetDateTime.now()
        rows.forEach {
            it.selected = req.selected
            it.updatedAt = now
            cartItemRepository.save(it)
        }
    }

    @Transactional
    fun bulkRemove(req: BulkDeleteCartRequest) {
        val userId = CurrentUser.userId()
        val ids = req.itemIds.toSet()
        if (ids.isEmpty()) return
        val rows = cartItemRepository.findAllByUserId(userId).filter { it.id != null && ids.contains(it.id) }
        cartItemRepository.deleteAll(rows)
    }

    @Transactional
    fun clear() {
        val userId = CurrentUser.userId()
        cartItemRepository.deleteAll(cartItemRepository.findAllByUserId(userId))
    }

    @Transactional
    fun removeItem(itemId: Long) {
        val userId = CurrentUser.userId()
        val item = cartItemRepository.findById(itemId).orElseThrow { BizException("cart item not found") }
        if (item.userId != userId) throw BizException("forbidden cart item")
        cartItemRepository.delete(item)
    }

    /**
     * 根据历史订单号将仍上架的商品行加回购物车（合并同 SKU 数量；数量不低于当前 MOQ）。
     */
    @Transactional
    fun reorderFromOrder(req: ReorderToCartRequest) {
        val userId = CurrentUser.userId()
        val order = orderRepository.findByOrderNo(req.orderNo.trim()).orElseThrow { BizException("order not found") }
        if (order.userId != userId) throw BizException("forbidden order")
        val items = orderItemRepository.findAllByOrderId(order.id!!).filter { !it.isDeleted }
        if (items.isEmpty()) throw BizException("order has no lines")
        for (line in items) {
            val product = productRepository.findById(line.productId).orElse(null) ?: continue
            if (!product.isActive) continue
            val qty = maxOf(line.quantity, product.moq)
            addItem(AddCartItemRequest(productId = product.publicId, quantity = qty))
        }
    }
}
