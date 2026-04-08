package com.trioForce.voyage.security

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

class AppUserPrincipal(
    val userId: Long,
    private val email: String,
    private val passwordHash: String,
    private val role: String
) : UserDetails {
    override fun getAuthorities(): MutableCollection<out GrantedAuthority> = mutableListOf(SimpleGrantedAuthority("ROLE_$role"))
    override fun getPassword(): String = passwordHash
    override fun getUsername(): String = email
}
