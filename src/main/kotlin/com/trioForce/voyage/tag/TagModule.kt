package com.trioForce.voyage.tag

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.ok
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime
import java.util.Optional

/**
 * 商品标签模块：
 * - [TagEntity]：标签主数据（编码唯一、展示名、排序、启用）
 * - [ProductTagEntity]：商品与标签多对多关联（级联删除）
 * - 后台 CRUD：`/api/v1/admin/tags`
 */
@Entity
@Table(name = "t_tags")
class TagEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(nullable = false, unique = true, length = 64)
    var code: String,
    @Column(nullable = false, length = 120)
    var name: String,
    @Column(name = "sort_no", nullable = false)
    var sortNo: Int = 0,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
    @Column(name = "updated_at", nullable = false)
    var updatedAt: OffsetDateTime = OffsetDateTime.now(),
)

@Entity
@Table(name = "t_product_tags")
class ProductTagEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "product_id", nullable = false)
    var productId: Long,
    @Column(name = "tag_id", nullable = false)
    var tagId: Long,
    @Column(name = "created_at", nullable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now(),
)

interface TagRepository : JpaRepository<TagEntity, Long> {
    fun findAllByOrderBySortNoAscIdAsc(): List<TagEntity>

    /** 前台筛选用：仅返回启用标签，排序一致 */
    fun findAllByIsActiveIsTrueOrderBySortNoAscIdAsc(): List<TagEntity>

    fun findByCode(code: String): Optional<TagEntity>
}

interface ProductTagRepository : JpaRepository<ProductTagEntity, Long> {
    fun deleteAllByProductId(productId: Long)
    fun deleteAllByTagId(tagId: Long)
    fun findAllByProductIdIn(productIds: Collection<Long>): List<ProductTagEntity>
}

data class TagView(
    val id: Long,
    val code: String,
    val name: String,
    val sortNo: Int = 0,
    val isActive: Boolean = true,
)

data class TagUpsertRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{1,62}$", message = "tag code: 2-64 chars, letters, digits, _ or -")
    val code: String,
    @field:NotBlank val name: String,
    val sortNo: Int = 0,
    val isActive: Boolean = true,
)

@Service
class TagService(
    private val tagRepository: TagRepository,
    private val productTagRepository: ProductTagRepository,
) {
    fun listAll(): List<TagView> =
        tagRepository.findAllByOrderBySortNoAscIdAsc().map {
            TagView(it.id!!, it.code, it.name, it.sortNo, it.isActive)
        }

    /** 匿名可读：目录页标签筛选 */
    fun listActiveForStorefront(): List<TagView> =
        tagRepository.findAllByIsActiveIsTrueOrderBySortNoAscIdAsc().map {
            TagView(it.id!!, it.code, it.name, it.sortNo, it.isActive)
        }

    @Transactional
    fun create(req: TagUpsertRequest): Long {
        val now = OffsetDateTime.now()
        val entity = tagRepository.save(
            TagEntity(
                code = req.code.trim().uppercase(),
                name = req.name.trim(),
                sortNo = req.sortNo,
                isActive = req.isActive,
                createdAt = now,
                updatedAt = now,
            )
        )
        return entity.id!!
    }

    @Transactional
    fun update(id: Long, req: TagUpsertRequest) {
        val entity = tagRepository.findById(id).orElseThrow { BizException("tag not found") }
        val newCode = req.code.trim().uppercase()
        if (entity.code != newCode) {
            throw BizException("tag code cannot be changed")
        }
        entity.name = req.name.trim()
        entity.sortNo = req.sortNo
        entity.isActive = req.isActive
        entity.updatedAt = OffsetDateTime.now()
        tagRepository.save(entity)
    }

    @Transactional
    fun delete(id: Long) {
        tagRepository.findById(id).orElseThrow { BizException("tag not found") }
        productTagRepository.deleteAllByTagId(id)
        tagRepository.deleteById(id)
    }
}

@RestController
class StorefrontTagController(private val tagService: TagService) {
    @GetMapping("/api/v1/tags")
    fun listActive(): ApiResponse<List<TagView>> = ok(tagService.listActiveForStorefront())
}

@RestController
class AdminTagController(private val tagService: TagService) {
    @GetMapping("/api/v1/admin/tags")
    fun list(): ApiResponse<List<TagView>> = ok(tagService.listAll())

    @PostMapping("/api/v1/admin/tags")
    fun create(@Valid @RequestBody req: TagUpsertRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to tagService.create(req)))

    @PutMapping("/api/v1/admin/tags/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: TagUpsertRequest): ApiResponse<String> {
        tagService.update(id, req)
        return ok("updated")
    }

    @DeleteMapping("/api/v1/admin/tags/{id}")
    fun delete(@PathVariable id: Long): ApiResponse<String> {
        tagService.delete(id)
        return ok("deleted")
    }
}
