package com.trioForce.voyage.security

import com.trioForce.voyage.common.BizException
import org.springframework.security.core.context.SecurityContextHolder

object CurrentUser {
    fun userId(): Long {
        val principal = SecurityContextHolder.getContext().authentication?.principal as? AppUserPrincipal
        return principal?.userId ?: throw BizException("unauthorized")
    }
}
