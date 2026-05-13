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
     * @param userId 若传且为正数，仅返回该下单用户的订单。
     */
    @GetMapping("/api/v1/admin/orders")
    fun listAllForAdmin(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) q: String?,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) phase: String?,
        @RequestParam(required = false) userId: Long?,
    ): ApiResponse<PagedOrders> = ok(orderService.listAdminPage(page, size, q, status, phase, userId))

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
     * 后台追加一条物流轨迹（写入 [t_order_logistics]）。
     */
    @PostMapping("/api/v1/admin/orders/{orderNo}/logistics")
    fun appendLogistics(
        @PathVariable orderNo: String,
        @Valid @RequestBody req: OrderLogisticsCreateRequest,
    ): ApiResponse<String> {
        orderService.adminAppendLogistics(orderNo, req)
        return ok("logistics recorded")
    }

    /**
     * 后台推进订单状态；[UpdateOrderStatusRequest.forceRepair] 为 true 时允许回退，且必须填写备注。
     */
    @PatchMapping("/api/v1/admin/orders/{orderNo}/status")
    fun updateStatus(@PathVariable orderNo: String, @Valid @RequestBody req: UpdateOrderStatusRequest): ApiResponse<String> {
        orderService.adminUpdateStatus(orderNo, req.status, req.remark, req.forceRepair)
        return ok("status updated")
    }

    /**
     * 后台逻辑删除订单。
     */
    @DeleteMapping("/api/v1/admin/orders/{orderNo}")
    fun adminDelete(@PathVariable orderNo: String): ApiResponse<String> {
        orderService.adminLogicalDelete(orderNo)
        return ok("deleted")
    }
}
