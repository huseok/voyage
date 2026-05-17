package com.trioForce.voyage.product

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
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

/**
 * 商品 SKU 规格属性（维度 + 取值），独立于字典管理。
 * 后台维护路径：`/api/v1/admin/sku-specs/`（dimensions、values 等子路径）
 */
@Entity
@Table(name = "t_sku_attr_dimensions")
class SkuAttrDimensionEntity(
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
@Table(name = "t_sku_attr_values")
class SkuAttrValueEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "dimension_id", nullable = false)
    var dimensionId: Long,
    @Column(nullable = false, length = 64)
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

interface SkuAttrDimensionRepository : JpaRepository<SkuAttrDimensionEntity, Long> {
    fun findAllByOrderBySortNoAscIdAsc(): List<SkuAttrDimensionEntity>

    fun findByCode(code: String): SkuAttrDimensionEntity?
}

interface SkuAttrValueRepository : JpaRepository<SkuAttrValueEntity, Long> {
    fun findAllByDimensionIdOrderBySortNoAscIdAsc(dimensionId: Long): List<SkuAttrValueEntity>

    fun deleteAllByDimensionId(dimensionId: Long)
}

data class SkuAttrValueView(
    val id: Long,
    val dimensionId: Long,
    val code: String,
    val name: String,
    val sortNo: Int,
    val isActive: Boolean,
)

data class SkuAttrDimensionView(
    val id: Long,
    val code: String,
    val name: String,
    val sortNo: Int,
    val isActive: Boolean,
    val values: List<SkuAttrValueView> = emptyList(),
)

data class SkuAttrDimensionUpsertRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z0-9][A-Z0-9_]{0,62}$", message = "code: uppercase letters, digits, underscore")
    val code: String,
    @field:NotBlank val name: String,
    val sortNo: Int = 0,
    val isActive: Boolean = true,
)

data class SkuAttrValueUpsertRequest(
    @field:NotBlank
    @field:Pattern(regexp = "^[A-Z0-9][A-Z0-9_]{0,62}$", message = "code: uppercase letters, digits, underscore")
    val code: String,
    @field:NotBlank val name: String,
    val sortNo: Int = 0,
    val isActive: Boolean = true,
)

data class SkuAttrValueBatchLine(
    @field:NotBlank val code: String,
    @field:NotBlank val name: String,
)

data class SkuAttrValueBatchCreateRequest(
    val items: List<SkuAttrValueBatchLine>,
    val sortNoStart: Int = 0,
    val isActive: Boolean = true,
)

/** 一次创建属性维度及其下多条取值（一对多） */
data class SkuAttrDimensionWithValuesCreateRequest(
    @field:Valid val dimension: SkuAttrDimensionUpsertRequest,
    val values: List<SkuAttrValueBatchLine> = emptyList(),
)

