package com.trioForce.voyage.product

import com.trioForce.voyage.audit.AuditLogEntity
import com.trioForce.voyage.audit.AuditLogRepository
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.tag.ProductTagEntity
import com.trioForce.voyage.tag.ProductTagRepository
import com.trioForce.voyage.tag.TagRepository
import com.trioForce.voyage.tag.TagView
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
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
    private val productMediaRepository: ProductMediaRepository,
    private val productTagRepository: ProductTagRepository,
    private val tagRepository: TagRepository,
    private val objectMapper: ObjectMapper,
    private val auditLogRepository: AuditLogRepository
) {
    /**
     * 前台分页列表：仅上架；支持国家、分类与关键词（标题、SKU、ID）。
     */
    fun listPublicPage(
        page: Int,
        size: Int,
        country: String?,
        q: String?,
        categoryId: Long?,
        tagId: Long?,
        promoOnly: Boolean,
    ): PagedProducts {
        val pageable = PageRequest.of(clampPage(page), clampSize(size), Sort.by(Sort.Direction.DESC, "id"))
        var spec: Specification<ProductEntity> = Specification.where(onlyActiveSpec())
        countrySpec(country)?.let { spec = spec.and(it) }
        categorySpec(categoryId)?.let { spec = spec.and(it) }
        tagSpec(tagId)?.let { spec = spec.and(it) }
        querySpec(q)?.let { spec = spec.and(it) }
        if (promoOnly) spec = spec.and(promoProductSpec())
        val result = productRepository.findAll(spec, pageable)
        val ids = result.content.mapNotNull { it.id }
        val mediaByProduct = batchGalleryByProductId(ids)
        val tagsByProduct = batchTagsByProductId(ids)
        return PagedProducts(
            items = result.content.map {
                val pid = it.id ?: throw IllegalStateException("product without id")
                // 批量结果缺键表示该商品无图，须显式传空列表，避免 gallery=null 再次逐条查库
                toProductView(
                    it,
                    includeMatrix = false,
                    gallery = mediaByProduct[pid] ?: emptyList(),
                    tags = tagsByProduct[pid] ?: emptyList(),
                )
            },
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
        val ids = result.content.mapNotNull { it.id }
        val mediaByProduct = batchGalleryByProductId(ids)
        val tagsByProduct = batchTagsByProductId(ids)
        return PagedProducts(
            items = result.content.map {
                val pid = it.id ?: throw IllegalStateException("product without id")
                toProductView(
                    it,
                    includeMatrix = false,
                    gallery = mediaByProduct[pid] ?: emptyList(),
                    tags = tagsByProduct[pid] ?: emptyList(),
                )
            },
            total = result.totalElements,
            page = result.number,
            size = result.size
        )
    }

    /**
     * 管理端详情：含下架商品。
     */
    fun adminDetail(id: Long): ProductView {
        val entity = productRepository.findById(id).orElseThrow { BizException("product not found") }
        val pid = entity.id!!
        val tags = batchTagsByProductId(listOf(pid))[pid] ?: emptyList()
        return toProductView(entity, includeMatrix = true, tags = tags)
    }

    /**
     * 查询前台商品详情（仅上架）；价格币种与列表一致，不要求登录。
     */
    fun detailPublic(id: Long): ProductView {
        val it = productRepository.findById(id).orElseThrow { BizException("product not found") }
        if (!it.isActive) throw BizException("product not found")
        val pid = it.id!!
        val tags = batchTagsByProductId(listOf(pid))[pid] ?: emptyList()
        return toProductView(it, includeMatrix = true, tags = tags)
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
        val id = entity.id!!
        syncProductMedia(id, req.images)
        syncProductTags(id, req.tagIds)
        return id
    }

    @Transactional
    fun update(id: Long, req: ProductAdminUpsertRequest) {
        val entity = productRepository.findById(id).orElseThrow { BizException("product not found") }
        entity.title = req.title.trim()
        entity.price = req.price
        entity.listPrice = normalizeListPrice(req.listPrice, req.price)
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
        syncProductMedia(id, req.images)
        syncProductTags(id, req.tagIds)
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

    /**
     * 列表场景批量加载图片，避免每个商品单独查询（N+1）。
     *
     * 返回的 Map 仅包含「至少有一张图」的商品；无图商品不在 Map 中，
     * 调用方须使用 `map[id] ?: emptyList()` 再传入 [toProductView]，禁止传 null（否则会再次按商品查库）。
     */
    /**
     * 批量加载商品标签，避免列表 N+1。
     * Map 缺键表示该商品无标签，调用方使用 `[id] ?: emptyList()`。
     */
    private fun batchTagsByProductId(ids: List<Long>): Map<Long, List<TagView>> {
        if (ids.isEmpty()) return emptyMap()
        val links = productTagRepository.findAllByProductIdIn(ids)
        if (links.isEmpty()) return emptyMap()
        val tagIds = links.map { it.tagId }.distinct()
        val tagEntities = tagRepository.findAllById(tagIds).associateBy { it.id!! }
        return links.groupBy { it.productId }.mapValues { (_, rows) ->
            rows.mapNotNull { tagEntities[it.tagId] }
                .filter { it.isActive }
                .sortedWith(compareBy({ it.sortNo }, { it.id ?: 0L }))
                .map { te -> TagView(te.id!!, te.code, te.name, te.sortNo, te.isActive) }
        }
    }

    /**
     * 覆盖商品标签关联。
     * - null：不改；
     * - []：清空；
     * - 非空：校验 id 存在后全量替换。
     */
    private fun syncProductTags(productId: Long, tagIds: List<Long>?) {
        if (tagIds == null) return
        val distinct = tagIds.distinct()
        if (distinct.isNotEmpty()) {
            val found = tagRepository.findAllById(distinct)
            if (found.size != distinct.size) throw BizException("invalid tag id")
        }
        productTagRepository.deleteAllByProductId(productId)
        if (distinct.isEmpty()) return
        val now = OffsetDateTime.now()
        productTagRepository.saveAll(
            distinct.map { tid ->
                ProductTagEntity(productId = productId, tagId = tid, createdAt = now)
            }
        )
    }

    private fun batchGalleryByProductId(ids: List<Long>): Map<Long, List<ProductImageView>> {
        if (ids.isEmpty()) return emptyMap()
        val rows = productMediaRepository.findAllByProductIdIn(ids)
        return rows.groupBy { it.productId }.mapValues { (_, list) ->
            list.sortedWith(compareBy({ it.sortNo }, { it.id ?: 0L }))
                .map { m -> ProductImageView(thumbUrl = m.thumbUrl, fullUrl = m.fullUrl) }
        }
    }

    /**
     * 覆盖商品图片集合。
     * - null：保留数据库现有记录；
     * - 空列表：删除全部关联媒体行；
     * - 非空：按顺序全量替换。
     */
    private fun syncProductMedia(productId: Long, images: List<ProductImageRef>?) {
        if (images == null) return
        images.forEach {
            assertSafeMediaUrl(it.thumbUrl)
            assertSafeMediaUrl(it.fullUrl)
        }
        productMediaRepository.deleteAllByProductId(productId)
        if (images.isEmpty()) return
        val now = OffsetDateTime.now()
        productMediaRepository.saveAll(
            images.mapIndexed { idx, ref ->
                ProductMediaEntity(
                    productId = productId,
                    thumbUrl = ref.thumbUrl.trim(),
                    fullUrl = ref.fullUrl.trim(),
                    sortNo = idx,
                    createdAt = now,
                )
            }
        )
    }

    private fun assertSafeMediaUrl(path: String) {
        val p = path.trim()
        if (p.isEmpty()) throw BizException("invalid media path")
        if (p.any { it.code < 32 }) throw BizException("invalid media path")
        if (!p.startsWith("/media/")) throw BizException("invalid media path (must start with /media/)")
        if (p.contains("..")) throw BizException("invalid media path")
        if (p.length > 512) throw BizException("invalid media path")
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

    private fun toProductView(
        it: ProductEntity,
        includeMatrix: Boolean,
        gallery: List<ProductImageView>? = null,
        tags: List<TagView> = emptyList(),
    ): ProductView {
        val options = if (!includeMatrix) emptyList() else productOptionRepository.findAllByProductIdOrderBySortNoAscIdAsc(it.id!!).map {
            ProductOptionView(it.optionName, it.optionValue, it.sortNo)
        }
        val skus = if (!includeMatrix) emptyList() else productSkuRepository.findAllByProductIdOrderByIdAsc(it.id!!).map {
            ProductSkuView(it.id!!, it.skuCode, it.attrJson, it.salePrice, it.stockQty, it.weightKg, it.isActive)
        }
        // gallery == null：详情/单笔查询路径，单独加载；非 null（含 emptyList）：由调用方负责批量或显式空列表
        val resolvedGallery = gallery ?: productMediaRepository.findAllByProductIdOrderBySortNoAscIdAsc(it.id!!).map { m ->
            ProductImageView(thumbUrl = m.thumbUrl, fullUrl = m.fullUrl)
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
            images = resolvedGallery,
            tags = tags,
            price = it.price,
            listPrice = it.listPrice,
            currency = it.currency
        )
    }

    /** 划线价须不低于售价；null 表示不设划线价 */
    private fun normalizeListPrice(listPrice: BigDecimal?, price: BigDecimal): BigDecimal? {
        if (listPrice == null) return null
        if (listPrice < price) throw BizException("listPrice must be >= price")
        return listPrice
    }

    /** 活动商品：已填划线价且划线价高于主档售价 */
    private fun promoProductSpec(): Specification<ProductEntity> =
        Specification { root, _, cb ->
            val lp = root.get<BigDecimal>("listPrice")
            cb.and(cb.isNotNull(lp), cb.gt(lp, root.get("price")))
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

    private fun categorySpec(categoryId: Long?): Specification<ProductEntity>? {
        if (categoryId == null) return null
        return Specification { root, _, cb -> cb.equal(root.get<Long>("categoryId"), categoryId) }
    }

    /** 关联表 [ProductTagEntity] 子查询：上架商品且绑定指定标签 */
    private fun tagSpec(tagId: Long?): Specification<ProductEntity>? {
        if (tagId == null) return null
        return Specification { root, query, cb ->
            val sq = query!!.subquery(Long::class.javaObjectType)
            val pt = sq.from(ProductTagEntity::class.java)
            sq.select(pt.get("productId"))
            sq.where(cb.equal(pt.get<Long>("tagId"), tagId))
            cb.`in`(root.get<Long>("id")).value(sq)
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
