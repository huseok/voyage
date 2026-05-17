package com.trioForce.voyage.dictionary

/**
 * 字典模块（Dictionary）：
 * 统一承载订单状态、售后状态、SKU 规格属性等可配置枚举。
 * 前台只读启用项；后台支持类型/字典项的分页、查询与增删改。
 */
import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
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

    fun findAllByDictTypeIdOrderBySortNoAscIdAsc(dictTypeId: Long): List<DictItemEntity>

    fun countByDictTypeId(dictTypeId: Long): Long
}

data class DictTypeView(val dictCode: String, val dictName: String)

data class DictItemView(val itemCode: String, val itemLabel: String, val sortNo: Int)

data class DictTypeAdminView(
    val id: Long,
    val dictCode: String,
    val dictName: String,
    val itemCount: Long
)

data class DictItemAdminView(
    val id: Long,
    val itemCode: String,
    val itemLabel: String,
    val sortNo: Int,
    val isActive: Boolean
)

data class PagedDictTypes(
    val items: List<DictTypeAdminView>,
    val total: Long,
    val page: Int,
    val size: Int
)

data class DictTypeCreateRequest(@field:NotBlank val dictCode: String, @field:NotBlank val dictName: String)

data class DictTypeUpdateRequest(@field:NotBlank val dictName: String)

data class DictItemCreateRequest(
    @field:NotBlank val itemCode: String,
    @field:NotBlank val itemLabel: String,
    val sortNo: Int = 0,
    val isActive: Boolean = true
)

data class DictItemUpdateRequest(
    @field:NotBlank val itemLabel: String,
    val sortNo: Int = 0,
    val isActive: Boolean = true
)

@Service
class DictionaryService(
    private val dictTypeRepository: DictTypeRepository,
    private val dictItemRepository: DictItemRepository
) {
    private val log = LogUtil.logger<DictionaryService>()

    /** 前台/公共：字典类型列表（不含 id）。 */
    fun listTypes(): List<DictTypeView> =
        dictTypeRepository.findAll().map { DictTypeView(it.dictCode, it.dictName) }

    /** 前台/公共：仅返回启用中的字典项。 */
    fun listItems(dictCode: String): List<DictItemView> {
        val type = dictTypeRepository.findByDictCode(dictCode.trim().uppercase()) ?: return emptyList()
        return dictItemRepository.findAllByDictTypeIdAndIsActiveTrueOrderBySortNoAscIdAsc(type.id!!).map { toItemView(it) }
    }

    /** 后台：分页查询字典类型，支持编码/名称关键字。 */
    fun listTypesAdmin(q: String?, page: Int, size: Int): PagedDictTypes {
        val p = page.coerceAtLeast(0)
        val s = size.coerceIn(1, 100)
        val keyword = q?.trim()?.lowercase().orEmpty()
        val all = dictTypeRepository.findAll().sortedBy { it.dictCode }
        val filtered =
            if (keyword.isEmpty()) {
                all
            } else {
                all.filter {
                    it.dictCode.lowercase().contains(keyword) || it.dictName.lowercase().contains(keyword)
                }
            }
        val slice = filtered.drop(p * s).take(s)
        val items =
            slice.map { type ->
                val count = dictItemRepository.countByDictTypeId(type.id!!)
                DictTypeAdminView(type.id!!, type.dictCode, type.dictName, count)
            }
        return PagedDictTypes(items, filtered.size.toLong(), p, s)
    }

    /** 后台：某类型下全部字典项（含停用）。 */
    fun listItemsAdmin(dictCode: String): List<DictItemAdminView> {
        val type = dictTypeRepository.findByDictCode(dictCode.trim().uppercase())
            ?: throw BizException("字典类型不存在")
        return dictItemRepository.findAllByDictTypeIdOrderBySortNoAscIdAsc(type.id!!).map { toItemAdminView(it) }
    }

    @Transactional
    fun createType(req: DictTypeCreateRequest): Long {
        val code = req.dictCode.trim().uppercase()
        if (code.isBlank()) throw BizException("字典编码不能为空")
        if (dictTypeRepository.findByDictCode(code) != null) throw BizException("字典编码已存在")
        val now = OffsetDateTime.now()
        val id =
            dictTypeRepository.save(
                DictTypeEntity(
                    dictCode = code,
                    dictName = req.dictName.trim(),
                    createdAt = now,
                    updatedAt = now
                )
            ).id!!
        log.info("已创建字典类型，编码={}", code)
        return id
    }

    @Transactional
    fun updateType(id: Long, req: DictTypeUpdateRequest) {
        val entity = dictTypeRepository.findById(id).orElseThrow { BizException("字典类型不存在") }
        entity.dictName = req.dictName.trim()
        entity.updatedAt = OffsetDateTime.now()
        dictTypeRepository.save(entity)
        log.info("已更新字典类型，id={}，编码={}", id, entity.dictCode)
    }

    @Transactional
    fun deleteType(id: Long) {
        val entity = dictTypeRepository.findById(id).orElseThrow { BizException("字典类型不存在") }
        val now = OffsetDateTime.now()
        dictItemRepository.findAllByDictTypeIdOrderBySortNoAscIdAsc(entity.id!!).forEach { item ->
            item.isDeleted = true
            item.deletedAt = now
            item.updatedAt = now
            dictItemRepository.save(item)
        }
        entity.isDeleted = true
        entity.deletedAt = now
        entity.updatedAt = now
        dictTypeRepository.save(entity)
        log.info("已删除字典类型及其字典项，编码={}", entity.dictCode)
    }

    @Transactional
    fun createItem(dictCode: String, req: DictItemCreateRequest): Long {
        val type = dictTypeRepository.findByDictCode(dictCode.trim().uppercase())
            ?: throw BizException("字典类型不存在")
        if (type.dictCode == "PRODUCT_SKU_ATTR" || type.dictCode == "PRODUCT_SKU_ATTR_VALUE") {
            throw BizException("SKU 规格请在「SKU 规格属性」中维护，勿通过字典新增")
        }
        val itemCode = req.itemCode.trim().uppercase()
        val now = OffsetDateTime.now()
        val exists =
            dictItemRepository.findAllByDictTypeIdOrderBySortNoAscIdAsc(type.id!!)
                .any { it.itemCode.equals(itemCode, ignoreCase = true) }
        if (exists) throw BizException("该项编码在本字典下已存在")
        val id =
            dictItemRepository.save(
                DictItemEntity(
                    dictTypeId = type.id!!,
                    itemCode = itemCode,
                    itemLabel = req.itemLabel.trim(),
                    sortNo = req.sortNo,
                    isActive = req.isActive,
                    createdAt = now,
                    updatedAt = now
                )
            ).id!!
        log.info("已创建字典项，dictCode={}，itemCode={}", type.dictCode, itemCode)
        return id
    }

    @Transactional
    fun updateItem(itemId: Long, req: DictItemUpdateRequest) {
        val entity = dictItemRepository.findById(itemId).orElseThrow { BizException("字典项不存在") }
        entity.itemLabel = req.itemLabel.trim()
        entity.sortNo = req.sortNo
        entity.isActive = req.isActive
        entity.updatedAt = OffsetDateTime.now()
        dictItemRepository.save(entity)
        log.info("已更新字典项，id={}，itemCode={}", itemId, entity.itemCode)
    }

    @Transactional
    fun deleteItem(itemId: Long) {
        val entity = dictItemRepository.findById(itemId).orElseThrow { BizException("字典项不存在") }
        val now = OffsetDateTime.now()
        entity.isDeleted = true
        entity.deletedAt = now
        entity.updatedAt = now
        dictItemRepository.save(entity)
        log.info("已删除字典项，id={}，itemCode={}", itemId, entity.itemCode)
    }

    private fun toItemView(it: DictItemEntity) =
        DictItemView(it.itemCode, it.itemLabel, it.sortNo)

    private fun toItemAdminView(it: DictItemEntity) =
        DictItemAdminView(it.id!!, it.itemCode, it.itemLabel, it.sortNo, it.isActive)
}

