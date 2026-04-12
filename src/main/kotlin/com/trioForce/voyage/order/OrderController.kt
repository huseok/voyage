package com.trioForce.voyage.order

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 订单控制器：
 * 用户侧下单/查单/确认完成，后台侧更新物流和状态。
 */
@RestController
@Tag(name = "Orders", description = "下单、查单与后台订单处理")
class OrderController(
    private val orderService: OrderService
) {
    /**
     * 从当前用户购物车创建订单。
     *
     * @param req 收货与贸易信息
     * @return 生成的订单号
     */
    @PostMapping("/api/v1/orders")
    fun create(@Valid @RequestBody req: CreateOrderRequest): ApiResponse<Map<String, String>> =
        ok(mapOf("orderNo" to orderService.createOrder(req)))

    /**
     * 查询当前用户单个订单详情。
     *
     * @param orderNo 订单号
     * @return 订单详情
     */
    @GetMapping("/api/v1/orders/{orderNo}")
    fun detail(@PathVariable orderNo: String): ApiResponse<OrderView> = ok(orderService.getOrder(orderNo))

    /**
     * 查询当前用户订单列表。
     *
     * @return 订单列表
     */
    @GetMapping("/api/v1/orders")
    fun listMyOrders(): ApiResponse<List<OrderView>> = ok(orderService.listMyOrders())

    /**
     * 后台查询全部订单（跨用户）。
     *
     * 需 **ADMIN** 角色；用于管理端列表，按订单主键 id 倒序。
     *
     * @return 全部订单的视图列表
     */
    @GetMapping("/api/v1/admin/orders")
    fun listAllForAdmin(): ApiResponse<List<OrderView>> = ok(orderService.listAllForAdmin())

    /**
     * 用户确认完成订单（收货后）。
     *
     * @param orderNo 订单号
     * @return 更新结果
     */
    @PatchMapping("/api/v1/orders/{orderNo}/confirm-completed")
    fun confirmCompleted(@PathVariable orderNo: String): ApiResponse<String> {
        orderService.confirmCompleted(orderNo)
        return ok("completed")
    }

    /**
     * 后台更新订单物流信息。
     *
     * @param orderNo 订单号
     * @param req 运单号和物流公司
     * @return 更新结果
     */
    @PatchMapping("/api/v1/admin/orders/{orderNo}/tracking-no")
    fun updateTracking(@PathVariable orderNo: String, @Valid @RequestBody req: UpdateTrackingRequest): ApiResponse<String> {
        orderService.adminUpdateTracking(orderNo, req)
        return ok("tracking updated")
    }

    /**
     * 后台推进订单状态。
     *
     * @param orderNo 订单号
     * @param req 新状态
     * @return 更新结果
     */
    @PatchMapping("/api/v1/admin/orders/{orderNo}/status")
    fun updateStatus(@PathVariable orderNo: String, @Valid @RequestBody req: UpdateOrderStatusRequest): ApiResponse<String> {
        orderService.adminUpdateStatus(orderNo, req.status)
        return ok("status updated")
    }
}
