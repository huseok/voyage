package com.trioForce.voyage.auth

import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
import com.trioForce.voyage.security.CurrentUser
import com.trioForce.voyage.security.JwtService
import com.trioForce.voyage.user.UserEntity
import com.trioForce.voyage.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.http.HttpStatus
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.time.OffsetDateTime

/**
 * 认证核心服务：
 * 负责注册、登录、改密、当前用户信息查询。
 *
 * **日志**：注册失败场景记录 WARN/DEBUG 级摘要，**不**输出密码与验证码明文。
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    private val captchaService: CaptchaService,
) {
    private val log = LogUtil.logger<AuthService>()

    /**
     * 创建新用户账号。
     *
     * @param req 注册请求
     */
    @Transactional
    fun register(req: RegisterRequest) {
        if (!captchaService.validateAndConsume(req.captchaId, req.captchaCode)) {
            // 详细原因已在 CaptchaService 中以 DEBUG 打出；此处仅业务层告警，便于聚合监控「注册被验证码拦住」的次数。
            log.warn(
                "用户注册被拒绝：图形验证码无效、过期或已使用（不记录邮箱与验证码明文） captchaIdLen={}",
                req.captchaId.trim().length,
            )
            throw BizException("invalid captcha")
        }
        if (userRepository.findByEmail(req.email).isPresent) throw BizException("email already exists")
        userRepository.save(
            UserEntity(
                email = req.email.trim().lowercase(),
                passwordHash = passwordEncoder.encode(req.password),
                name = req.name.trim(),
                salutation = req.salutation.trim(),
                phone = req.phone?.trim(),
                country = req.country?.trim()
            )
        )
        // 成功路径仅记一条 INFO，避免输出完整邮箱（如需审计请依赖数据库或专用审计表）。
        log.info("用户注册成功")
    }

    /**
     * 校验账号密码并签发 JWT。
     *
     * @param req 登录请求
     * @return JWT 令牌
     */
    fun login(req: LoginRequest): LoginResponse {
        val user = userRepository.findByEmail(req.email.trim().lowercase()).orElseThrow { BizException("invalid email or password") }
        if (!passwordEncoder.matches(req.password, user.passwordHash)) throw BizException("invalid email or password")
        if (user.status != "ACTIVE") throw BizException("user disabled")
        val access = jwtService.generateAccessToken(user.id!!, user.email, user.role)
        val refresh = jwtService.generateRefreshToken(user.id!!)
        return LoginResponse(
            accessToken = access,
            refreshToken = refresh,
            expiresIn = jwtService.accessTokenTtlSeconds()
        )
    }

    /**
     * 用 refresh token 轮换签发新的 access + refresh（refresh 旋转）。
     * 失败时返回 401，由前端清空态并引导重新登录。
     */
    fun refresh(req: RefreshTokenRequest): LoginResponse {
        val claims =
            try {
                jwtService.parseRefreshTokenClaims(req.refreshToken.trim())
            } catch (_: Exception) {
                throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token")
            }
        val userId =
            claims.subject.toLongOrNull()
                ?: throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token")
        val user = userRepository.findById(userId).orElseThrow { ResponseStatusException(HttpStatus.UNAUTHORIZED, "invalid refresh token") }
        if (user.status != "ACTIVE") throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "user disabled")
        val access = jwtService.generateAccessToken(user.id!!, user.email, user.role)
        val refresh = jwtService.generateRefreshToken(user.id!!)
        return LoginResponse(
            accessToken = access,
            refreshToken = refresh,
            expiresIn = jwtService.accessTokenTtlSeconds()
        )
    }

    /**
     * 修改当前登录用户密码。
     *
     * @param req 旧密码与新密码
     */
    @Transactional
    fun changePassword(req: ChangePasswordRequest) {
        val user = userRepository.findById(CurrentUser.userId()).orElseThrow { BizException("user not found") }
        if (!passwordEncoder.matches(req.oldPassword, user.passwordHash)) throw BizException("old password not correct")
        user.passwordHash = passwordEncoder.encode(req.newPassword)
        user.updatedAt = OffsetDateTime.now()
        userRepository.save(user)
    }

    /**
     * 获取当前登录用户信息。
     *
     * @return 用户基础信息
     */
    fun me(): MeResponse {
        val user = userRepository.findById(CurrentUser.userId()).orElseThrow { BizException("user not found") }
        return MeResponse(user.id!!, user.email, user.name, user.salutation, user.role)
    }
}