@RestController
class DictionaryController(private val dictionaryService: DictionaryService) {
    @GetMapping("/api/v1/dicts/types")
    fun listTypes(): ApiResponse<List<DictTypeView>> = ok(dictionaryService.listTypes())

    @GetMapping("/api/v1/dicts/{dictCode}/items")
    fun listItems(@PathVariable dictCode: String): ApiResponse<List<DictItemView>> =
        ok(dictionaryService.listItems(dictCode))

    @GetMapping("/api/v1/admin/dicts/types")
    fun listTypesAdmin(
        @RequestParam(required = false) q: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "10") size: Int
    ): ApiResponse<PagedDictTypes> = ok(dictionaryService.listTypesAdmin(q, page, size))

    @GetMapping("/api/v1/admin/dicts/{dictCode}/items")
    fun listItemsAdmin(@PathVariable dictCode: String): ApiResponse<List<DictItemAdminView>> =
        ok(dictionaryService.listItemsAdmin(dictCode))

    @PostMapping("/api/v1/admin/dicts/types")
    fun createType(@Valid @RequestBody req: DictTypeCreateRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to dictionaryService.createType(req)))

    @PutMapping("/api/v1/admin/dicts/types/{id}")
    fun updateType(@PathVariable id: Long, @Valid @RequestBody req: DictTypeUpdateRequest): ApiResponse<String> {
        dictionaryService.updateType(id, req)
        return ok("updated")
    }

    @DeleteMapping("/api/v1/admin/dicts/types/{id}")
    fun deleteType(@PathVariable id: Long): ApiResponse<String> {
        dictionaryService.deleteType(id)
        return ok("deleted")
    }

    @PostMapping("/api/v1/admin/dicts/{dictCode}/items")
    fun createItem(
        @PathVariable dictCode: String,
        @Valid @RequestBody req: DictItemCreateRequest
    ): ApiResponse<Map<String, Long>> = ok(mapOf("id" to dictionaryService.createItem(dictCode, req)))

    @PutMapping("/api/v1/admin/dicts/items/{itemId}")
    fun updateItem(
        @PathVariable itemId: Long,
        @Valid @RequestBody req: DictItemUpdateRequest
    ): ApiResponse<String> {
        dictionaryService.updateItem(itemId, req)
        return ok("updated")
    }

    @DeleteMapping("/api/v1/admin/dicts/items/{itemId}")
    fun deleteItem(@PathVariable itemId: Long): ApiResponse<String> {
        dictionaryService.deleteItem(itemId)
        return ok("deleted")
    }
}
