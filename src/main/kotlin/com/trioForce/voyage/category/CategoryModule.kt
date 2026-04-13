package com.trioForce.voyage.category

/**
 * 分类模块（Category）：
 * - 前台：查询分类树/列表用于筛选和导航
 * - 后台：新增分类（后续可扩展编辑、禁用、拖拽排序）
 *
 * 当前先提供最小闭环，保证前后台能共用统一分类数据。
 */
import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.Where
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime

@Entity
@Table(name = "t_categories")
@Where(clause = "is_deleted = false")
class CategoryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "parent_id")
    var parentId: Long? = null,
    @Column(nullable = false, length = 120)
    var name: String,
    @Column(nullable = false, unique = true, length = 80)
    var code: String,
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

interface CategoryRepository : JpaRepository<CategoryEntity, Long> {
    /** 管理端与前台共用：按 sort_no + id 稳定输出，避免列表抖动 */
    fun findAllByOrderBySortNoAscIdAsc(): List<CategoryEntity>
}

data class CategoryUpsertRequest(
    val parentId: Long? = null,
    @field:NotBlank val name: String,
    @field:NotBlank val code: String,
    val sortNo: Int = 0,
    val isActive: Boolean = true
)

data class CategoryView(
    val id: Long,
    val parentId: Long?,
    val name: String,
    val code: String,
    val sortNo: Int,
    val isActive: Boolean
)

@Service
class CategoryService(private val categoryRepository: CategoryRepository) {
    /** 分类列表输出 DTO，避免直接暴露实体。 */
    fun listAll(): List<CategoryView> = categoryRepository.findAllByOrderBySortNoAscIdAsc().map {
        CategoryView(it.id!!, it.parentId, it.name, it.code, it.sortNo, it.isActive)
    }

    @Transactional
    /** 后台新增分类。code 统一大写，便于跨系统做稳定枚举映射。 */
    fun create(req: CategoryUpsertRequest): Long {
        val now = OffsetDateTime.now()
        val entity = categoryRepository.save(
            CategoryEntity(
                parentId = req.parentId,
                name = req.name.trim(),
                code = req.code.trim().uppercase(),
                sortNo = req.sortNo,
                isActive = req.isActive,
                createdAt = now,
                updatedAt = now
            )
        )
        return entity.id!!
    }

    @Transactional
    /** 后台更新分类。 */
    fun update(id: Long, req: CategoryUpsertRequest) {
        val now = OffsetDateTime.now()
        val entity = categoryRepository.findById(id).orElseThrow { IllegalArgumentException("category not found") }
        entity.parentId = req.parentId
        entity.name = req.name.trim()
        entity.code = req.code.trim().uppercase()
        entity.sortNo = req.sortNo
        entity.isActive = req.isActive
        entity.updatedAt = now
        categoryRepository.save(entity)
    }

    @Transactional
    /** 后台删除分类。 */
    fun delete(id: Long) {
        val entity = categoryRepository.findById(id).orElseThrow { IllegalArgumentException("category not found") }
        entity.isDeleted = true
        entity.deletedAt = OffsetDateTime.now()
        entity.updatedAt = OffsetDateTime.now()
        categoryRepository.save(entity)
    }
}

@RestController
class CategoryController(private val categoryService: CategoryService) {
    /** 前台分类查询（支持左侧树、搜索建议等场景）。 */
    @GetMapping("/api/v1/categories")
    fun list(): ApiResponse<List<CategoryView>> = ok(categoryService.listAll())

    /** 后台新增分类。 */
    @PostMapping("/api/v1/admin/categories")
    fun create(@Valid @RequestBody req: CategoryUpsertRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to categoryService.create(req)))

    /** 后台编辑分类。 */
    @PutMapping("/api/v1/admin/categories/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody req: CategoryUpsertRequest): ApiResponse<String> {
        categoryService.update(id, req)
        return ok("updated")
    }

    /** 后台删除分类。 */
    @DeleteMapping("/api/v1/admin/categories/{id}")
    fun delete(@PathVariable id: Long): ApiResponse<String> {
        categoryService.delete(id)
        return ok("deleted")
    }
}
