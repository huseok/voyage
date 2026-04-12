package com.trioForce.voyage.product

import com.trioForce.voyage.common.BizException
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime
import java.util.Locale

/**
 * 商品服务：
 * 负责商品查询和后台商品维护。
 */
@Service
class ProductService(
    private val productRepository: ProductRepository
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
            items = result.content.map { toProductView(it, loggedIn) },
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
            items = result.content.map { toProductView(it, loggedIn = true) },
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
        return toProductView(entity, loggedIn = true)
    }

    /**
     * 查询前台商品详情（仅上架）。
     */
    fun detailPublic(id: Long, loggedIn: Boolean): ProductView {
        val it = productRepository.findById(id).orElseThrow { BizException("product not found") }
        if (!it.isActive) throw BizException("product not found")
        return toProductView(it, loggedIn)
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
        entity.isActive = req.isActive
        entity.updatedAt = OffsetDateTime.now()
        productRepository.save(entity)
    }

    private fun toProductView(it: ProductEntity, loggedIn: Boolean): ProductView =
        ProductView(
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
            isActive = it.isActive,
            price = if (loggedIn) it.price else null,
            currency = if (loggedIn) it.currency else null
        )

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
