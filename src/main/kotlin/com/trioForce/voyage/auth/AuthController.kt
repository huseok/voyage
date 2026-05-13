package com.trioForce.voyage.auth

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import com.trioForce.voyage.common.logging.LogUtil
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*

/**
 * 认证接口控制器：
 * 暴露注册、登录、改密和当前用户信息接口。
 *
 * **说明**：图形验证码的签发与校验日志见 [CaptchaService]、[AuthService]；本控制器保持瘦逻辑，仅在必要时打访问级日志。
 */
@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "注册、登录与当前用户")
class AuthController(
    private val authService: AuthService,
    private val captchaService: CaptchaService,
) {
    private val log = LogUtil.logger<AuthController>()

    /**
     * 获取注册用图形验证码。
     *
     * 返回体中的 [CaptchaResponse.imageBase64] 为完整 data URI，可直接赋给前端 `<img src>`；
     * [CaptchaResponse.captchaId] 须在提交注册时原样回传。具体生成与内存策略见 [CaptchaService.create]。
     */
    @Operation(summary = "获取注册图形验证码")
    @GetMapping("/captcha")
    fun captcha(): ApiResponse<CaptchaResponse> {
        log.debug("HTTP GET /api/v1/auth/captcha")
        return ok(captchaService.create())
    }

    /**
     * 用户注册接口。
     *
     * @param req 注册参数（邮箱、密码、姓名等）
     * @return 统一响应，成功返回 registered
     */
    @PostMapping("/register")
    fun register(@Valid @RequestBody req: RegisterRequest): ApiResponse<String> {
        authService.register(req)
        return ok("registered")
    }

    /**
     * 用户登录接口。
     *
     * @param req 登录参数（邮箱、密码）
     * @return 登录成功后 JWT 令牌
     */
    @PostMapping("/login")
    fun login(@Valid @RequestBody req: LoginRequest): ApiResponse<LoginResponse> = ok(authService.login(req))

    /**
     * 用 refresh token 换取新的 access + refresh（旋转 refresh）。
     */
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody req: RefreshTokenRequest): ApiResponse<LoginResponse> = ok(authService.refresh(req))

    /**
     * 修改密码接口。
     *
     * @param req 修改密码参数（旧密码、新密码）
     * @return 修改结果
     */
    @PostMapping("/change-password")
    fun changePassword(@Valid @RequestBody req: ChangePasswordRequest): ApiResponse<String> {
        authService.changePassword(req)
        return ok("password updated")
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 当前用户信息
     */
    @GetMapping("/me")
    fun me(): ApiResponse<MeResponse> = ok(authService.me())
}
