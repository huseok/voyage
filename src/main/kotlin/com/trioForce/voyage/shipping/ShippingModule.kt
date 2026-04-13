package com.trioForce.voyage.shipping

/**
 * 运费模板模块（Shipping）：
 * - 模板：定义计费模式（按重量/固定运费）
 * - 规则：定义分区与首重/续重费用
 *
 * 该模块供商品绑定模板、购物车/结算运费估算、后台模板管理复用。
 */

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.Where
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.math.BigDecimal
import java.time.OffsetDateTime

@Entity
@Table(name = "t_shipping_templates")
@Where(clause = "is_deleted = false")
class ShippingTemplateEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "template_name", nullable = false, length = 120)
    var templateName: String,
    @Column(name = "billing_mode", nullable = false, length = 30)
    var billingMode: String = "BY_WEIGHT",
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
@Table(name = "t_shipping_template_rules")
@Where(clause = "is_deleted = false")
class ShippingTemplateRuleEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "template_id", nullable = false)
    var templateId: Long,
    @Column(name = "region_code", nullable = false, length = 40)
    var regionCode: String = "GLOBAL",
    @Column(name = "first_weight_kg", nullable = false, precision = 10, scale = 3)
    var firstWeightKg: BigDecimal = BigDecimal.ZERO,
    @Column(name = "first_fee", nullable = false, precision = 12, scale = 2)
    var firstFee: BigDecimal = BigDecimal.ZERO,
    @Column(name = "additional_weight_kg", nullable = false, precision = 10, scale = 3)
    var additionalWeightKg: BigDecimal = BigDecimal.ONE,
    @Column(name = "additional_fee", nullable = false, precision = 12, scale = 2)
    var additionalFee: BigDecimal = BigDecimal.ZERO,
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

interface ShippingTemplateRepository : JpaRepository<ShippingTemplateEntity, Long>
interface ShippingTemplateRuleRepository : JpaRepository<ShippingTemplateRuleEntity, Long> {
    /** 规则按 sort_no 排序，确保运费命中逻辑可预测。 */
    fun findAllByTemplateIdOrderBySortNoAscIdAsc(templateId: Long): List<ShippingTemplateRuleEntity>
}

data class ShippingTemplateCreateRequest(
    @field:NotBlank val templateName: String,
    @field:NotBlank val billingMode: String = "BY_WEIGHT"
)
data class ShippingTemplateRuleCreateRequest(
    @field:DecimalMin("0.0") val firstWeightKg: BigDecimal,
    @field:DecimalMin("0.0") val firstFee: BigDecimal,
    @field:DecimalMin("0.001") val additionalWeightKg: BigDecimal,
    @field:DecimalMin("0.0") val additionalFee: BigDecimal,
    val regionCode: String = "GLOBAL",
    val sortNo: Int = 0
)
data class ShippingTemplateView(
    val id: Long,
    val templateName: String,
    val billingMode: String,
    val isActive: Boolean
)
data class ShippingTemplateRuleView(
    val id: Long,
    val templateId: Long,
    val regionCode: String,
    val firstWeightKg: BigDecimal,
    val firstFee: BigDecimal,
    val additionalWeightKg: BigDecimal,
    val additionalFee: BigDecimal,
    val sortNo: Int
)

@Service
class ShippingService(
    private val templateRepository: ShippingTemplateRepository,
    private val ruleRepository: ShippingTemplateRuleRepository
) {
    /** 前台/后台模板列表。 */
    fun listTemplates(): List<ShippingTemplateView> = templateRepository.findAll().map {
        ShippingTemplateView(it.id!!, it.templateName, it.billingMode, it.isActive)
    }

    /** 单模板规则列表。 */
    fun listRules(templateId: Long): List<ShippingTemplateRuleView> =
        ruleRepository.findAllByTemplateIdOrderBySortNoAscIdAsc(templateId).map {
            ShippingTemplateRuleView(
                it.id!!,
                it.templateId,
                it.regionCode,
                it.firstWeightKg,
                it.firstFee,
                it.additionalWeightKg,
                it.additionalFee,
                it.sortNo
            )
        }

    @Transactional
    /** 后台创建运费模板。 */
    fun createTemplate(req: ShippingTemplateCreateRequest): Long {
        val now = OffsetDateTime.now()
        return templateRepository.save(
            ShippingTemplateEntity(
                templateName = req.templateName.trim(),
                billingMode = req.billingMode.trim().uppercase(),
                createdAt = now,
                updatedAt = now
            )
        ).id!!
    }

    @Transactional
    /** 后台为模板创建规则。 */
    fun createRule(templateId: Long, req: ShippingTemplateRuleCreateRequest): Long =
        ruleRepository.save(
            ShippingTemplateRuleEntity(
                templateId = templateId,
                regionCode = req.regionCode.trim().uppercase(),
                firstWeightKg = req.firstWeightKg,
                firstFee = req.firstFee,
                additionalWeightKg = req.additionalWeightKg,
                additionalFee = req.additionalFee,
                sortNo = req.sortNo
            )
        ).id!!

    @Transactional
    /** 删除模板规则。 */
    fun deleteRule(ruleId: Long) {
        val row = ruleRepository.findById(ruleId).orElseThrow { IllegalArgumentException("rule not found") }
        row.isDeleted = true
        row.deletedAt = OffsetDateTime.now()
        row.updatedAt = OffsetDateTime.now()
        ruleRepository.save(row)
    }
}

@RestController
class ShippingController(private val shippingService: ShippingService) {
    /** 前台可读的模板列表（用于展示与调试）。 */
    @GetMapping("/api/v1/shipping/templates")
    fun listTemplates(): ApiResponse<List<ShippingTemplateView>> = ok(shippingService.listTemplates())

    /** 查询某模板规则。 */
    @GetMapping("/api/v1/shipping/templates/{id}/rules")
    fun listRules(@PathVariable id: Long): ApiResponse<List<ShippingTemplateRuleView>> = ok(shippingService.listRules(id))

    /** 后台新增模板。 */
    @PostMapping("/api/v1/admin/shipping/templates")
    fun createTemplate(@Valid @RequestBody req: ShippingTemplateCreateRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to shippingService.createTemplate(req)))

    /** 后台新增模板规则。 */
    @PostMapping("/api/v1/admin/shipping/templates/{id}/rules")
    fun createRule(@PathVariable id: Long, @Valid @RequestBody req: ShippingTemplateRuleCreateRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to shippingService.createRule(id, req)))

    /** 后台删除运费规则。 */
    @DeleteMapping("/api/v1/admin/shipping/rules/{ruleId}")
    fun deleteRule(@PathVariable ruleId: Long): ApiResponse<String> {
        shippingService.deleteRule(ruleId)
        return ok("deleted")
    }
}
