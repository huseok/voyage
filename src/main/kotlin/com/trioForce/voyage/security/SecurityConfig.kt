package com.trioForce.voyage.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthFilter: JwtAuthFilter,
    @Value("\${app.cors.allowed-origins}") private val allowedOrigins: String
) {
    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        return http
            .csrf { it.disable() }
            .cors {}
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests {
                // 登录注册、公开商品、文档接口允许匿名访问
                it.requestMatchers(
                    "/api/v1/auth/register",
                    "/api/v1/auth/login",
                    "/api/v1/auth/refresh",
                    "/api/v1/auth/captcha",
                    "/api/v1/legal/versions",
                ).permitAll()
                it.requestMatchers("/api/v1/products/**").permitAll()
                // 首页 / 目录 / 顶栏分类导航：仅列表查询，与 CategoryController GET /api/v1/categories 对应
                it.requestMatchers("/api/v1/categories").permitAll()
                it.requestMatchers("/api/v1/tags").permitAll()
                it.requestMatchers("/api/v1/site/contents", "/api/v1/site/promos").permitAll()
                it.requestMatchers("/api/v1/storefront/settings").permitAll()
                it.requestMatchers("/api/v1/exchange-rates").permitAll()
                it.requestMatchers("/api/v1/i18n/catalogs/**").permitAll()
                it.requestMatchers("/media/**").permitAll()
                it.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                // 后台接口必须 ADMIN 角色
                it.requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                it.anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter::class.java)
            .build()
    }

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(authenticationConfiguration: AuthenticationConfiguration): AuthenticationManager =
        authenticationConfiguration.authenticationManager

    /**
     * CORS 配置（配置化）：
     * - 开发环境放行 localhost 前端端口
     * - 生产环境仅放行正式前端域名
     */
    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOrigins = allowedOrigins.split(",").map { it.trim() }.filter { it.isNotBlank() }
        config.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = true

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
