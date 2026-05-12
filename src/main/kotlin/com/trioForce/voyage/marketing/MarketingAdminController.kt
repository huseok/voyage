package com.trioForce.voyage.marketing

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
class MarketingAdminController(
    private val marketingAdminService: MarketingAdminService,
) {
    @GetMapping("/api/v1/admin/coupons")
    fun listCoupons(): ApiResponse<List<CouponAdminView>> =
        ok(marketingAdminService.listCoupons())

    @PostMapping("/api/v1/admin/coupons")
    fun createCoupon(@Valid @RequestBody req: CouponAdminUpsertRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to marketingAdminService.createCoupon(req)))

    @PutMapping("/api/v1/admin/coupons/{id}")
    fun updateCoupon(
        @PathVariable id: Long,
        @Valid @RequestBody req: CouponAdminUpsertRequest,
    ): ApiResponse<String> {
        marketingAdminService.updateCoupon(id, req)
        return ok("updated")
    }

    @PatchMapping("/api/v1/admin/coupons/{id}/active")
    fun patchCouponActive(
        @PathVariable id: Long,
        @Valid @RequestBody req: CouponActivePatchRequest,
    ): ApiResponse<String> {
        marketingAdminService.patchCouponActive(id, req)
        return ok("updated")
    }

    @GetMapping("/api/v1/admin/promotions")
    fun listPromotions(): ApiResponse<List<PromotionAdminView>> =
        ok(marketingAdminService.listPromotions())

    @PostMapping("/api/v1/admin/promotions")
    fun createPromotion(@Valid @RequestBody req: PromotionAdminUpsertRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to marketingAdminService.createPromotion(req)))

    @PutMapping("/api/v1/admin/promotions/{id}")
    fun updatePromotion(
        @PathVariable id: Long,
        @Valid @RequestBody req: PromotionAdminUpsertRequest,
    ): ApiResponse<String> {
        marketingAdminService.updatePromotion(id, req)
        return ok("updated")
    }

    @PatchMapping("/api/v1/admin/promotions/{id}/active")
    fun patchPromotionActive(
        @PathVariable id: Long,
        @Valid @RequestBody req: PromotionActivePatchRequest,
    ): ApiResponse<String> {
        marketingAdminService.patchPromotionActive(id, req)
        return ok("updated")
    }
}
