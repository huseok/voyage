package com.trioForce.voyage.aftersale

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 售后控制器：
 * 用户可提交/查看自己的工单，后台可查看全部并更新状态。
 */
@RestController
@Tag(name = "After-sales", description = "售后工单")
class AfterSaleController(
    private val afterSaleService: AfterSaleService
) {
    /**
     * 用户提交售后申请。
     *
     * @param req 售后请求体
     * @return 提交结果
     */
    @PostMapping("/api/v1/after-sales")
    fun create(@Valid @RequestBody req: CreateAfterSaleRequest): ApiResponse<String> {
        afterSaleService.create(req)
        return ok("created")
    }

    /**
     * 查询当前用户售后列表。
     *
     * @return 当前用户售后工单列表
     */
    @GetMapping("/api/v1/after-sales")
    fun listMine(): ApiResponse<List<AfterSaleView>> = ok(afterSaleService.listMine())

    /**
     * 后台查询全部售后。
     *
     * @return 全部售后工单
     */
    @GetMapping("/api/v1/admin/after-sales")
    fun listAll(): ApiResponse<List<AfterSaleView>> = ok(afterSaleService.listAll())

    /**
     * 后台更新售后状态。
     *
     * @param id 工单 ID
     * @param req 状态更新请求体
     * @return 更新结果
     */
    @PatchMapping("/api/v1/admin/after-sales/{id}/status")
    fun updateStatus(@PathVariable id: Long, @Valid @RequestBody req: UpdateAfterSaleStatusRequest): ApiResponse<String> {
        afterSaleService.updateStatus(id, req.status)
        return ok("updated")
    }
}
