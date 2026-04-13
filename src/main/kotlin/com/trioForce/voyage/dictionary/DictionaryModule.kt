package com.trioForce.voyage.dictionary

/**
 * 字典模块（Dictionary）：
 * 统一承载订单状态、售后状态、合作状态、计费模式等可配置枚举。
 * 前台/后台都通过该模块读取展示值，减少硬编码分散。
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
@Table(name = "t_dict_types")
@Where(clause = "is_deleted = false")
class DictTypeEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "dict_code", nullable = false, unique = true, length = 80)
    var dictCode: String,
    @Column(name = "dict_name", nullable = false, length = 120)
    var dictName: String,
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
@Table(name = "t_dict_items")
@Where(clause = "is_deleted = false")
class DictItemEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "dict_type_id", nullable = false)
    var dictTypeId: Long,
    @Column(name = "item_code", nullable = false, length = 80)
    var itemCode: String,
    @Column(name = "item_label", nullable = false, length = 120)
    var itemLabel: String,
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

interface DictTypeRepository : JpaRepository<DictTypeEntity, Long> {
    fun findByDictCode(dictCode: String): DictTypeEntity?
}

interface DictItemRepository : JpaRepository<DictItemEntity, Long> {
    fun findAllByDictTypeIdAndIsActiveTrueOrderBySortNoAscIdAsc(dictTypeId: Long): List<DictItemEntity>
}

data class DictTypeView(val dictCode: String, val dictName: String)
data class DictItemView(val itemCode: String, val itemLabel: String, val sortNo: Int)
data class DictTypeCreateRequest(@field:NotBlank val dictCode: String, @field:NotBlank val dictName: String)
data class DictItemCreateRequest(
    @field:NotBlank val itemCode: String,
    @field:NotBlank val itemLabel: String,
    val sortNo: Int = 0
)

@Service
class DictionaryService(
    private val dictTypeRepository: DictTypeRepository,
    private val dictItemRepository: DictItemRepository
) {
    fun listTypes(): List<DictTypeView> = dictTypeRepository.findAll().map { DictTypeView(it.dictCode, it.dictName) }

    fun listItems(dictCode: String): List<DictItemView> {
        val type = dictTypeRepository.findByDictCode(dictCode.trim().uppercase()) ?: return emptyList()
        return dictItemRepository.findAllByDictTypeIdAndIsActiveTrueOrderBySortNoAscIdAsc(type.id!!).map {
            DictItemView(it.itemCode, it.itemLabel, it.sortNo)
        }
    }

    @Transactional
    fun createType(req: DictTypeCreateRequest): Long {
        val now = OffsetDateTime.now()
        return dictTypeRepository.save(
            DictTypeEntity(
                dictCode = req.dictCode.trim().uppercase(),
                dictName = req.dictName.trim(),
                createdAt = now,
                updatedAt = now
            )
        ).id!!
    }

    @Transactional
    fun createItem(dictCode: String, req: DictItemCreateRequest): Long {
        val type = dictTypeRepository.findByDictCode(dictCode.trim().uppercase())
            ?: throw IllegalArgumentException("dict type not found")
        val now = OffsetDateTime.now()
        return dictItemRepository.save(
            DictItemEntity(
                dictTypeId = type.id!!,
                itemCode = req.itemCode.trim().uppercase(),
                itemLabel = req.itemLabel.trim(),
                sortNo = req.sortNo,
                createdAt = now,
                updatedAt = now
            )
        ).id!!
    }
}

@RestController
class DictionaryController(private val dictionaryService: DictionaryService) {
    /** 前台/后台：查询字典类型。 */
    @GetMapping("/api/v1/dicts/types")
    fun listTypes(): ApiResponse<List<DictTypeView>> = ok(dictionaryService.listTypes())

    /** 前台/后台：按字典编码查询字典项。 */
    @GetMapping("/api/v1/dicts/{dictCode}/items")
    fun listItems(@PathVariable dictCode: String): ApiResponse<List<DictItemView>> = ok(dictionaryService.listItems(dictCode))

    /** 后台：创建字典类型。 */
    @PostMapping("/api/v1/admin/dicts/types")
    fun createType(@Valid @RequestBody req: DictTypeCreateRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to dictionaryService.createType(req)))

    /** 后台：创建字典项。 */
    @PostMapping("/api/v1/admin/dicts/{dictCode}/items")
    fun createItem(@PathVariable dictCode: String, @Valid @RequestBody req: DictItemCreateRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to dictionaryService.createItem(dictCode, req)))
}
