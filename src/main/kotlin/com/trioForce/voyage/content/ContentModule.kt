package com.trioForce.voyage.content

/**
 * 内容模块（CMS + 商务合作）：
 * - t_site_contents：站点说明、首页 banner、优势卡片、联系人信息等
 * - t_business_cooperations：商务合作线索
 *
 * 前台可读内容，后台可维护内容和线索状态。
 */

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.Where
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime

@Entity
@Table(name = "t_site_contents")
@Where(clause = "is_deleted = false")
class SiteContentEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "content_key", nullable = false, unique = true, length = 80)
    var contentKey: String,
    @Column(name = "content_type", nullable = false, length = 30)
    var contentType: String,
    @Column(length = 255)
    var title: String? = null,
    @Column(length = 500)
    var subtitle: String? = null,
    @Column(columnDefinition = "text")
    var body: String? = null,
    @Column(name = "image_url", length = 500)
    var imageUrl: String? = null,
    @Column(name = "action_url", length = 500)
    var actionUrl: String? = null,
    @Column(name = "sort_no", nullable = false)
    var sortNo: Int = 0,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
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
@Table(name = "t_business_cooperations")
@Where(clause = "is_deleted = false")
class BusinessCooperationEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, length = 255)
    var email: String,
    @Column(name = "contact_name", length = 100)
    var contactName: String? = null,
    @Column(length = 60)
    var whatsapp: String? = null,
    @Column(length = 60)
    var wechat: String? = null,
    @Column(columnDefinition = "text", nullable = false)
    var content: String,
    @Column(nullable = false, length = 30)
    var status: String = "NEW",
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

interface SiteContentRepository : JpaRepository<SiteContentEntity, Long> {
    fun findAllByIsActiveTrueOrderBySortNoAscIdAsc(): List<SiteContentEntity>
    fun findByContentKey(contentKey: String): SiteContentEntity?
}

interface BusinessCooperationRepository : JpaRepository<BusinessCooperationEntity, Long>

data class SiteContentUpsertRequest(
    @field:NotBlank val contentKey: String,
    @field:NotBlank val contentType: String,
    val title: String? = null,
    val subtitle: String? = null,
    val body: String? = null,
    val imageUrl: String? = null,
    val actionUrl: String? = null,
    val sortNo: Int = 0,
    val isActive: Boolean = true
)

data class SiteContentView(
    val id: Long,
    val contentKey: String,
    val contentType: String,
    val title: String?,
    val subtitle: String?,
    val body: String?,
    val imageUrl: String?,
    val actionUrl: String?,
    val sortNo: Int,
    val isActive: Boolean
)

data class CreateBusinessCooperationRequest(
    @field:Email @field:NotBlank val email: String,
    val contactName: String? = null,
    val whatsapp: String? = null,
    val wechat: String? = null,
    @field:NotBlank val content: String
)

data class BusinessCooperationStatusRequest(
    @field:NotBlank val status: String
)

data class BusinessCooperationView(
    val id: Long,
    val email: String,
    val contactName: String?,
    val whatsapp: String?,
    val wechat: String?,
    val content: String,
    val status: String,
    val createdAt: OffsetDateTime
)

