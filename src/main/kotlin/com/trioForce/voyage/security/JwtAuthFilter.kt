package com.trioForce.voyage.security

import com.trioForce.voyage.user.UserRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthFilter(
    private val jwtService: JwtService,
    private val userRepository: UserRepository
) : OncePerRequestFilter() {
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        val authHeader = request.getHeader("Authorization")
        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response)
            return
        }

        val token = authHeader.substring(7)
        val claims = runCatching { jwtService.parseClaims(token) }.getOrNull()
        if (claims == null) {
            filterChain.doFilter(request, response)
            return
        }

        // JWT 中只存最少字段，用户完整信息始终以数据库为准
        val userId = (claims["uid"] as Number).toLong()
        val user = userRepository.findById(userId).orElse(null)
        if (user == null) {
            filterChain.doFilter(request, response)
            return
        }

        val principal = AppUserPrincipal(
            userId = user.id!!,
            email = user.email,
            passwordHash = user.passwordHash,
            role = user.role
        )

        val auth = UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
        auth.details = WebAuthenticationDetailsSource().buildDetails(request)
        SecurityContextHolder.getContext().authentication = auth
        filterChain.doFilter(request, response)
    }
}
