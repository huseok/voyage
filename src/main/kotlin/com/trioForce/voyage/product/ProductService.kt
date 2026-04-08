package com.trioForce.voyage.product

import com.trioForce.voyage.common.BizException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

/**
 * 商品服务：
 * 负责商品查询和后台商品维护。
 */
@Service
class ProductService(
    private val productRepository: ProductRepository
) {
    /**
     * 查询前台商品列表。
     *
     * @param loggedIn 是否已登录，用于控制价格字段返回
     * @return 商品列表
     */
    fun listPublic(loggedIn: Boolean): List<ProductView> {
        return productRepository.findAllByIsActiveTrue().map {
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
        }
    }

    /**
     * 查询前台商品详情。
     *
     * @param id 商品 ID
     * @param loggedIn 是否已登录
     * @return 商品详情
     */
    fun detailPublic(id: Long, loggedIn: Boolean): ProductView {
        val it = productRepository.findById(id).orElseThrow { BizException("product not found") }
        if (!it.isActive) throw BizException("product not found")
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
            isActive = it.isActive,
            price = if (loggedIn) it.price else null,
            currency = if (loggedIn) it.currency else null
        )
    }

    /**
     * 后台创建商品。
     *
     * @param req 商品参数
     * @return 新商品 ID
     */
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

    /**
     * 后台更新商品。
     *
     * @param id 商品 ID
     * @param req 商品参数
     */
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
}