@Service
class ContentService(
    private val siteContentRepository: SiteContentRepository,
    private val cooperationRepository: BusinessCooperationRepository
) {
    /** 前台读取所有已启用内容。 */
    fun listActiveContents(): List<SiteContentView> =
        siteContentRepository.findAllByIsActiveTrueOrderBySortNoAscIdAsc().map(::toContentView)

    /** 后台读取内容（当前简化为全量）。 */
    fun listAllContents(): List<SiteContentView> = siteContentRepository.findAll().map(::toContentView)

    @Transactional
    /** 后台新增或更新内容（按 content_key 幂等更新）。 */
    fun upsertContent(req: SiteContentUpsertRequest): Long {
        val now = OffsetDateTime.now()
        val key = req.contentKey.trim().uppercase()
        val existed = siteContentRepository.findByContentKey(key)
        val entity = existed?.apply {
            contentType = req.contentType.trim().uppercase()
            title = req.title?.trim()
            subtitle = req.subtitle?.trim()
            body = req.body?.trim()
            imageUrl = req.imageUrl?.trim()
            actionUrl = req.actionUrl?.trim()
            sortNo = req.sortNo
            isActive = req.isActive
            updatedAt = now
        } ?: SiteContentEntity(
            contentKey = key,
            contentType = req.contentType.trim().uppercase(),
            title = req.title?.trim(),
            subtitle = req.subtitle?.trim(),
            body = req.body?.trim(),
            imageUrl = req.imageUrl?.trim(),
            actionUrl = req.actionUrl?.trim(),
            sortNo = req.sortNo,
            isActive = req.isActive,
            createdAt = now,
            updatedAt = now
        )
        return siteContentRepository.save(entity).id!!
    }

    @Transactional
    /** 前台提交商务合作线索。 */
    fun createCooperation(req: CreateBusinessCooperationRequest): Long {
        val now = OffsetDateTime.now()
        return cooperationRepository.save(
            BusinessCooperationEntity(
                email = req.email.trim().lowercase(),
                contactName = req.contactName?.trim(),
                whatsapp = req.whatsapp?.trim(),
                wechat = req.wechat?.trim(),
                content = req.content.trim(),
                createdAt = now,
                updatedAt = now
            )
        ).id!!
    }

    /** 后台查询合作线索。 */
    fun listCooperations(): List<BusinessCooperationView> = cooperationRepository.findAll().map {
        BusinessCooperationView(it.id!!, it.email, it.contactName, it.whatsapp, it.wechat, it.content, it.status, it.createdAt)
    }

    @Transactional
    /** 后台推进合作线索状态。 */
    fun updateCooperationStatus(id: Long, req: BusinessCooperationStatusRequest) {
        val row = cooperationRepository.findById(id).orElseThrow { IllegalArgumentException("cooperation not found") }
        row.status = req.status.trim().uppercase()
        row.updatedAt = OffsetDateTime.now()
        cooperationRepository.save(row)
    }

    private fun toContentView(e: SiteContentEntity): SiteContentView =
        SiteContentView(e.id!!, e.contentKey, e.contentType, e.title, e.subtitle, e.body, e.imageUrl, e.actionUrl, e.sortNo, e.isActive)
}

@RestController
class ContentController(private val contentService: ContentService) {
    /** 前台：读取站点内容。 */
    @GetMapping("/api/v1/site/contents")
    fun listSiteContents(): ApiResponse<List<SiteContentView>> = ok(contentService.listActiveContents())

    /** 后台：读取所有站点内容。 */
    @GetMapping("/api/v1/admin/site/contents")
    fun listAllSiteContents(): ApiResponse<List<SiteContentView>> = ok(contentService.listAllContents())

    /** 后台：新增或更新站点内容。 */
    @PostMapping("/api/v1/admin/site/contents")
    fun upsertSiteContent(@Valid @RequestBody req: SiteContentUpsertRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to contentService.upsertContent(req)))

    /** 前台：提交商务合作。 */
    @PostMapping("/api/v1/business/cooperations")
    fun createCooperation(@Valid @RequestBody req: CreateBusinessCooperationRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to contentService.createCooperation(req)))

    /** 后台：线索列表。 */
    @GetMapping("/api/v1/admin/business/cooperations")
    fun listCooperations(): ApiResponse<List<BusinessCooperationView>> = ok(contentService.listCooperations())

    /** 后台：更新线索状态。 */
    @PatchMapping("/api/v1/admin/business/cooperations/{id}/status")
    fun updateCooperationStatus(@PathVariable id: Long, @Valid @RequestBody req: BusinessCooperationStatusRequest): ApiResponse<String> {
        contentService.updateCooperationStatus(id, req)
        return ok("updated")
    }
}
