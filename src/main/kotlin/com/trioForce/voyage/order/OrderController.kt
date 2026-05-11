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
     * 后台分页查询订单（跨用户）。
     *
     * @param phase 阶段：`ALL` 或不传=全部；`PENDING_PAYMENT`/`PAID`/`SHIPPED`/`DELIVERED`/`COMPLETED`/`CANCELLED`；
     *   `FULFILLING`=待发货+配送中；`DONE`=已送达或已完成。
     * @param status 若传则按精确状态覆盖 phase。
     */
    @GetMapping("/api/v1/admin/orders")
    fun listAllForAdmin(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) phase: String?,
    ): ApiResponse<PagedOrders> = ok(orderService.listAdminPage(page, size, q, status, phase))

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
        orderService.adminUpdateStatus(orderNo, req.status, req.remark)
        return ok("status updated")
    }

    /**
     * 后台按字典顺序自动流转到下一状态。
     */
    @PatchMapping("/api/v1/admin/orders/{orderNo}/status/flow-next")
    fun flowNextStatus(@PathVariable orderNo: String, @RequestBody req: FlowNextOrderStatusRequest): ApiResponse<String> {
        orderService.adminFlowNextStatus(orderNo, req.remark)
        return ok("status flow-next updated")
    }
}
