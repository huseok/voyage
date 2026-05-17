package com.trioForce.voyage.product

import jakarta.persistence.*
import org.hibernate.annotations.Where
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.OffsetDateTime

/**
 * SKU 与规格选项实体：
 * - t_product_options：商品规格项（如 Color=Red）
 * - t_product_skus：可售最小单元（属性组合 + 价格/库存/重量）
 */
@Entity
@Table(name = "t_product_options")
@Where(clause = "is_deleted = false")
class ProductOptionEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "product_id", nullable = false)
    var productId: Long,
    @Column(name = "option_name", nullable = false, length = 80)
    var optionName: String,
    @Column(name = "option_value", nullable = false, length = 120)
    var optionValue: String,
    @Column(name = "sort_no", nullable = false)
    var sortNo: Int = 0,
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
@Table(name = "t_product_skus")
@Where(clause = "is_deleted = false")
class ProductSkuEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "product_id", nullable = false)
    var productId: Long,
    @Column(name = "sku_code", nullable = false, length = 64)
    var skuCode: String,
    @Column(name = "attr_json", nullable = false, columnDefinition = "text")
    var attrJson: String,
    @Column(name = "sale_price", nullable = false, precision = 12, scale = 2)
    var salePrice: BigDecimal,
    @Column(name = "stock_qty", nullable = false)
    var stockQty: Int = 0,
    @Column(name = "weight_kg", precision = 10, scale = 3)
    var weightKg: BigDecimal? = null,
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

interface ProductOptionRepository : JpaRepository<ProductOptionEntity, Long> {
    fun findAllByProductIdOrderBySortNoAscIdAsc(productId: Long): List<ProductOptionEntity>

    /**
     * 物理删除该商品下全部规格项（含历史软删行），避免全量覆盖保存时 `sku_code` 唯一约束冲突。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM t_product_options WHERE product_id = :productId", nativeQuery = true)
    fun hardDeleteAllByProductId(@Param("productId") productId: Long): Int
}

interface ProductSkuRepository : JpaRepository<ProductSkuEntity, Long> {
    fun findAllByProductIdOrderByIdAsc(productId: Long): List<ProductSkuEntity>

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "DELETE FROM t_product_skus WHERE product_id = :productId", nativeQuery = true)
    fun hardDeleteAllByProductId(@Param("productId") productId: Long): Int
}
