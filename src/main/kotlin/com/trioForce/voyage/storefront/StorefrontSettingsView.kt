package com.trioForce.voyage.storefront

/** [StorefrontSettingsController] 返回给前台的只读配置 */
data class StorefrontSettingsView(
    val homePromoZoneTagCode: String,
    val homeFeaturedTagCode: String,
    val homeHotTagCode: String,
)
