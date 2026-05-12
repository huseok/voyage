package com.trioForce.voyage.user

import com.trioForce.voyage.loyalty.MembershipService
import com.trioForce.voyage.loyalty.UserMembershipRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service

@Service
class UserAdminService(
    private val userRepository: UserRepository,
    private val membershipRepository: UserMembershipRepository,
    private val membershipService: MembershipService,
) {
    fun listCustomers(page: Int, size: Int, q: String?): PagedCustomers {
        val p = page.coerceAtLeast(0)
        val s = size.coerceIn(1, 100)
        val pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "id"))
        val spec = adminSearchSpec(q)
        val pg = if (spec != null) userRepository.findAll(spec, pageable) else userRepository.findAll(pageable)
        val ids = pg.content.mapNotNull { it.id }
        val memByUser = membershipRepository.findAllById(ids).associateBy { it.userId }
        val items = pg.content.map { u ->
            val id = u.id!!
            val m = memByUser[id]
            val tier = m?.tier ?: "NONE"
            CustomerAdminView(
                id = id,
                email = u.email,
                name = u.name,
                phone = u.phone,
                country = u.country,
                role = u.role,
                status = u.status,
                createdAt = u.createdAt,
                tier = tier,
                lifetimePaidUsd = m?.lifetimePaidUsd?.stripTrailingZeros()?.toPlainString() ?: "0",
                memberDiscountPercent = membershipService.discountPercent(tier),
            )
        }
        return PagedCustomers(
            items = items,
            total = pg.totalElements,
            page = p,
            size = s,
        )
    }

    private fun adminSearchSpec(q: String?): Specification<UserEntity>? {
        val raw = q?.trim() ?: return null
        if (raw.isEmpty()) return null
        val like = "%${raw.lowercase()}%"
        return Specification { root, _, cb ->
            val email = cb.like(cb.lower(root.get("email")), like)
            val name = cb.like(cb.lower(root.get("name")), like)
            cb.or(email, name)
        }
    }
}
