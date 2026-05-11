package com.trioForce.voyage.auth

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
 * 用户自助注册请求体。
 *
 * **验证码**：`captchaId` / `captchaCode` 须与 [CaptchaResponse] 及用户所见图片一致；
 * 服务端一次性校验见 [CaptchaService.validateAndConsume]（通过后该 `captchaId` 作废）。
 */
data class RegisterRequest(
    @field:Email @field:NotBlank val email: String,
    @field:NotBlank @field:Size(min = 6, max = 64) val password: String,
    @field:NotBlank val name: String,
    val phone: String? = null,
    val country: String? = null,
    /** 与 `GET /api/v1/auth/captcha` 返回的 `captchaId` 完全一致。 */
    @field:NotBlank val captchaId: String,
    /** 用户辨认的图片字符；与服务端比对时不区分大小写，**禁止**写入日志。 */
    @field:NotBlank val captchaCode: String,
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
data class MeResponse(val id: Long, val email: String, val name: String, val role: String)