@Service
class ProductSkuSpecService(
    private val dimensionRepository: SkuAttrDimensionRepository,
    private val valueRepository: SkuAttrValueRepository,
) {
    private val log = LogUtil.logger<ProductSkuSpecService>()

    /** 规格矩阵等后台页：维度及启用中的取值一览 */
    fun catalogForAdmin(): List<SkuAttrDimensionView> {
        val dimensions = dimensionRepository.findAllByOrderBySortNoAscIdAsc()
        return dimensions.map { dim ->
            val values =
                valueRepository.findAllByDimensionIdOrderBySortNoAscIdAsc(dim.id!!)
                    .filter { it.isActive }
                    .map { toValueView(it) }
            SkuAttrDimensionView(dim.id!!, dim.code, dim.name, dim.sortNo, dim.isActive, values)
        }
    }

    fun listDimensions(): List<SkuAttrDimensionView> =
        dimensionRepository.findAllByOrderBySortNoAscIdAsc().map { toDimensionView(it, includeValues = true) }

    fun listValues(dimensionId: Long): List<SkuAttrValueView> {
        ensureDimension(dimensionId)
        return valueRepository.findAllByDimensionIdOrderBySortNoAscIdAsc(dimensionId).map { toValueView(it) }
    }

    @Transactional
    fun createDimension(req: SkuAttrDimensionUpsertRequest): Long {
        val code = req.code.trim().uppercase()
        if (dimensionRepository.findByCode(code) != null) throw BizException("属性维度编码已存在")
        val now = OffsetDateTime.now()
        val id =
            dimensionRepository.save(
                SkuAttrDimensionEntity(
                    code = code,
                    name = req.name.trim(),
                    sortNo = req.sortNo,
                    isActive = req.isActive,
                    createdAt = now,
                    updatedAt = now,
                )
            ).id!!
        log.info("已创建 SKU 属性维度，code={}", code)
        return id
    }

    @Transactional
    fun createDimensionWithValues(req: SkuAttrDimensionWithValuesCreateRequest): Pair<Long, Int> {
        val dimId = createDimension(req.dimension)
        val count =
            if (req.values.isEmpty()) {
                0
            } else {
                createValuesBatch(
                    dimId,
                    SkuAttrValueBatchCreateRequest(items = req.values, sortNoStart = 0, isActive = true),
                )
            }
        log.info("已创建 SKU 维度及取值，dimensionId={}，取值条数={}", dimId, count)
        return dimId to count
    }

    @Transactional
    fun updateDimension(id: Long, req: SkuAttrDimensionUpsertRequest) {
        val entity = ensureDimension(id)
        val newCode = req.code.trim().uppercase()
        if (entity.code != newCode) throw BizException("属性维度编码创建后不可修改")
        entity.name = req.name.trim()
        entity.sortNo = req.sortNo
        entity.isActive = req.isActive
        entity.updatedAt = OffsetDateTime.now()
        dimensionRepository.save(entity)
        log.info("已更新 SKU 属性维度，id={}", id)
    }

    @Transactional
    fun deleteDimension(id: Long) {
        ensureDimension(id)
        valueRepository.deleteAllByDimensionId(id)
        dimensionRepository.deleteById(id)
        log.info("已删除 SKU 属性维度及其取值，id={}", id)
    }

    @Transactional
    fun createValue(dimensionId: Long, req: SkuAttrValueUpsertRequest): Long {
        val dim = ensureDimension(dimensionId)
        val code = req.code.trim().uppercase()
        assertValueCodeUnique(dimensionId, code, null)
        val now = OffsetDateTime.now()
        val id =
            valueRepository.save(
                SkuAttrValueEntity(
                    dimensionId = dim.id!!,
                    code = code,
                    name = req.name.trim(),
                    sortNo = req.sortNo,
                    isActive = req.isActive,
                    createdAt = now,
                    updatedAt = now,
                )
            ).id!!
        log.info("已创建 SKU 属性取值，dimension={}，code={}", dim.code, code)
        return id
    }

    @Transactional
    fun createValuesBatch(dimensionId: Long, req: SkuAttrValueBatchCreateRequest): Int {
        val dim = ensureDimension(dimensionId)
        if (req.items.isEmpty()) throw BizException("请至少填写一条取值")
        val now = OffsetDateTime.now()
        var sort = req.sortNoStart
        var created = 0
        for (line in req.items) {
            val code = line.code.trim().uppercase()
            val name = line.name.trim()
            if (code.isBlank() || name.isBlank()) continue
            val exists =
                valueRepository.findAllByDimensionIdOrderBySortNoAscIdAsc(dim.id!!)
                    .any { it.code.equals(code, ignoreCase = true) }
            if (exists) continue
            valueRepository.save(
                SkuAttrValueEntity(
                    dimensionId = dim.id!!,
                    code = code,
                    name = name,
                    sortNo = sort++,
                    isActive = req.isActive,
                    createdAt = now,
                    updatedAt = now,
                )
            )
            created++
        }
        if (created == 0) throw BizException("没有新增取值（可能编码均已存在或内容为空）")
        log.info("批量创建 SKU 取值 {} 条，dimension={}", created, dim.code)
        return created
    }

    @Transactional
    fun updateValue(valueId: Long, req: SkuAttrValueUpsertRequest) {
        val entity = valueRepository.findById(valueId).orElseThrow { BizException("属性取值不存在") }
        val newCode = req.code.trim().uppercase()
        if (entity.code != newCode) throw BizException("属性取值编码创建后不可修改")
        entity.name = req.name.trim()
        entity.sortNo = req.sortNo
        entity.isActive = req.isActive
        entity.updatedAt = OffsetDateTime.now()
        valueRepository.save(entity)
        log.info("已更新 SKU 属性取值，id={}", valueId)
    }

    @Transactional
    fun deleteValue(valueId: Long) {
        if (!valueRepository.existsById(valueId)) throw BizException("属性取值不存在")
        valueRepository.deleteById(valueId)
        log.info("已删除 SKU 属性取值，id={}", valueId)
    }

    private fun ensureDimension(id: Long): SkuAttrDimensionEntity =
        dimensionRepository.findById(id).orElseThrow { BizException("属性维度不存在") }

    private fun assertValueCodeUnique(dimensionId: Long, code: String, excludeId: Long?) {
        val dup =
            valueRepository.findAllByDimensionIdOrderBySortNoAscIdAsc(dimensionId)
                .any { it.code.equals(code, ignoreCase = true) && it.id != excludeId }
        if (dup) throw BizException("该维度下取值编码已存在")
    }

    private fun toDimensionView(entity: SkuAttrDimensionEntity, includeValues: Boolean): SkuAttrDimensionView {
        val values =
            if (includeValues && entity.id != null) {
                valueRepository.findAllByDimensionIdOrderBySortNoAscIdAsc(entity.id!!).map { toValueView(it) }
            } else {
                emptyList()
            }
        return SkuAttrDimensionView(entity.id!!, entity.code, entity.name, entity.sortNo, entity.isActive, values)
    }

    private fun toValueView(entity: SkuAttrValueEntity) =
        SkuAttrValueView(entity.id!!, entity.dimensionId, entity.code, entity.name, entity.sortNo, entity.isActive)
}

