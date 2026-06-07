package com.trioForce.voyage.payment

import org.springframework.boot.context.properties.ConfigurationProperties

/** 环境变量可覆盖库内 PayPal 凭据（与翻译配置模式一致） */
@ConfigurationProperties(prefix = "voyage.payment.paypal")
data class PaymentProperties(
    var clientId: String = "",
    var clientSecret: String = "",
    var webhookId: String = "",
)
