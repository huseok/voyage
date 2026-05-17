package com.trioForce.voyage.auth

import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class CaptchaResponse(
    /** 会话标识：提交 [RegisterRequest] 时须与此处返回值完全一致（一次性消费，勿缓存跨页滥用）。 */
    val captchaId: String,
    /**
     * PNG 图片的 Base64 表示，且为 **完整 data URI**（形如 `data:image/png;base64,...`），
     * 前端可直接 `<img src={imageBase64} />`。服务端生成逻辑见 [CaptchaService.create]（避免重复拼接前缀导致图片无法显示）。
     */
    val imageBase64: String,
)

/**
 * 用户自助注册请求体（欧美式：名/姓分开，须同意当前版服务条款与隐私政策）。
 */
data class RegisterRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 6, max = 64) val password: String,
    @field:NotBlank @field:Size(max = 80) val firstName: String,
    @field:NotBlank @field:Size(max = 80) val lastName: String,
    /** 展示用全名；若为空则由服务端按 firstName + lastName 拼接 */
    val name: String? = null,
    /** 可选称谓，如 Mr、Ms */
    val salutation: String? = null,
    @field:Size(max = 200) val companyName: String? = null,
    val phone: String? = null,
    val country: String? = null,
    @field:NotBlank val captchaId: String,
    @field:NotBlank val captchaCode: String,
    @field:AssertTrue(message = "must accept terms of service")
    val acceptedTerms: Boolean = false,
    @field:AssertTrue(message = "must accept privacy policy")
    val acceptedPrivacy: Boolean = false,
    @field:NotBlank val termsVersion: String,
    @field:NotBlank val privacyVersion: String,
)

data class LoginRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank val password: String
)

data class ChangePasswordRequest(
    @field:NotBlank val oldPassword: String,
    @field:NotBlank @field:Size(min = 6, max = 64) val newPassword: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    /** Access token 剩余有效时间（秒），供前端调度刷新 */
    val expiresIn: Long
)

data class RefreshTokenRequest(
    @field:NotBlank val refreshToken: String
)

data class MeResponse(
    val id: Long,
    val email: String,
    val name: String,
    val firstName: String,
    val lastName: String,
    val salutation: String,
    val companyName: String?,
    val phone: String?,
    val country: String?,
    val role: String,
)