@RestController
class AdminProductSkuSpecController(private val service: ProductSkuSpecService) {
    @GetMapping("/api/v1/admin/sku-specs/catalog")
    fun catalog(): ApiResponse<List<SkuAttrDimensionView>> = ok(service.catalogForAdmin())

    @GetMapping("/api/v1/admin/sku-specs/dimensions")
    fun listDimensions(): ApiResponse<List<SkuAttrDimensionView>> = ok(service.listDimensions())

    @GetMapping("/api/v1/admin/sku-specs/dimensions/{dimensionId}/values")
    fun listValues(@PathVariable dimensionId: Long): ApiResponse<List<SkuAttrValueView>> =
        ok(service.listValues(dimensionId))

    @PostMapping("/api/v1/admin/sku-specs/dimensions")
    fun createDimension(@Valid @RequestBody req: SkuAttrDimensionUpsertRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to service.createDimension(req)))

    @PostMapping("/api/v1/admin/sku-specs/dimensions/with-values")
    fun createDimensionWithValues(
        @Valid @RequestBody req: SkuAttrDimensionWithValuesCreateRequest,
    ): ApiResponse<Map<String, Long>> {
        val (dimId, valuesCreated) = service.createDimensionWithValues(req)
        return ok(mapOf("id" to dimId, "valuesCreated" to valuesCreated.toLong()))
    }

    @PutMapping("/api/v1/admin/sku-specs/dimensions/{id}")
    fun updateDimension(@PathVariable id: Long, @Valid @RequestBody req: SkuAttrDimensionUpsertRequest): ApiResponse<String> {
        service.updateDimension(id, req)
        return ok("updated")
    }

    @DeleteMapping("/api/v1/admin/sku-specs/dimensions/{id}")
    fun deleteDimension(@PathVariable id: Long): ApiResponse<String> {
        service.deleteDimension(id)
        return ok("deleted")
    }

    @PostMapping("/api/v1/admin/sku-specs/dimensions/{dimensionId}/values")
    fun createValue(
        @PathVariable dimensionId: Long,
        @Valid @RequestBody req: SkuAttrValueUpsertRequest,
    ): ApiResponse<Map<String, Long>> = ok(mapOf("id" to service.createValue(dimensionId, req)))

    @PostMapping("/api/v1/admin/sku-specs/dimensions/{dimensionId}/values/batch")
    fun createValuesBatch(
        @PathVariable dimensionId: Long,
        @Valid @RequestBody req: SkuAttrValueBatchCreateRequest,
    ): ApiResponse<Map<String, Int>> = ok(mapOf("created" to service.createValuesBatch(dimensionId, req)))

    @PutMapping("/api/v1/admin/sku-specs/values/{valueId}")
    fun updateValue(@PathVariable valueId: Long, @Valid @RequestBody req: SkuAttrValueUpsertRequest): ApiResponse<String> {
        service.updateValue(valueId, req)
        return ok("updated")
    }

    @DeleteMapping("/api/v1/admin/sku-specs/values/{valueId}")
    fun deleteValue(@PathVariable valueId: Long): ApiResponse<String> {
        service.deleteValue(valueId)
        return ok("deleted")
    }
}
