package com.trioForce.voyage.product

import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.logging.LogUtil
import org.springframework.stereotype.Component

/**
 * 将「接口层商品键」（路径变量、JSON 里的 `productId`）解析为 [ProductEntity]。
 *
 * 设计背景：对外主键为 [ProductEntity.publicId]（雪花字符串），库内关联仍用自增 [ProductEntity.id]；
 * 为兼容旧书签/测试数据，仍允许纯数字命中历史 `id`（见 [requireEntityByClientKey] 顺序）。
 *
 * 日志：命中 legacy 数字 id 时打 **DEBUG**，便于观察迁移残留，正常流量不刷屏。
 */
@Component
class ProductLookup(
    private val productRepository: ProductRepository,
) {
    private val log = LogUtil.logger<ProductLookup>()

    /**
     * 解析前台/接口传入的商品键：
     * 1. 先按 [ProductRepository.findByPublicId] 精确匹配；
     * 2. 再尝试将整串解析为 Long 并按自增主键查找（legacy）；
     * 3. 均失败则 [BizException]。
     */
    fun requireEntityByClientKey(raw: String): ProductEntity {
        val key = raw.trim()
        if (key.isEmpty()) throw BizException("product not found")
        val byPublic = productRepository.findByPublicId(key)
        if (byPublic.isPresent) return byPublic.get()
        val legacy = key.toLongOrNull()
        if (legacy != null) {
            val legacyEntity = productRepository.findById(legacy).orElse(null)
            if (legacyEntity != null) {
                log.debug("product resolved by legacy numeric id={}", legacy)
                return legacyEntity
            }
        }
        throw BizException("product not found")
    }

    /** 仅需要内部主键时的便捷封装（购物车行、订单行等 FK 场景）。 */
    fun requireInternalId(raw: String): Long = requireEntityByClientKey(raw).id!!
}
