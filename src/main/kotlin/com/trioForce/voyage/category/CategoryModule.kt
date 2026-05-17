package com.trioForce.voyage.category

/**
 * 分类模块（Category）：
 * - 前台：查询分类树/列表用于筛选和导航
 * - 后台：新增分类（后续可扩展编辑、禁用、拖拽排序）
 *
 * 当前先提供最小闭环，保证前后台能共用统一分类数据。
 */
import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.BizException
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
    @Column(name = "name_zh", nullable = false, length = 120)
    var nameZh: String,
    @Column(name = "name_en", nullable = false, length = 120)
    var nameEn: String,
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

    fun findAllByParentIdOrderBySortNoAscIdAsc(parentId: Long): List<CategoryEntity>

    fun countByParentId(parentId: Long): Long
}

data class CategoryUpsertRequest(
    val parentId: Long? = null,
    @field:NotBlank val nameZh: String,
    @field:NotBlank val nameEn: String,
    @field:NotBlank val code: String,
    val sortNo: Int = 0,
    val isActive: Boolean = true
)

data class CategoryView(
    val id: Long,
    val parentId: Long?,
    val nameZh: String,
    val nameEn: String,
    val code: String,
    val sortNo: Int,
    val isActive: Boolean
)

@Service
class CategoryService(private val categoryRepository: CategoryRepository) {
    /** 分类列表输出 DTO，避免直接暴露实体。 */
    fun listAll(): List<CategoryView> = categoryRepository.findAllByOrderBySortNoAscIdAsc().map { toView(it) }

    /** 某一级分类下的子分类 ID（含自身），供商品按父级筛选 */
    fun resolveCategoryFilterIds(categoryId: Long): List<Long> {
        val self = categoryRepository.findById(categoryId).orElse(null) ?: return listOf(categoryId)
        if (self.parentId != null) return listOf(categoryId)
        val childIds = categoryRepository.findAllByParentIdOrderBySortNoAscIdAsc(categoryId).mapNotNull { it.id }
        return listOf(categoryId) + childIds
    }

    @Transactional
    /** 后台新增分类。code 统一大写，便于跨系统做稳定枚举映射。 */
    fun create(req: CategoryUpsertRequest): Long {
        validateParent(req.parentId, selfId = null)
        val code = req.code.trim().uppercase()
        if (code.isBlank()) throw BizException("分类编码不能为空")
        val now = OffsetDateTime.now()
        val entity = categoryRepository.save(
            CategoryEntity(
                parentId = req.parentId,
                nameZh = req.nameZh.trim(),
                nameEn = req.nameEn.trim(),
                code = code,
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
        val entity = categoryRepository.findById(id).orElseThrow { BizException("分类不存在") }
        val hasChildren = categoryRepository.countByParentId(id) > 0
        if (hasChildren && req.parentId != null) {
            throw BizException("该分类下已有子分类，不能改为二级分类")
        }
        validateParent(req.parentId, selfId = id)
        val now = OffsetDateTime.now()
        entity.parentId = req.parentId
        entity.nameZh = req.nameZh.trim()
        entity.nameEn = req.nameEn.trim()
        entity.code = req.code.trim().uppercase()
        entity.sortNo = req.sortNo
        entity.isActive = req.isActive
        entity.updatedAt = now
        categoryRepository.save(entity)
    }

    @Transactional
    /** 后台删除分类（若有子分类一并逻辑删除）。 */
    fun delete(id: Long) {
        val entity = categoryRepository.findById(id).orElseThrow { BizException("分类不存在") }
        val now = OffsetDateTime.now()
        categoryRepository.findAllByParentIdOrderBySortNoAscIdAsc(id).forEach { child ->
            child.isDeleted = true
            child.deletedAt = now
            child.updatedAt = now
            categoryRepository.save(child)
        }
        entity.isDeleted = true
        entity.deletedAt = now
        entity.updatedAt = now
        categoryRepository.save(entity)
    }

    private fun toView(it: CategoryEntity) =
        CategoryView(it.id!!, it.parentId, it.nameZh, it.nameEn, it.code, it.sortNo, it.isActive)

    /** 仅允许二级：父级必须是一级分类，且不能选自身 */
    private fun validateParent(parentId: Long?, selfId: Long?) {
        if (parentId == null) return
        if (selfId != null && parentId == selfId) throw BizException("不能将自身设为父级分类")
        val parent = categoryRepository.findById(parentId).orElseThrow { BizException("父级分类不存在") }
        if (parent.parentId != null) throw BizException("仅支持二级分类，请选择一级分类作为父级")
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
