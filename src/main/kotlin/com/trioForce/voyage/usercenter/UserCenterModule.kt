package com.trioForce.voyage.usercenter

/**
 * 用户中心模块（User Center）：
 * - 地址管理（新增、列表）
 * - 浏览记录（商品详情页可写入）
 */

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.BizException
import com.trioForce.voyage.common.ok
import com.trioForce.voyage.product.ProductLookup
import com.trioForce.voyage.product.ProductRepository
import com.trioForce.voyage.security.CurrentUser
import jakarta.persistence.*
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.hibernate.annotations.Where
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.*
import java.time.OffsetDateTime

@Entity
@Table(name = "t_user_addresses")
@Where(clause = "is_deleted = false")
class UserAddressEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "user_id", nullable = false)
    var userId: Long,
    @Column(name = "receiver_name", nullable = false, length = 100)
    var receiverName: String,
    @Column(name = "receiver_phone", nullable = false, length = 30)
    var receiverPhone: String,
    @Column(nullable = false, length = 60)
    var country: String,
    @Column(name = "address_line", nullable = false, length = 255)
    var addressLine: String,
    @Column(name = "receiver_company", length = 120)
    var receiverCompany: String? = null,
    @Column(length = 100)
    var province: String? = null,
    @Column(length = 100)
    var city: String? = null,
    @Column(name = "postal_code", length = 20)
    var postalCode: String? = null,
    @Column(name = "is_default", nullable = false)
    var isDefault: Boolean = false,
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
@Table(name = "t_browse_histories")
@Where(clause = "is_deleted = false")
class BrowseHistoryEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,
    @Column(name = "user_id", nullable = false)
    var userId: Long,
    @Column(name = "product_id", nullable = false)
    var productId: Long,
    @Column(name = "viewed_at", nullable = false)
    var viewedAt: OffsetDateTime = OffsetDateTime.now(),
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

interface UserAddressRepository : JpaRepository<UserAddressEntity, Long> {
    fun findAllByUserIdOrderByIsDefaultDescIdDesc(userId: Long): List<UserAddressEntity>

    @Query("select e from UserAddressEntity e where e.id = :id and e.userId = :userId and e.isDeleted = false")
    fun findActiveByIdAndUserId(@Param("id") id: Long, @Param("userId") userId: Long): UserAddressEntity?
}

interface BrowseHistoryRepository : JpaRepository<BrowseHistoryEntity, Long> {
    fun findAllByUserIdOrderByViewedAtDesc(userId: Long): List<BrowseHistoryEntity>
}

data class UserAddressCreateRequest(
    @field:NotBlank val receiverName: String,
    @field:NotBlank val receiverPhone: String,
    @field:NotBlank val country: String,
    @field:NotBlank val addressLine: String,
    val receiverCompany: String? = null,
    val province: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val isDefault: Boolean = false,
)

data class UserAddressUpdateRequest(
    @field:NotBlank val receiverName: String,
    @field:NotBlank val receiverPhone: String,
    @field:NotBlank val country: String,
    @field:NotBlank val addressLine: String,
    val receiverCompany: String? = null,
    val province: String? = null,
    val city: String? = null,
    val postalCode: String? = null,
    val isDefault: Boolean = false,
)

data class UserAddressView(
    val id: Long,
    val receiverName: String,
    val receiverPhone: String,
    val receiverCompany: String?,
    val country: String,
    val addressLine: String,
    val province: String?,
    val city: String?,
    val postalCode: String?,
    val isDefault: Boolean,
)

data class BrowseHistoryCreateRequest(val productId: String)
data class BrowseHistoryView(val id: Long, val productId: String, val viewedAt: OffsetDateTime)

