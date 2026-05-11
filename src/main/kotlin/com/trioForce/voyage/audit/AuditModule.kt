package com.trioForce.voyage.audit

/**
 * 审计模块（Audit）：
 * - t_audit_logs：记录关键操作
 * - t_order_status_histories：订单状态流转历史
 *
 * 目标：支持后台追责、运营复盘与合规审计。
 */

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import com.trioForce.voyage.order.OrderRepository
import jakarta.persistence.*
import org.hibernate.annotations.Where
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@Entity
@Table(name = "t_audit_logs")
@Where(clause = "is_deleted = false")
class AuditLogEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "actor_user_id")
    var actorUserId: Long? = null,
    @Column(name = "actor_role", length = 30)
    var actorRole: String? = null,
    @Column(name = "action_code", nullable = false, length = 80)
    var actionCode: String,
    @Column(name = "entity_type", nullable = false, length = 80)
    var entityType: String,
    @Column(name = "entity_id", nullable = false, length = 80)
    var entityId: String,
    @Column(name = "detail_json", columnDefinition = "text")
    var detailJson: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "created_by")
    var createdBy: Long? = null,
    @Column(name = "updated_by")
    var updatedBy: Long? = null,
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,
    @Column(name = "deleted_by")
    var deletedBy: Long? = null,
    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null
)

@Entity
@Table(name = "t_order_status_histories")
@Where(clause = "is_deleted = false")
class OrderStatusHistoryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "order_id", nullable = false)
    var orderId: Long,
    @Column(name = "from_status", length = 32)
    var fromStatus: String? = null,
    @Column(name = "to_status", nullable = false, length = 32)
    var toStatus: String,
    @Column(name = "changed_by")
    var changedBy: Long? = null,
    @Column(name = "changed_at", nullable = false)
    var changedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(length = 255)
    var remark: String? = null,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "created_by")
    var createdBy: Long? = null,
    @Column(name = "updated_by")
    var updatedBy: Long? = null,
    @Column(name = "is_deleted", nullable = false)
    var isDeleted: Boolean = false,
    @Column(name = "deleted_by")
    var deletedBy: Long? = null,
    @Column(name = "deleted_at")
    var deletedAt: OffsetDateTime? = null
)

interface AuditLogRepository : JpaRepository<AuditLogEntity, Long>
interface OrderStatusHistoryRepository : JpaRepository<OrderStatusHistoryEntity, Long> {
    fun findAllByOrderIdOrderByChangedAtAsc(orderId: Long): List<OrderStatusHistoryEntity>
}

data class AuditLogView(
    val id: Long,
    val actorUserId: Long?,
    val actorRole: String?,
    val actionCode: String,
    val entityType: String,
    val entityId: String,
    val detailJson: String?,
    val createdAt: OffsetDateTime
)

data class PagedAuditLogs(
    val items: List<AuditLogView>,
    val total: Long,
    val page: Int,
    val size: Int,
)

data class OrderStatusHistoryView(
    val id: Long,
    val orderId: Long,
    val fromStatus: String?,
    val toStatus: String,
    val changedBy: Long?,
    val changedAt: OffsetDateTime,
    val remark: String?
)

@Service
class AuditService(
    private val auditLogRepository: AuditLogRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val orderRepository: OrderRepository
) {
    fun listAuditLogs(): List<AuditLogView> = auditLogRepository.findAll().map {
        AuditLogView(it.id!!, it.actorUserId, it.actorRole, it.actionCode, it.entityType, it.entityId, it.detailJson, it.createdAt)
    }

    fun listAuditLogsPage(page: Int, size: Int): PagedAuditLogs {
        val pageable = PageRequest.of(page.coerceAtLeast(0), size.coerceIn(1, 100), Sort.by(Sort.Direction.DESC, "id"))
        val result = auditLogRepository.findAll(pageable)
        return PagedAuditLogs(
            items = result.content.map {
                AuditLogView(it.id!!, it.actorUserId, it.actorRole, it.actionCode, it.entityType, it.entityId, it.detailJson, it.createdAt)
            },
            total = result.totalElements,
            page = result.number,
            size = result.size,
        )
    }

    fun listOrderHistories(orderId: Long): List<OrderStatusHistoryView> =
        orderStatusHistoryRepository.findAllByOrderIdOrderByChangedAtAsc(orderId).map {
            OrderStatusHistoryView(it.id!!, it.orderId, it.fromStatus, it.toStatus, it.changedBy, it.changedAt, it.remark)
        }

    /** 按业务订单号查询流转历史，便于前端直接使用 orderNo。 */
    fun listOrderHistoriesByOrderNo(orderNo: String): List<OrderStatusHistoryView> {
        val order = orderRepository.findByOrderNo(orderNo).orElseThrow { IllegalArgumentException("order not found") }
        return listOrderHistories(order.id!!)
    }
}

@RestController
class AuditController(private val auditService: AuditService) {
    /** 后台：操作日志分页列表。 */
    @GetMapping("/api/v1/admin/audit/logs")
    fun listAuditLogs(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<PagedAuditLogs> = ok(auditService.listAuditLogsPage(page, size))

    /** 后台：订单状态流转历史。 */
    @GetMapping("/api/v1/admin/audit/orders/{orderId}/histories")
    fun listOrderHistories(@PathVariable orderId: Long): ApiResponse<List<OrderStatusHistoryView>> =
        ok(auditService.listOrderHistories(orderId))

    /** 后台：按 orderNo 查询订单状态流转历史。 */
    @GetMapping("/api/v1/admin/audit/orders/by-order-no/{orderNo}/histories")
    fun listOrderHistoriesByOrderNo(@PathVariable orderNo: String): ApiResponse<List<OrderStatusHistoryView>> =
        ok(auditService.listOrderHistoriesByOrderNo(orderNo))
}
