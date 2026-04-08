package com.trioForce.voyage.auth

import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.security.CurrentUser
import com.trioForce.voyage.security.JwtService
import com.trioForce.voyage.user.UserEntity
import com.trioForce.voyage.user.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/**
 * 认证核心服务：
 * 负责注册、登录、改密、当前用户信息查询。
 */
@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService
) {
    /**
     * 创建新用户账号。
     *
     * @param req 注册请求
     */
    @Transactional
    fun register(req: RegisterRequest) {
        if (userRepository.findByEmail(req.email).isPresent) throw BizException("email already exists")
        userRepository.save(
            UserEntity(
                email = req.email.trim().lowercase(),
                passwordHash = passwordEncoder.encode(req.password),
                name = req.name.trim(),
                phone = req.phone?.trim(),
                country = req.country?.trim()
            )
        )
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
        val token = jwtService.generateToken(user.id!!, user.email, user.role)
        return LoginResponse(token)
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
        return MeResponse(user.id!!, user.email, user.name, user.role)
    }
}
