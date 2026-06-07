package com.trioForce.voyage.payment

import jakarta.validation.constraints.NotBlank

data class PaymentSettingsView(
    val provider: String,
    val enabled: Boolean,
    val sandbox: Boolean,
    val brandName: String,
    val returnUrlBase: String?,
    val clientIdConfigured: Boolean,
    val clientIdMask: String?,
    val clientSecretConfigured: Boolean,
    val clientSecretMask: String?,
    val webhookIdConfigured: Boolean,
    val envCredentialsActive: Boolean,
    val updatedAt: String,
)

data class PaymentSettingsUpdateRequest(
    val enabled: Boolean,
    val sandbox: Boolean,
    @field:NotBlank val brandName: String,
    val returnUrlBase: String?,
    val clientId: String?,
    val clientSecret: String?,
    val webhookId: String?,
    val clearClientSecret: Boolean? = false,
)

/** 前台加载 PayPal JS / 按钮用（不含密钥） */
data class PaymentPublicConfigView(
    val provider: String,
    val enabled: Boolean,
    val sandbox: Boolean,
    val clientId: String?,
    val brandName: String,
)

data class PayPalCreateResponse(
    val paypalOrderId: String,
    val approveUrl: String,
)

data class PayPalCaptureRequest(
    @field:NotBlank val paypalOrderId: String,
)

data class PayPalCaptureResponse(
    val orderNo: String,
    val paymentStatus: String,
    val status: String,
)
