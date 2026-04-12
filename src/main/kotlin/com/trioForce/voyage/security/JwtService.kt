package com.trioForce.voyage.security

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.io.Decoders
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.Date
import javax.crypto.SecretKey

@Service
class JwtService(
    @Value("\${app.jwt.secret}") private val secret: String,
    @Value("\${app.jwt.access-token-minutes}") private val accessTokenMinutes: Long,
    @Value("\${app.jwt.refresh-token-days}") private val refreshTokenDays: Long
) {
    private fun key(): SecretKey {
        val normalized = if (secret.length < 32) secret.padEnd(32, 'x') else secret
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(normalized.toByteArray())))
    }

    /** Access token：带 `uid`、`role`，用于 API Bearer。 */
    fun generateAccessToken(userId: Long, email: String, role: String): String {
        val now = Date()
        val exp = Date(now.time + accessTokenMinutes * 60_000)
        return Jwts.builder()
            .subject(email)
            .claim("uid", userId)
            .claim("role", role)
            .claim("typ", "access")
            .issuedAt(now)
            .expiration(exp)
            .signWith(key())
            .compact()
    }

    /** Refresh token：仅 `typ=refresh` + subject=userId，长期有效；每次 refresh 轮换。 */
    fun generateRefreshToken(userId: Long): String {
        val now = Date()
        val exp = Date(now.time + refreshTokenDays * 24 * 60 * 60 * 1000)
        return Jwts.builder()
            .subject(userId.toString())
            .claim("typ", "refresh")
            .issuedAt(now)
            .expiration(exp)
            .signWith(key())
            .compact()
    }

    fun accessTokenTtlSeconds(): Long = accessTokenMinutes * 60

    /**
     * 解析并校验 **access** JWT；过期、签名错误、或非 access 类型返回 null。
     */
    fun parseAccessTokenClaims(token: String): Claims? =
        runCatching {
            val c = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).payload
            val typ = c["typ"]?.toString()
            if (typ == "refresh") return@runCatching null
            if (typ != null && typ != "access") return@runCatching null
            if (c["uid"] == null) return@runCatching null
            c
        }.getOrNull()

    /** 解析 **refresh** JWT；非法类型或签名失败时抛 Jwt 异常。 */
    fun parseRefreshTokenClaims(token: String): Claims {
        val c = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).payload
        require(c["typ"]?.toString() == "refresh") { "not a refresh token" }
        require(c.subject.isNotBlank()) { "invalid refresh subject" }
        return c
    }

}
