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
    @Value("\${app.jwt.access-token-minutes}") private val accessTokenMinutes: Long
) {
    private fun key(): SecretKey {
        val normalized = if (secret.length < 32) secret.padEnd(32, 'x') else secret
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(java.util.Base64.getEncoder().encodeToString(normalized.toByteArray())))
    }

    fun generateToken(userId: Long, email: String, role: String): String {
        val now = Date()
        val exp = Date(now.time + accessTokenMinutes * 60_000)
        return Jwts.builder()
            .subject(email)
            .claim("uid", userId)
            .claim("role", role)
            .issuedAt(now)
            .expiration(exp)
            .signWith(key())
            .compact()
    }

    fun parseClaims(token: String): Claims = Jwts.parser().verifyWith(key()).build().parseSignedClaims(token).payload
}
