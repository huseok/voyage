package com.trioForce.voyage.stats

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class StatsAdminController(
    private val statsAdminService: StatsAdminService,
) {
    @GetMapping("/api/v1/admin/stats/summary")
    fun summary(): ApiResponse<AdminStatsSummaryView> = ok(statsAdminService.summary())
}
