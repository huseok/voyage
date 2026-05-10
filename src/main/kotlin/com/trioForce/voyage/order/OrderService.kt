package com.trioForce.voyage.order

import com.trioForce.voyage.audit.OrderStatusHistoryEntity
import com.trioForce.voyage.audit.OrderStatusHistoryRepository
import com.trioForce.voyage.cart.CartItemRepository
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.dictionary.DictionaryService
import com.trioForce.voyage.product.ProductRepository
import com.trioForce.voyage.security.CurrentUser
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.ThreadLocalRandom

/**
 * 订单服务：
 * 负责从购物车创建订单、查询订单、状态流转和物流信息维护。
 */
@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val dictionaryService: DictionaryService
) {
    /**
     * 从购物车创建订单，并清空购物车。
     *
     * @param req 收货与贸易参数
     * @return 新订单号
     */
    @Transactional
    fun createOrder(req: CreateOrderRequest): String {
        val userId = CurrentUser.userId()
        val cartItems = cartItemRepository.findAllByUserId(userId)
        if (cartItems.isEmpty()) throw BizException("cart is empty")

        var total = BigDecimal.ZERO
        var currency = "USD"
        val snapshots = cartItems.map {
            val product = productRepository.findById(it.productId).orElseThrow { BizException("product not found: ${it.productId}") }
            val lineAmount = product.price.multiply(BigDecimal.valueOf(it.quantity.toLong()))
            total = total.add(lineAmount)
            currency = product.currency
            Triple(it, product, lineAmount)
        }

        val now = OffsetDateTime.now()
        val orderNo = generateOrderNo()
        val order = orderRepository.save(
            OrderEntity(
                orderNo = orderNo,
                userId = userId,
                status = "PENDING_PAYMENT",
                totalAmount = total,
                currency = currency,
                receiverName = req.receiverName.trim(),
                receiverPhone = req.receiverPhone.trim(),
                receiverCompany = req.receiverCompany?.trim(),
                taxNo = req.taxNo?.trim(),
                country = req.country.trim(),
                addressLine = req.addressLine.trim(),
                postalCode = req.postalCode?.trim(),
                incoterm = req.incoterm?.trim()?.uppercase(),
                shippingMethod = req.shippingMethod?.trim(),
                createdAt = now,
                updatedAt = now
            )
        )
        // 记录初始状态，便于后台追踪订单生命周期。
        appendStatusHistory(order.id!!, null, "PENDING_PAYMENT", "create order")

        orderItemRepository.saveAll(
            snapshots.map {
                OrderItemEntity(
                    orderId = order.id!!,
                    productId = it.second.id!!,
                    titleSnapshot = it.second.title,
                    priceSnapshot = it.second.price,
                    quantity = it.first.quantity,
                    createdAt = now
                )
            }
        )

        // 下单后清空购物车，避免重复下单
        cartItemRepository.deleteAll(cartItems)
        return orderNo
    }

    /**
     * 查询当前用户订单详情。
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    fun getOrder(orderNo: String): OrderView {
        val userId = CurrentUser.userId()
        val order = orderRepository.findByOrderNo(orderNo).orElseThrow { BizException("order not found") }
        if (order.userId != userId) throw BizException("forbidden order")
        return toView(order)
    }

    /**
     * 查询当前用户订单列表。
     *
     * @return 当前用户全部订单（倒序）
     */
    fun listMyOrders(): List<OrderView> = orderRepository.findAllByUserIdOrderByIdDesc(CurrentUser.userId()).map { toView(it) }

    /**
     * 后台查询全部订单（按订单实体主键 id 倒序）。
     *
     * 与 [listMyOrders] 不同：不限制当前用户，供运营/管理员总览；调用方需已通过 ADMIN 鉴权。
     *
     * @return 全库订单视图列表
     */
    fun listAllForAdmin(): List<OrderView> =
        orderRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).map { toView(it) }

    /**
     * 后台录入物流信息。
     *
     * @param orderNo 订单号
     * @param req 运单与物流参数
     */
    @Transactional
    fun adminUpdateTracking(orderNo: String, req: UpdateTrackingRequest) {
        val order = orderRepository.findByOrderNo(orderNo).orElseThrow { BizException("order not found") }
        if (order.status != "PAID" && order.status != "SHIPPED") throw BizException("only paid/shipped order can set tracking")
        val fromStatus = order.status
        order.trackingNo = req.trackingNo.trim()
        order.logisticsCompany = req.logisticsCompany?.trim()
        // 录入运单号后自动切到 SHIPPED，减少后台手工步骤
        order.status = "SHIPPED"
        order.updatedAt = OffsetDateTime.now()
        orderRepository.save(order)
        appendStatusHistory(order.id!!, fromStatus, "SHIPPED", "update tracking")
    }

    /**
     * 后台手工推进订单状态。
     *
     * @param orderNo 订单号
     * @param status 目标状态
     */
    @Transactional
    fun adminUpdateStatus(orderNo: String, status: String, remark: String? = null) {
        val order = orderRepository.findByOrderNo(orderNo).orElseThrow { BizException("order not found") }
        val next = status.trim().uppercase()
        validateTransition(order.status, next, allowSkip = true)
        val fromStatus = order.status
        order.status = next
        order.updatedAt = OffsetDateTime.now()
        orderRepository.save(order)
        appendStatusHistory(order.id!!, fromStatus, next, remark?.trim().takeUnless { it.isNullOrBlank() } ?: "admin update status")
    }

    /**
     * 后台自动流转到下一状态（按字典 ORDER_STATUS 排序）。
     */
    @Transactional
    fun adminFlowNextStatus(orderNo: String, remark: String? = null) {
        val order = orderRepository.findByOrderNo(orderNo).orElseThrow { BizException("order not found") }
        val chain = getConfiguredOrderStatuses()
        val currentIdx = chain.indexOf(order.status)
        if (currentIdx < 0) throw BizException("current status not configured in ORDER_STATUS: ${order.status}")
        if (currentIdx >= chain.lastIndex) throw BizException("already at final status")
        val next = chain[currentIdx + 1]
        val fromStatus = order.status
        order.status = next
        order.updatedAt = OffsetDateTime.now()
        orderRepository.save(order)
        appendStatusHistory(order.id!!, fromStatus, next, remark?.trim().takeUnless { it.isNullOrBlank() } ?: "flow next")
    }

    /**
     * 用户确认订单完成。
     *
     * @param orderNo 订单号
     */
    @Transactional
    fun confirmCompleted(orderNo: String) {
        val userId = CurrentUser.userId()
        val order = orderRepository.findByOrderNo(orderNo).orElseThrow { BizException("order not found") }
        if (order.userId != userId) throw BizException("forbidden order")
        if (order.status != "DELIVERED" && order.status != "SHIPPED") throw BizException("order cannot be completed now")
        val fromStatus = order.status
        order.status = "COMPLETED"
        order.updatedAt = OffsetDateTime.now()
        orderRepository.save(order)
        appendStatusHistory(order.id!!, fromStatus, "COMPLETED", "confirm completed")
    }

    private fun appendStatusHistory(orderId: Long, from: String?, to: String, remark: String) {
        orderStatusHistoryRepository.save(
            OrderStatusHistoryEntity(
                orderId = orderId,
                fromStatus = from,
                toStatus = to,
                changedBy = runCatching { CurrentUser.userId() }.getOrNull(),
                changedAt = OffsetDateTime.now(),
                remark = remark
            )
        )
    }

    private fun toView(order: OrderEntity): OrderView {
        val items = orderItemRepository.findAllByOrderId(order.id!!).map {
            OrderItemView(
                productId = it.productId,
                titleSnapshot = it.titleSnapshot,
                priceSnapshot = it.priceSnapshot.toPlainString(),
                quantity = it.quantity
            )
        }
        return OrderView(
            orderNo = order.orderNo,
            status = order.status,
            totalAmount = order.totalAmount.toPlainString(),
            currency = order.currency,
            receiverName = order.receiverName,
            receiverPhone = order.receiverPhone,
            receiverCompany = order.receiverCompany,
            taxNo = order.taxNo,
            country = order.country,
            addressLine = order.addressLine,
            postalCode = order.postalCode,
            incoterm = order.incoterm,
            shippingMethod = order.shippingMethod,
            logisticsCompany = order.logisticsCompany,
            trackingNo = order.trackingNo,
            items = items
        )
    }

    private fun validateTransition(current: String, next: String, allowSkip: Boolean) {
        val chain = getConfiguredOrderStatuses()
        val currentIdx = chain.indexOf(current)
        val nextIdx = chain.indexOf(next)
        if (currentIdx < 0 || nextIdx < 0) {
            throw BizException("status not configured in ORDER_STATUS")
        }
        if (nextIdx <= currentIdx) {
            throw BizException("status cannot rollback or keep same: $current -> $next")
        }
        if (!allowSkip && nextIdx != currentIdx + 1) {
            throw BizException("status must flow to next step: $current -> $next")
        }
    }

    private fun getConfiguredOrderStatuses(): List<String> {
        val items = dictionaryService.listItems("ORDER_STATUS")
        if (items.isEmpty()) throw BizException("ORDER_STATUS dictionary is empty")
        return items.sortedBy { it.sortNo }.map { it.itemCode.uppercase() }
    }

    /**
     * 订单号：`GB-{yyMMdd}-{HHmmss}-{ssssss}`
     * - `yyMMdd`：下单日（公历）
     * - `HHmmss`：时分秒
     * - 末段 6 位随机数，同日同时段可读；冲突时重试生成
     */
    private fun generateOrderNo(): String {
        val now = OffsetDateTime.now()
        val date = now.format(DateTimeFormatter.ofPattern("yyMMdd"))
        val time = now.format(DateTimeFormatter.ofPattern("HHmmss"))
        repeat(16) {
            val seq = ThreadLocalRandom.current().nextInt(0, 1_000_000)
            val candidate = String.format("GB-%s-%s-%06d", date, time, seq)
            if (!orderRepository.existsByOrderNo(candidate)) return candidate
        }
        val fallback = String.format("GB-%s-%s-%09d", date, time, System.nanoTime() % 1_000_000_000L)
        if (!orderRepository.existsByOrderNo(fallback)) return fallback
        return String.format("GB-%s-%s-%s", date, time, java.util.UUID.randomUUID().toString().replace("-", "").take(12).uppercase())
    }
}
