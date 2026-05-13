package com.trioForce.voyage.storefront

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 前台展示相关配置（与 CMS / 商品等业务模块分离）。
 */
@ConfigurationProperties(prefix = "voyage.storefront")
data class StorefrontProperties(
    /** 首页「活动商品」轮播绑定的标签编码；须与标签管理中的 code 一致 */
    var homePromoZoneTagCode: String = "HOME_PROMO",
    /** 首页「精选」区块；空则回退为无标签分页 */
    var homeFeaturedTagCode: String = "",
    /** 首页「热销」区块（暂由标签驱动）；空则回退 promo 列表 */
    var homeHotTagCode: String = "",
)
