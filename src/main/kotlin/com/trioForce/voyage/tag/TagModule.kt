package com.trioForce.voyage.tag

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.ok
import com.trioForce.voyage.i18n.I18nLocaleSupport
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
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
 * - [TagEntity]：标签主数据（编码唯一、中英文名称、排序、启用、是否前台展示）
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
    @Column(name = "name_zh", nullable = false, length = 120)
    var nameZh: String,
    @Column(name = "name_en", nullable = false, length = 120)
    var nameEn: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    var i18n: Map<String, String> = emptyMap(),
    @Column(name = "sort_no", nullable = false)
    var sortNo: Int = 0,
    @Column(name = "is_active", nullable = false)
    var isActive: Boolean = true,
    /** 是否在商城前台展示（目录筛选、商品详情标签等）；停用标签始终不展示 */
    @Column(name = "show_on_storefront", nullable = false)
    var showOnStorefront: Boolean = true,
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

    /** 前台筛选用：启用且允许前台展示 */
    fun findAllByIsActiveIsTrueAndShowOnStorefrontIsTrueOrderBySortNoAscIdAsc(): List<TagEntity>

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
    val nameZh: String,
    val nameEn: String,
    val i18n: Map<String, String> = emptyMap(),
    val sortNo: Int = 0,
    val isActive: Boolean = true,
    val showOnStorefront: Boolean = true,
)

data class TagUpsertRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Za-z0-9][A-Za-z0-9_-]{1,62}$", message = "tag code: 2-64 chars, letters, digits, _ or -")
    val code: String,
    val nameZh: String? = null,
    val nameEn: String? = null,
    val i18n: Map<String, String>? = null,
    val sortNo: Int = 0,
    val isActive: Boolean = true,
    val showOnStorefront: Boolean = true,
)

private fun TagEntity.toView(): TagView {
    val map = if (i18n.isNotEmpty()) i18n else I18nLocaleSupport.fromBilingual(nameZh, nameEn)
    return TagView(id!!, code, nameZh, nameEn, map, sortNo, isActive, showOnStorefront)
}

private fun resolveTagI18n(req: TagUpsertRequest): Triple<Map<String, String>, String, String> {
    val map = linkedMapOf<String, String>()
    req.i18n?.forEach { (k, v) ->
        val key = k.trim()
        val valStr = v.trim()
        if (key.isNotEmpty() && valStr.isNotEmpty()) map[key] = valStr
    }
    if (map.isEmpty()) {
        val zh = req.nameZh?.trim().orEmpty()
        val en = req.nameEn?.trim().orEmpty()
        if (en.isBlank()) throw BizException("英文名称（en-US）不能为空")
        return Triple(I18nLocaleSupport.fromBilingual(zh.ifEmpty { en }, en), zh.ifEmpty { en }, en)
    }
    val en = map["en-US"]?.trim().orEmpty()
    if (en.isBlank()) throw BizException("英文名称（en-US）不能为空")
    val (zh, enCol) = I18nLocaleSupport.syncBilingualColumns(map)
    return Triple(map, zh, enCol)
}

@Service
class TagService(
    private val tagRepository: TagRepository,
    private val productTagRepository: ProductTagRepository,
) {
    fun listAll(): List<TagView> =
        tagRepository.findAllByOrderBySortNoAscIdAsc().map { it.toView() }

    /** 匿名可读：目录页标签筛选（启用 + 允许前台展示） */
    fun listActiveForStorefront(): List<TagView> =
        tagRepository.findAllByIsActiveIsTrueAndShowOnStorefrontIsTrueOrderBySortNoAscIdAsc().map { it.toView() }

    @Transactional
    fun create(req: TagUpsertRequest): Long {
        val now = OffsetDateTime.now()
        val (i18nMap, nameZh, nameEn) = resolveTagI18n(req)
        val entity = tagRepository.save(
            TagEntity(
                code = req.code.trim().uppercase(),
                nameZh = nameZh,
                nameEn = nameEn,
                i18n = i18nMap,
                sortNo = req.sortNo,
                isActive = req.isActive,
                showOnStorefront = req.showOnStorefront,
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
        val (i18nMap, nameZh, nameEn) = resolveTagI18n(req)
        entity.nameZh = nameZh
        entity.nameEn = nameEn
        entity.i18n = i18nMap
        entity.sortNo = req.sortNo
        entity.isActive = req.isActive
        entity.showOnStorefront = req.showOnStorefront
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
