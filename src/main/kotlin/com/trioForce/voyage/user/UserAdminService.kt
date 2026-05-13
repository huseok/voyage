package com.trioForce.voyage.user

import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
import com.trioForce.voyage.loyalty.MembershipService
import com.trioForce.voyage.loyalty.UserMembershipRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.SecureRandom
import java.time.OffsetDateTime

/**
 * 后台「客户」领域服务：分页查询注册用户、维护运营备注/偏好、重置登录密码。
 *
 * - 列表数据会 **LEFT** 关联会员累计表：无记录时档位视为 `NONE`、累计金额为 `0`。
 * - 重置密码返回 **明文临时密码** 仅供管理员线下告知客户；**日志中绝不打印该明文**（见 [resetPasswordReturnPlain]）。
 */
@Service
class UserAdminService(
    private val userRepository: UserRepository,
    private val membershipRepository: UserMembershipRepository,
    private val membershipService: MembershipService,
    private val passwordEncoder: PasswordEncoder,
) {
    private val log = LogUtil.logger<UserAdminService>()

    /**
     * 分页查询可运营查看的客户列表，并按用户 id 批量拉取会员档位行以组装 [CustomerAdminView]。
     *
     * @param page 页码，从 0 起；小于 0 时按 0 处理。
     * @param size 每页条数，限制在 1～100，防止单次拉取过大。
     * @param q 可选关键字：对邮箱、姓名做 **大小写不敏感** 模糊匹配；空或纯空白时不加筛选条件。
     */
    fun listCustomers(page: Int, size: Int, q: String?): PagedCustomers {
        val p = page.coerceAtLeast(0)
        val s = size.coerceIn(1, 100)
        // 按主键倒序：新注册用户靠前，便于运营处理近期线索。
        val pageable = PageRequest.of(p, s, Sort.by(Sort.Direction.DESC, "id"))
        val spec = adminSearchSpec(q)
        val qPresent = !q.isNullOrBlank()
        val pg = if (spec != null) userRepository.findAll(spec, pageable) else userRepository.findAll(pageable)
        val ids = pg.content.mapNotNull { it.id }
        // 会员表主键即 userId，此处一次性查出当前页涉及用户，避免 N+1。
        val memByUser = membershipRepository.findAllById(ids).associateBy { it.userId }
        val items = pg.content.map { u ->
            val id = u.id!!
            val m = memByUser[id]
            val tier = m?.tier ?: "NONE"
            CustomerAdminView(
                id = id,
                email = u.email,
                name = u.name,
                salutation = u.salutation,
                phone = u.phone,
                country = u.country,
                role = u.role,
                status = u.status,
                createdAt = u.createdAt,
                tier = tier,
                lifetimePaidUsd = m?.lifetimePaidUsd?.stripTrailingZeros()?.toPlainString() ?: "0",
                memberDiscountPercent = membershipService.discountPercent(tier),
                adminNote = u.adminNote,
                preferences = u.preferences,
            )
        }
        log.debug(
            "管理员查询客户列表：page={} size={} 含关键字筛选={} 本页条数={} 命中总条数={}",
            p,
            s,
            qPresent,
            items.size,
            pg.totalElements,
        )
        return PagedCustomers(
            items = items,
            total = pg.totalElements,
            page = p,
            size = s,
        )
    }

    /**
     * 按 id 更新 [UserEntity.adminNote] / [UserEntity.preferences]。
     *
     * 请求体字段为 **null** 表示「本字段不修改」；非 null 时 trim 后写入，空串会存为 `null`（清空）。
     */
    @Transactional
    fun updateCustomer(id: Long, req: CustomerAdminUpdateRequest) {
        val user = userRepository.findById(id).orElseThrow { BizException("user not found") }
        val touchNote = req.adminNote != null
        val touchPref = req.preferences != null
        if (req.adminNote != null) {
            user.adminNote = req.adminNote.trim().takeUnless { it.isEmpty() }
        }
        if (req.preferences != null) {
            user.preferences = req.preferences.trim().takeUnless { it.isEmpty() }
        }
        user.updatedAt = OffsetDateTime.now()
        userRepository.save(user)
        log.info("管理员更新客户备注/偏好：userId={} 更新备注字段={} 更新偏好字段={}", id, touchNote, touchPref)
    }

    /**
     * 生成随机临时明文密码、写入 bcrypt 哈希并持久化；返回值 **仅** 用于接口响应给前端展示一次。
     *
     * **安全**：禁止在日志中输出临时密码；如需审计请依赖独立审计表或脱敏流水。
     */
    @Transactional
    fun resetPasswordReturnPlain(id: Long): String {
        val user = userRepository.findById(id).orElseThrow { BizException("user not found") }
        val plain = randomTempPassword()
        user.passwordHash = passwordEncoder.encode(plain)
        user.updatedAt = OffsetDateTime.now()
        userRepository.save(user)
        log.warn("管理员已重置客户登录密码：userId={}（临时密码不在日志中记录）", id)
        return plain
    }

    /**
     * 构造 JPA [Specification]：对邮箱、姓名做 OR 模糊匹配（小写化后 like）。
     * 若关键字无效则返回 null，由调用方走无 spec 的全表分页（仍受 page/size 限制）。
     */
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

    /** 生成易读、去混淆的临时密码（排除易混字符 0/O、1/l/I 等）。 */
    private fun randomTempPassword(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789"
        val r = SecureRandom()
        return (1..12).map { chars[r.nextInt(chars.length)] }.joinToString("")
    }
}
