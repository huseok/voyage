package com.trioForce.voyage.product

import com.trioForce.voyage.audit.AuditLogEntity
import com.trioForce.voyage.audit.AuditLogRepository
import com.trioForce.voyage.common.BizException
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.Locale
import java.util.TreeMap

/**
 * 商品服务：
 * 负责商品查询和后台商品维护。
 */
@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val productOptionRepository: ProductOptionRepository,
    private val productSkuRepository: ProductSkuRepository,
    private val objectMapper: ObjectMapper,
    private val auditLogRepository: AuditLogRepository
) {
    /**
     * 前台分页列表：仅上架；支持国家与关键词（标题、SKU、ID）。
     */
    fun listPublicPage(
        page: Int,
        size: Int,
        country: String?,
        q: String?,
        loggedIn: Boolean
    ): PagedProducts {
        val pageable = PageRequest.of(clampPage(page), clampSize(size), Sort.by(Sort.Direction.DESC, "id"))
        var spec: Specification<ProductEntity> = Specification.where(onlyActiveSpec())
        countrySpec(country)?.let { spec = spec.and(it) }
        querySpec(q)?.let { spec = spec.and(it) }
        val result = productRepository.findAll(spec, pageable)
        return PagedProducts(
            items = result.content.map { toProductView(it, loggedIn, includeMatrix = false) },
            total = result.totalElements,
            page = result.number,
            size = result.size
        )
    }

    /**
     * 管理端分页列表：含下架商品；可选上架筛选与关键词。
     *
     * @param activeFilter null 表示全部，true/false 表示仅上架/仅下架
     */
    fun listAdminPage(page: Int, size: Int, q: String?, activeFilter: Boolean?): PagedProducts {
        val pageable = PageRequest.of(clampPage(page), clampSize(size), Sort.by(Sort.Direction.DESC, "id"))
        var spec: Specification<ProductEntity> = Specification.where(Specification { _, _, cb -> cb.conjunction() })
        activeFilter?.let { active ->
            spec = spec.and { root, _, cb -> cb.equal(root.get<Boolean>("isActive"), active) }
        }
        querySpec(q)?.let { spec = spec.and(it) }
        val result = productRepository.findAll(spec, pageable)
        return PagedProducts(
            items = result.content.map { toProductView(it, loggedIn = true, includeMatrix = false) },
            total = result.totalElements,
            page = result.number,
            size = result.size
        )
    }

    /**
     * 管理端详情：含下架商品，始终返回价格。
     */
    fun adminDetail(id: Long): ProductView {
        val entity = productRepository.findById(id).orElseThrow { BizException("product not found") }
        return toProductView(entity, loggedIn = true, includeMatrix = true)
    }

    /**
     * 查询前台商品详情（仅上架）。
     */
    fun detailPublic(id: Long, loggedIn: Boolean): ProductView {
        val it = productRepository.findById(id).orElseThrow { BizException("product not found") }
        if (!it.isActive) throw BizException("product not found")
        return toProductView(it, loggedIn, includeMatrix = true)
    }

    @Transactional
    fun create(req: ProductAdminUpsertRequest): Long {
        val now = OffsetDateTime.now()
        val entity = productRepository.save(
            ProductEntity(
                title = req.title.trim(),
                price = req.price,
                currency = req.currency.uppercase(),
                moq = req.moq,
                description = req.description?.trim(),
                skuCode = req.skuCode?.trim(),
                hsCode = req.hsCode?.trim(),
                unit = req.unit?.trim(),
                incoterm = req.incoterm?.trim()?.uppercase(),
                originCountry = req.originCountry?.trim(),
                leadTimeDays = req.leadTimeDays,
                weightKg = req.weightKg,
                categoryId = req.categoryId,
                shippingTemplateId = req.shippingTemplateId,
                isActive = req.isActive,
                createdAt = now,
                updatedAt = now
            )
        )
        return entity.id!!
    }

    @Transactional
    fun update(id: Long, req: ProductAdminUpsertRequest) {
        val entity = productRepository.findById(id).orElseThrow { BizException("product not found") }
        entity.title = req.title.trim()
        entity.price = req.price
        entity.currency = req.currency.uppercase()
        entity.moq = req.moq
        entity.description = req.description?.trim()
        entity.skuCode = req.skuCode?.trim()
        entity.hsCode = req.hsCode?.trim()
        entity.unit = req.unit?.trim()
        entity.incoterm = req.incoterm?.trim()?.uppercase()
        entity.originCountry = req.originCountry?.trim()
        entity.leadTimeDays = req.leadTimeDays
        entity.weightKg = req.weightKg
        entity.categoryId = req.categoryId
        entity.shippingTemplateId = req.shippingTemplateId
        entity.isActive = req.isActive
        entity.updatedAt = OffsetDateTime.now()
        productRepository.save(entity)
    }

    @Transactional
    /** 后台批量更新商品上下架状态。 */
    fun bulkUpdateStatus(ids: List<Long>, isActive: Boolean): Int {
        if (ids.isEmpty()) return 0
        val rows = productRepository.findAllById(ids)
        val now = OffsetDateTime.now()
        rows.forEach {
            it.isActive = isActive
            it.updatedAt = now
        }
        productRepository.saveAll(rows)
        auditLogRepository.save(
            AuditLogEntity(
                actorUserId = null,
                actorRole = "ADMIN",
                actionCode = if (isActive) "PRODUCT_BULK_ON_SHELF" else "PRODUCT_BULK_OFF_SHELF",
                entityType = "PRODUCT",
                entityId = rows.joinToString(",") { it.id.toString() },
                detailJson = """{"count":${rows.size},"targetStatus":$isActive}""",
                createdAt = now
            )
        )
        return rows.size
    }

    fun getSkuMatrix(productId: Long): ProductSkuMatrixView {
        productRepository.findById(productId).orElseThrow { BizException("product not found") }
        val options = productOptionRepository.findAllByProductIdOrderBySortNoAscIdAsc(productId).map {
            ProductOptionView(it.optionName, it.optionValue, it.sortNo)
        }
        val skus = productSkuRepository.findAllByProductIdOrderByIdAsc(productId).map {
            ProductSkuView(it.id!!, it.skuCode, it.attrJson, it.salePrice, it.stockQty, it.weightKg, it.isActive)
        }
        return ProductSkuMatrixView(productId, options, skus)
    }

    @Transactional
    fun upsertSkuMatrix(productId: Long, req: ProductSkuMatrixUpsertRequest) {
        productRepository.findById(productId).orElseThrow { BizException("product not found") }
        // 基本校验：attrJson 必须是合法 JSON 对象，避免后续前端解析失败
        val skuCodeSet = mutableSetOf<String>()
        val attrSet = mutableSetOf<String>()
        req.skus.forEach {
            val normalizedSku = it.skuCode.trim().uppercase()
            if (normalizedSku.isBlank()) throw BizException("skuCode cannot be blank")
            if (!skuCodeSet.add(normalizedSku)) throw BizException("duplicate skuCode: $normalizedSku")
            val node = objectMapper.readTree(it.attrJson)
            if (!node.isObject) throw BizException("attrJson must be object json")
            val attrs = objectMapper.convertValue(node, Map::class.java)
                .mapKeys { entry -> entry.key.toString() }
                .mapValues { entry -> entry.value?.toString() ?: "" }
                .filterKeys { key -> key.isNotBlank() }
            val canonical = canonicalAttrKey(attrs)
            if (canonical.isBlank()) throw BizException("attrJson cannot be empty object")
            if (!attrSet.add(canonical)) throw BizException("duplicate sku attrs: $canonical")
        }
        productOptionRepository.deleteAllByProductId(productId)
        productSkuRepository.deleteAllByProductId(productId)
        val now = OffsetDateTime.now()
        val options = req.options.map {
            ProductOptionEntity(
                productId = productId,
                optionName = it.optionName.trim(),
                optionValue = it.optionValue.trim(),
                sortNo = it.sortNo,
                createdAt = now,
                updatedAt = now
            )
        }
        val skus = req.skus.map {
            ProductSkuEntity(
                productId = productId,
                skuCode = it.skuCode.trim().uppercase(),
                attrJson = it.attrJson.trim(),
                salePrice = it.salePrice,
                stockQty = it.stockQty,
                weightKg = it.weightKg,
                isActive = it.isActive,
                createdAt = now,
                updatedAt = now
            )
        }
        productOptionRepository.saveAll(options)
        productSkuRepository.saveAll(skus)
    }

    private fun canonicalAttrKey(attrs: Map<String, String>): String {
        val sorted = TreeMap<String, String>()
        attrs.forEach { (k, v) ->
            val key = k.trim()
            val value = v.trim()
            if (key.isNotBlank() && value.isNotBlank()) sorted[key] = value
        }
        return sorted.entries.joinToString("|") { "${it.key}:${it.value}" }
    }

    private fun toProductView(it: ProductEntity, loggedIn: Boolean, includeMatrix: Boolean): ProductView {
        val options = if (!includeMatrix) emptyList() else productOptionRepository.findAllByProductIdOrderBySortNoAscIdAsc(it.id!!).map {
            ProductOptionView(it.optionName, it.optionValue, it.sortNo)
        }
        val skus = if (!includeMatrix) emptyList() else productSkuRepository.findAllByProductIdOrderByIdAsc(it.id!!).map {
            ProductSkuView(it.id!!, it.skuCode, it.attrJson, it.salePrice, it.stockQty, it.weightKg, it.isActive)
        }
        return ProductView(
            id = it.id!!,
            title = it.title,
            moq = it.moq,
            description = it.description,
            skuCode = it.skuCode,
            hsCode = it.hsCode,
            unit = it.unit,
            incoterm = it.incoterm,
            originCountry = it.originCountry,
            leadTimeDays = it.leadTimeDays,
            weightKg = it.weightKg,
            categoryId = it.categoryId,
            shippingTemplateId = it.shippingTemplateId,
            isActive = it.isActive,
            options = options,
            skus = skus,
            price = if (loggedIn) it.price else null,
            currency = if (loggedIn) it.currency else null
        )
    }

    private fun onlyActiveSpec(): Specification<ProductEntity> =
        Specification { root, _, cb -> cb.isTrue(root.get("isActive")) }

    private fun countrySpec(country: String?): Specification<ProductEntity>? {
        if (country.isNullOrBlank()) return null
        val c = country.trim().lowercase(Locale.ROOT)
        return Specification { root, _, cb ->
            cb.equal(cb.lower(root.get("originCountry")), c)
        }
    }

    private fun querySpec(q: String?): Specification<ProductEntity>? {
        if (q.isNullOrBlank()) return null
        val term = q.trim()
        val lowered = term.lowercase(Locale.ROOT)
        val like = "%$lowered%"
        val idLong = runCatching { term.toLong() }.getOrNull()
        return Specification { root, _, cb ->
            val title = cb.like(cb.lower(root.get("title")), like)
            val sku = cb.like(cb.lower(cb.coalesce(root.get("skuCode"), "")), like)
            val idMatch = idLong?.let { cb.equal(root.get<Long>("id"), it) } ?: cb.disjunction()
            cb.or(title, sku, idMatch)
        }
    }

    private fun clampPage(page: Int): Int = page.coerceAtLeast(0)

    private fun clampSize(size: Int): Int = size.coerceIn(1, 100)
}
