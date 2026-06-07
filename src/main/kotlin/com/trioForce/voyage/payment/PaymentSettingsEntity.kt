package com.trioForce.voyage.payment

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.OffsetDateTime

/** 支付全局配置（单行 id=1） */
@Entity
@Table(name = "t_payment_settings")
class PaymentSettingsEntity(
    @Id
    var id: Long = 1L,

    @Column(name = "provider", nullable = false, length = 32)
    var provider: String = "paypal",

    @Column(name = "enabled", nullable = false)
    var enabled: Boolean = false,

    @Column(name = "sandbox", nullable = false)
    var sandbox: Boolean = true,

    @Column(name = "client_id", length = 256)
    var clientId: String? = null,

    @Column(name = "client_secret", length = 512)
    var clientSecret: String? = null,

    @Column(name = "webhook_id", length = 128)
    var webhookId: String? = null,

    @Column(name = "brand_name", nullable = false, length = 128)
    var brandName: String = "Globuy",

    /** 支付完成回跳前台根地址，如 https://shop.example.com；空则用 CORS 首域名 */
    @Column(name = "return_url_base", length = 512)
    var returnUrlBase: String? = null,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)
