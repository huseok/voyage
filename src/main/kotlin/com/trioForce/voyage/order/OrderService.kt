package com.trioForce.voyage.order

import com.trioForce.voyage.audit.OrderStatusHistoryEntity
import com.trioForce.voyage.audit.OrderStatusHistoryRepository
import com.trioForce.voyage.cart.CartItemEntity
import com.trioForce.voyage.cart.CartItemRepository
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.dictionary.DictionaryService
import com.trioForce.voyage.loyalty.MembershipService
import com.trioForce.voyage.marketing.MarketingService
import com.trioForce.voyage.product.ProductEntity
import com.trioForce.voyage.product.ProductMediaRepository
import com.trioForce.voyage.product.ProductRepository
import com.trioForce.voyage.security.CurrentUser
import com.trioForce.voyage.usercenter.UserAddressRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
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
    private val productMediaRepository: ProductMediaRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val dictionaryService: DictionaryService,
    private val marketingService: MarketingService,
    private val membershipService: MembershipService,
    private val userAddressRepository: UserAddressRepository,
    private val orderLogisticsRepository: OrderLogisticsRepository,
) {
    /**
     * 从购物车创建订单：仅处理 **已勾选** 行；可选 [CreateOrderRequest.cartItemIds] 再收窄行集合。
     * 金额：商品小计 → 会员折扣 → 满减 → 优惠券 → 加运费（当前运费恒为 0，占位后续模板）。
     * 成功后删除对应购物车行。
     */
    @Transactional
    fun createOrder(req: CreateOrderRequest): String {
        val userId = CurrentUser.userId()
        val ship = resolveShipping(req, userId)
        var rows = cartItemRepository.findAllByUserId(userId).filter { it.selected }
        val idFilter = req.cartItemIds?.filter { it > 0 }?.toSet()
        if (idFilter != null && idFilter.isNotEmpty()) {
            rows = rows.filter { it.id != null && idFilter.contains(it.id) }
        }
        if (rows.isEmpty()) throw BizException("no items selected for checkout")

        val productIdSet = rows.map { it.productId }.toSet()
        val thumbsByProduct = productMediaRepository.findAllByProductIdIn(productIdSet)
            .groupBy { it.productId }
            .mapValues { (_, list) -> list.minByOrNull { it.sortNo } ?: list.first() }

        data class LineSnap(val cart: CartItemEntity, val product: ProductEntity, val lineAmount: BigDecimal, val thumbUrl: String?)

        val snaps = rows.map { row ->
            val product = productRepository.findById(row.productId).orElseThrow { BizException("product not found: ${row.productId}") }
            if (!product.isActive) throw BizException("product inactive: ${row.productId}")
            val line = product.price.multiply(BigDecimal.valueOf(row.quantity.toLong()))
            LineSnap(row, product, line, thumbsByProduct[row.productId]?.thumbUrl)
        }

        var currency = "USD"
        var subtotal = BigDecimal.ZERO
        for (s in snaps) {
            subtotal = subtotal.add(s.lineAmount)
            currency = s.product.currency
        }

        val tier = membershipService.getOrCreate(userId).tier
        val discountMember = membershipService.memberDiscountAmount(tier, subtotal)
        val discountPromo = marketingService.computeBestPromotionOff(subtotal, productIdSet)
        val afterMemberPromo = subtotal.subtract(discountMember).subtract(discountPromo).max(BigDecimal.ZERO)
        val (discountCoupon, couponSnap) = marketingService.computeCouponOff(
            req.couponCode,
            goodsSubtotalForMin = subtotal,
            applyBase = afterMemberPromo,
            productIds = productIdSet,
        )
        val shippingFee = BigDecimal.ZERO
        val payable = afterMemberPromo.subtract(discountCoupon).add(shippingFee).max(BigDecimal.ZERO)
            .setScale(2, RoundingMode.HALF_UP)

        val now = OffsetDateTime.now()
        val orderNo = generateOrderNo()
        val order = orderRepository.save(
            OrderEntity(
                orderNo = orderNo,
                userId = userId,
                status = "PENDING_PAYMENT",
                totalAmount = payable,
                subtotalAmount = subtotal,
                discountMember = discountMember,
                discountCoupon = discountCoupon,
                discountPromo = discountPromo,
                shippingFee = shippingFee,
                couponCodeSnapshot = couponSnap,
                paypalOrderId = null,
                paymentStatus = "UNPAID",
                currency = currency,
                receiverName = ship.receiverName,
                receiverPhone = ship.receiverPhone,
                receiverCompany = ship.receiverCompany,
                taxNo = req.taxNo?.trim(),
                country = ship.country,
                addressLine = ship.addressLine,
                receiverProvince = ship.receiverProvince,
                receiverCity = ship.receiverCity,
                postalCode = ship.postalCode,
                incoterm = req.incoterm?.trim()?.uppercase(),
                shippingMethod = req.shippingMethod?.trim(),
                createdAt = now,
                updatedAt = now,
            ),
        )
        appendStatusHistory(order.id!!, null, "PENDING_PAYMENT", "create order")

        orderItemRepository.saveAll(
            snaps.map { s ->
                OrderItemEntity(
                    orderId = order.id!!,
                    productId = s.product.id!!,
                    titleSnapshot = s.product.title,
                    priceSnapshot = s.product.price,
                    quantity = s.cart.quantity,
                    thumbUrl = s.thumbUrl,
                    createdAt = now,
                    updatedAt = now,
                )
            },
        )

        cartItemRepository.deleteAll(snaps.map { it.cart })
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

    /** 支付模块读取订单实体（校验归属与待支付状态） */
    fun getOrderEntityForPayment(orderNo: String): OrderEntity {
        val userId = CurrentUser.userId()
        val order = orderRepository.findByOrderNo(orderNo).orElseThrow { BizException("order not found") }
        if (order.userId != userId) throw BizException("forbidden order")
        if (order.paymentStatus == "PAID") throw BizException("order already paid")
        if (order.status != "PENDING_PAYMENT") throw BizException("order is not pending payment")
        return order
    }

    /** 创建 PayPal 订单后写入 paypal_order_id */
    @Transactional
    fun attachPayPalOrderId(orderNo: String, paypalOrderId: String) {
        val order = getOrderEntityForPayment(orderNo)
        order.paypalOrderId = paypalOrderId
        order.updatedAt = OffsetDateTime.now()
        orderRepository.save(order)
    }

    /** PayPal 捕获成功后标记订单已付 */
    @Transactional
    fun markPaidFromPayPal(orderNo: String, paypalOrderId: String): OrderView {
        val userId = CurrentUser.userId()
        val order = orderRepository.findByOrderNo(orderNo).orElseThrow { BizException("order not found") }
        if (order.userId != userId) throw BizException("forbidden order")
        if (order.paymentStatus == "PAID") return toView(order)
        if (order.status != "PENDING_PAYMENT") throw BizException("order is not pending payment")
        val existing = order.paypalOrderId?.trim().orEmpty()
        if (existing.isNotEmpty() && existing != paypalOrderId) {
            throw BizException("paypal order mismatch")
        }
        order.paypalOrderId = paypalOrderId
        val fromStatus = order.status
        order.status = "PAID"
        onBecamePaid(order, fromStatus)
        order.updatedAt = OffsetDateTime.now()
        orderRepository.save(order)
        appendStatusHistory(order.id!!, fromStatus, "PAID", "paypal capture")
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
     * 后台分页查询订单；可选关键字（订单号、收货人）、精确状态或阶段筛选（phase 与 status 同时传时以 status 为准）、按下单用户 id 精确筛选。
     */
    fun listAdminPage(
        page: Int,
        size: Int,
        q: String?,
        status: String?,
        phase: String?,
        userId: Long?,
    ): PagedOrders {
        val pageable = PageRequest.of(clampOrderPage(page), clampOrderSize(size), Sort.by(Sort.Direction.DESC, "id"))
        var spec: Specification<OrderEntity> = Specification { _, _, cb -> cb.conjunction() }
        userId?.takeIf { it > 0 }?.let { uid ->
            spec = spec.and { root, _, cb -> cb.equal(root.get<Long>("userId"), uid) }
        }
        keywordOrderSpec(q)?.let { spec = spec.and(it) }
        val st = status?.trim()?.takeUnless { it.isBlank() }?.uppercase()
        if (st != null) {
            spec = spec.and { root, _, cb -> cb.equal(root.get<String>("status"), st) }
        } else {
            phaseOrderSpec(phase)?.let { spec = spec.and(it) }
        }
        val result = orderRepository.findAll(spec, pageable)
        return PagedOrders(
            items = result.content.map { toView(it) },
            total = result.totalElements,
            page = result.number,
            size = result.size,
        )
    }

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
        persistLogisticsLine(
            orderNo = order.orderNo,
            trackingNo = req.trackingNo.trim(),
            carrier = req.logisticsCompany?.trim()?.takeUnless { it.isEmpty() },
            remark = "admin update tracking",
        )
    }

    /**
     * 后台手工推进订单状态。
     *
     * @param forceRepair 为 true 时允许字典顺序上的**回退**，此时 [remark] 必填（运营修复错单）。
     */
    @Transactional
    fun adminUpdateStatus(orderNo: String, status: String, remark: String? = null, forceRepair: Boolean = false) {
        val order = orderRepository.findByOrderNo(orderNo).orElseThrow { BizException("order not found") }
        val next = status.trim().uppercase()
        if (forceRepair) {
            val r = remark?.trim().orEmpty()
            if (r.isEmpty()) throw BizException("force repair requires remark")
            val chain = getConfiguredOrderStatuses()
            val currentIdx = chain.indexOf(order.status)
            val nextIdx = chain.indexOf(next)
            if (currentIdx < 0 || nextIdx < 0) {
                throw BizException("status not configured in ORDER_STATUS")
            }
            if (nextIdx >= currentIdx) {
                throw BizException("force repair only allows rollback to an earlier status")
            }
        } else {
            validateTransition(order.status, next, allowSkip = true)
        }
        val fromStatus = order.status
        order.status = next
        order.updatedAt = OffsetDateTime.now()
        onBecamePaid(order, fromStatus)
        orderRepository.save(order)
        val histRemark =
            remark?.trim().takeUnless { it.isNullOrBlank() }
                ?: if (forceRepair) "force repair" else "admin update status"
        appendStatusHistory(order.id!!, fromStatus, next, histRemark)
    }

    /** 后台单独写入一条物流轨迹（不修改订单主表快照时仍可追溯）。 */
    @Transactional
    fun adminAppendLogistics(orderNo: String, req: OrderLogisticsCreateRequest) {
        orderRepository.findByOrderNo(orderNo).orElseThrow { BizException("order not found") }
        persistLogisticsLine(
            orderNo = orderNo.trim(),
            trackingNo = req.trackingNo.trim(),
            carrier = req.carrier?.trim()?.takeUnless { it.isEmpty() },
            remark = req.remark?.trim()?.takeUnless { it.isEmpty() } ?: "admin append logistics",
        )
    }

    /** 后台逻辑删除订单（列表默认不可见，依赖实体 [@Where]）。 */
    @Transactional
    fun adminLogicalDelete(orderNo: String) {
        val order = orderRepository.findByOrderNo(orderNo).orElseThrow { BizException("order not found") }
        val uid = runCatching { CurrentUser.userId() }.getOrNull()
        val fromStatus = order.status
        order.isDeleted = true
        order.deletedAt = OffsetDateTime.now()
        order.deletedBy = uid
        order.updatedAt = OffsetDateTime.now()
        orderRepository.save(order)
        appendStatusHistory(order.id!!, fromStatus, fromStatus, "admin logical delete")
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

    private data class ResolvedShipping(
        val receiverName: String,
        val receiverPhone: String,
        val receiverCompany: String?,
        val country: String,
        val addressLine: String,
        val receiverProvince: String?,
        val receiverCity: String?,
        val postalCode: String?,
    )

    private fun resolveShipping(req: CreateOrderRequest, userId: Long): ResolvedShipping {
        if (req.savedAddressId != null) {
            val addr = userAddressRepository.findActiveByIdAndUserId(req.savedAddressId, userId)
                ?: throw BizException("address not found")
            return ResolvedShipping(
                receiverName = addr.receiverName,
                receiverPhone = addr.receiverPhone,
                receiverCompany = addr.receiverCompany?.takeUnless { it.isBlank() },
                country = addr.country,
                addressLine = addr.addressLine,
                receiverProvince = addr.province?.takeUnless { it.isBlank() },
                receiverCity = addr.city?.takeUnless { it.isBlank() },
                postalCode = addr.postalCode?.takeUnless { it.isBlank() },
            )
        }
        val rn = req.receiverName?.trim().orEmpty()
        val rp = req.receiverPhone?.trim().orEmpty()
        val c = req.country?.trim().orEmpty()
        val al = req.addressLine?.trim().orEmpty()
        if (rn.isEmpty() || rp.isEmpty() || c.isEmpty() || al.isEmpty()) {
            throw BizException("receiver, phone, country and address are required")
        }
        return ResolvedShipping(
            receiverName = rn,
            receiverPhone = rp,
            receiverCompany = req.receiverCompany?.trim()?.takeUnless { it.isEmpty() },
            country = c,
            addressLine = al,
            receiverProvince = req.receiverProvince?.trim()?.takeUnless { it.isEmpty() },
            receiverCity = req.receiverCity?.trim()?.takeUnless { it.isEmpty() },
            postalCode = req.postalCode?.trim(),
        )
    }

    private fun persistLogisticsLine(orderNo: String, trackingNo: String, carrier: String?, remark: String?) {
        orderLogisticsRepository.save(
            OrderLogisticsEntity(
                orderNo = orderNo,
                trackingNo = trackingNo,
                carrier = carrier,
                remark = remark,
                createdAt = OffsetDateTime.now(),
                createdBy = runCatching { CurrentUser.userId() }.getOrNull(),
            ),
        )
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

    private fun keywordOrderSpec(q: String?): Specification<OrderEntity>? {
        if (q.isNullOrBlank()) return null
        val term = q.trim()
        val like = "%${term.lowercase()}%"
        return Specification { root, _, cb ->
            cb.or(
                cb.like(cb.lower(root.get("orderNo")), like),
                cb.like(cb.lower(root.get("receiverName")), like),
            )
        }
    }

    private fun phaseOrderSpec(phase: String?): Specification<OrderEntity>? {
        val p = phase?.trim()?.uppercase()?.takeUnless { it.isBlank() || it == "ALL" } ?: return null
        return when (p) {
            "PENDING_PAYMENT" -> orderStatusEquals("PENDING_PAYMENT")
            "PAID" -> orderStatusEquals("PAID")
            "SHIPPED" -> orderStatusEquals("SHIPPED")
            "DELIVERED" -> orderStatusEquals("DELIVERED")
            "COMPLETED" -> orderStatusEquals("COMPLETED")
            "FULFILLING" -> orderStatusIn(listOf("PAID", "SHIPPED"))
            "DONE" -> orderStatusIn(listOf("DELIVERED", "COMPLETED"))
            "CANCELLED" -> orderStatusEquals("CANCELLED")
            else -> null
        }
    }

    private fun orderStatusEquals(status: String): Specification<OrderEntity> =
        Specification { root, _, cb -> cb.equal(root.get<String>("status"), status) }

    private fun orderStatusIn(statuses: List<String>): Specification<OrderEntity> =
        Specification { root, _, cb -> root.get<String>("status").`in`(statuses) }

    private fun clampOrderPage(page: Int): Int = page.coerceAtLeast(0)

    private fun clampOrderSize(size: Int): Int = size.coerceIn(1, 100)

    private fun onBecamePaid(order: OrderEntity, previousStatus: String) {
        if (order.status != "PAID" || previousStatus == "PAID") return
        order.paymentStatus = "PAID"
        membershipService.accumulateAfterOrderPaid(order.userId, order.totalAmount)
    }

    private fun toView(order: OrderEntity): OrderView {
        val itemRows = orderItemRepository.findAllByOrderId(order.id!!)
        val pidSet = itemRows.map { it.productId }.toSet()
        val pubByInternal = productRepository.findAllById(pidSet).associate { it.id!! to it.publicId }
        val items = itemRows.map {
            OrderItemView(
                productId = pubByInternal[it.productId] ?: it.productId.toString(),
                titleSnapshot = it.titleSnapshot,
                priceSnapshot = it.priceSnapshot.toPlainString(),
                quantity = it.quantity,
                thumbUrl = it.thumbUrl,
            )
        }
        val logisticsRows = orderLogisticsRepository.findAllByOrderNoOrderByCreatedAtDesc(order.orderNo).map { row ->
            OrderLogisticsView(
                id = row.id!!,
                orderNo = row.orderNo,
                carrier = row.carrier,
                trackingNo = row.trackingNo,
                remark = row.remark,
                createdAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(row.createdAt),
            )
        }
        return OrderView(
            orderNo = order.orderNo,
            userId = order.userId,
            status = order.status,
            paymentStatus = order.paymentStatus,
            totalAmount = order.totalAmount.toPlainString(),
            subtotalAmount = order.subtotalAmount.toPlainString(),
            discountMember = order.discountMember.toPlainString(),
            discountPromo = order.discountPromo.toPlainString(),
            discountCoupon = order.discountCoupon.toPlainString(),
            shippingFee = order.shippingFee.toPlainString(),
            couponCodeSnapshot = order.couponCodeSnapshot,
            paypalOrderId = order.paypalOrderId,
            currency = order.currency,
            receiverName = order.receiverName,
            receiverPhone = order.receiverPhone,
            receiverCompany = order.receiverCompany,
            taxNo = order.taxNo,
            country = order.country,
            addressLine = order.addressLine,
            receiverProvince = order.receiverProvince,
            receiverCity = order.receiverCity,
            postalCode = order.postalCode,
            incoterm = order.incoterm,
            shippingMethod = order.shippingMethod,
            logisticsCompany = order.logisticsCompany,
            trackingNo = order.trackingNo,
            items = items,
            createdAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(order.createdAt),
            logistics = logisticsRows,
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