@Service
class UserCenterService(
    private val addressRepository: UserAddressRepository,
    private val browseHistoryRepository: BrowseHistoryRepository,
    private val productRepository: ProductRepository,
    private val productLookup: ProductLookup,
) {
    fun listAddresses(): List<UserAddressView> = addressRepository.findAllByUserIdOrderByIsDefaultDescIdDesc(CurrentUser.userId()).map {
        UserAddressView(
            id = it.id!!,
            receiverName = it.receiverName,
            receiverPhone = it.receiverPhone,
            receiverCompany = it.receiverCompany,
            country = it.country,
            addressLine = it.addressLine,
            province = it.province,
            city = it.city,
            postalCode = it.postalCode,
            isDefault = it.isDefault,
        )
    }

    private fun clearDefaultExcept(userId: Long, exceptId: Long?, now: OffsetDateTime) {
        addressRepository.findAllByUserIdOrderByIsDefaultDescIdDesc(userId).forEach { row ->
            if (row.isDefault && (exceptId == null || row.id != exceptId)) {
                row.isDefault = false
                row.updatedAt = now
                addressRepository.save(row)
            }
        }
    }

    @Transactional
    fun createAddress(req: UserAddressCreateRequest): Long {
        val userId = CurrentUser.userId()
        val now = OffsetDateTime.now()
        if (req.isDefault) {
            clearDefaultExcept(userId, null, now)
        }
        return addressRepository.save(
            UserAddressEntity(
                userId = userId,
                receiverName = req.receiverName.trim(),
                receiverPhone = req.receiverPhone.trim(),
                country = req.country.trim(),
                addressLine = req.addressLine.trim(),
                receiverCompany = req.receiverCompany?.trim()?.takeUnless { it.isEmpty() },
                province = req.province?.trim()?.takeUnless { it.isEmpty() },
                city = req.city?.trim()?.takeUnless { it.isEmpty() },
                postalCode = req.postalCode?.trim(),
                isDefault = req.isDefault,
                createdAt = now,
                updatedAt = now,
            ),
        ).id!!
    }

    @Transactional
    fun updateAddress(id: Long, req: UserAddressUpdateRequest) {
        val userId = CurrentUser.userId()
        val row = addressRepository.findActiveByIdAndUserId(id, userId) ?: throw BizException("address not found")
        val now = OffsetDateTime.now()
        if (req.isDefault) {
            clearDefaultExcept(userId, row.id, now)
        }
        row.receiverName = req.receiverName.trim()
        row.receiverPhone = req.receiverPhone.trim()
        row.country = req.country.trim()
        row.addressLine = req.addressLine.trim()
        row.receiverCompany = req.receiverCompany?.trim()?.takeUnless { it.isEmpty() }
        row.province = req.province?.trim()?.takeUnless { it.isEmpty() }
        row.city = req.city?.trim()?.takeUnless { it.isEmpty() }
        row.postalCode = req.postalCode?.trim()
        row.isDefault = req.isDefault
        row.updatedAt = now
        addressRepository.save(row)
    }

    @Transactional
    fun deleteAddress(id: Long) {
        val userId = CurrentUser.userId()
        val row = addressRepository.findActiveByIdAndUserId(id, userId) ?: throw BizException("address not found")
        val now = OffsetDateTime.now()
        row.isDeleted = true
        row.deletedAt = now
        row.deletedBy = userId
        row.updatedAt = now
        addressRepository.save(row)
        if (row.isDefault) {
            val rest = addressRepository.findAllByUserIdOrderByIsDefaultDescIdDesc(userId)
            if (rest.isNotEmpty()) {
                val next = rest.first()
                next.isDefault = true
                next.updatedAt = now
                addressRepository.save(next)
            }
        }
    }

    @Transactional
    fun setDefaultAddress(id: Long) {
        val userId = CurrentUser.userId()
        val row = addressRepository.findActiveByIdAndUserId(id, userId) ?: throw BizException("address not found")
        val now = OffsetDateTime.now()
        clearDefaultExcept(userId, row.id, now)
        row.isDefault = true
        row.updatedAt = now
        addressRepository.save(row)
    }

    fun listBrowseHistories(): List<BrowseHistoryView> {
        val rows = browseHistoryRepository.findAllByUserIdOrderByViewedAtDesc(CurrentUser.userId())
        val ids = rows.map { it.productId }.toSet()
        val pub = productRepository.findAllById(ids).associate { it.id!! to it.publicId }
        return rows.map {
            BrowseHistoryView(it.id!!, pub[it.productId] ?: it.productId.toString(), it.viewedAt)
        }
    }

    @Transactional
    fun addBrowseHistory(req: BrowseHistoryCreateRequest): Long {
        val p = productLookup.requireEntityByClientKey(req.productId)
        return browseHistoryRepository.save(
            BrowseHistoryEntity(
                userId = CurrentUser.userId(),
                productId = p.id!!,
                viewedAt = OffsetDateTime.now()
            )
        ).id!!
    }
}

@RestController
class UserCenterController(private val userCenterService: UserCenterService) {
    /** 用户中心：地址列表。 */
    @GetMapping("/api/v1/user/addresses")
    fun listAddresses(): ApiResponse<List<UserAddressView>> = ok(userCenterService.listAddresses())

    /** 用户中心：新增地址。 */
    @PostMapping("/api/v1/user/addresses")
    fun createAddress(@Valid @RequestBody req: UserAddressCreateRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to userCenterService.createAddress(req)))

    /** 用户中心：更新地址。 */
    @PutMapping("/api/v1/user/addresses/{id}")
    fun updateAddress(@PathVariable id: Long, @Valid @RequestBody req: UserAddressUpdateRequest): ApiResponse<String> {
        userCenterService.updateAddress(id, req)
        return ok("updated")
    }

    /** 用户中心：删除地址（软删除）。 */
    @DeleteMapping("/api/v1/user/addresses/{id}")
    fun deleteAddress(@PathVariable id: Long): ApiResponse<String> {
        userCenterService.deleteAddress(id)
        return ok("deleted")
    }

    /** 用户中心：设为默认地址。 */
    @PatchMapping("/api/v1/user/addresses/{id}/default")
    fun setDefaultAddress(@PathVariable id: Long): ApiResponse<String> {
        userCenterService.setDefaultAddress(id)
        return ok("default set")
    }

    /** 用户中心：浏览历史列表。 */
    @GetMapping("/api/v1/user/browse-histories")
    fun listBrowseHistories(): ApiResponse<List<BrowseHistoryView>> = ok(userCenterService.listBrowseHistories())

    /** 商品详情页：记录浏览行为。 */
    @PostMapping("/api/v1/user/browse-histories")
    fun addBrowseHistory(@Valid @RequestBody req: BrowseHistoryCreateRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to userCenterService.addBrowseHistory(req)))
}
