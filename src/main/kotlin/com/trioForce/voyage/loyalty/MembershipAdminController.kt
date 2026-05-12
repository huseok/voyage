package com.trioForce.voyage.loyalty

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

@RestController
class MembershipAdminController(
    private val membershipAdminService: MembershipAdminService,
) {
    @GetMapping("/api/v1/admin/membership/tier-rules")
    fun listTierRules(): ApiResponse<List<MembershipTierRuleAdminView>> =
        ok(membershipAdminService.listTierRules())

    @PostMapping("/api/v1/admin/membership/tier-rules")
    fun createTierRule(@Valid @RequestBody req: MembershipTierRuleUpsertRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to membershipAdminService.createTierRule(req)))

    @PutMapping("/api/v1/admin/membership/tier-rules/{id}")
    fun updateTierRule(
        @PathVariable id: Long,
        @Valid @RequestBody req: MembershipTierRuleUpsertRequest,
    ): ApiResponse<String> {
        membershipAdminService.updateTierRule(id, req)
        return ok("updated")
    }

    @PatchMapping("/api/v1/admin/membership/tier-rules/{id}/active")
    fun patchTierRuleActive(
        @PathVariable id: Long,
        @Valid @RequestBody req: MembershipTierRuleActivePatchRequest,
    ): ApiResponse<String> {
        membershipAdminService.patchTierRuleActive(id, req)
        return ok("updated")
    }
}
