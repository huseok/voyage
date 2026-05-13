package com.trioForce.voyage.storefront

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 前台只读配置（匿名可访问），与站点 CMS、商品接口拆分。
 */
@RestController
@Tag(name = "Storefront", description = "前台商城")
class StorefrontSettingsController(
    private val storefrontProperties: StorefrontProperties,
) {
    @GetMapping("/api/v1/storefront/settings")
    fun settings(): ApiResponse<StorefrontSettingsView> =
        ok(
            StorefrontSettingsView(
                homePromoZoneTagCode = storefrontProperties.homePromoZoneTagCode.trim().uppercase(),
                homeFeaturedTagCode = storefrontProperties.homeFeaturedTagCode.trim().uppercase(),
                homeHotTagCode = storefrontProperties.homeHotTagCode.trim().uppercase(),
            ),
        )
}
