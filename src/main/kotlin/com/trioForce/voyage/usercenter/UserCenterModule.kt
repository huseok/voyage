package com.trioForce.voyage.usercenter

/**
 * 用户中心模块（User Center）：
 * - 地址管理（新增、列表）
 * - 浏览记录（商品详情页可写入）
 */

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import com.trioForce.voyage.security.CurrentUser
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
}

interface BrowseHistoryRepository : JpaRepository<BrowseHistoryEntity, Long> {
    fun findAllByUserIdOrderByViewedAtDesc(userId: Long): List<BrowseHistoryEntity>
}

data class UserAddressCreateRequest(
    @field:NotBlank val receiverName: String,
    @field:NotBlank val receiverPhone: String,
    @field:NotBlank val country: String,
    @field:NotBlank val addressLine: String,
    val postalCode: String? = null,
    val isDefault: Boolean = false
)

data class UserAddressView(
    val id: Long,
    val receiverName: String,
    val receiverPhone: String,
    val country: String,
    val addressLine: String,
    val postalCode: String?,
    val isDefault: Boolean
)

data class BrowseHistoryCreateRequest(val productId: Long)
data class BrowseHistoryView(val id: Long, val productId: Long, val viewedAt: OffsetDateTime)

@Service
class UserCenterService(
    private val addressRepository: UserAddressRepository,
    private val browseHistoryRepository: BrowseHistoryRepository
) {
    fun listAddresses(): List<UserAddressView> = addressRepository.findAllByUserIdOrderByIsDefaultDescIdDesc(CurrentUser.userId()).map {
        UserAddressView(it.id!!, it.receiverName, it.receiverPhone, it.country, it.addressLine, it.postalCode, it.isDefault)
    }

    @Transactional
    fun createAddress(req: UserAddressCreateRequest): Long {
        val userId = CurrentUser.userId()
        val now = OffsetDateTime.now()
        if (req.isDefault) {
            // 若新增为默认地址，先取消其他默认标记。
            addressRepository.findAllByUserIdOrderByIsDefaultDescIdDesc(userId).forEach { row ->
                if (row.isDefault) {
                    row.isDefault = false
                    row.updatedAt = now
                    addressRepository.save(row)
                }
            }
        }
        return addressRepository.save(
            UserAddressEntity(
                userId = userId,
                receiverName = req.receiverName.trim(),
                receiverPhone = req.receiverPhone.trim(),
                country = req.country.trim(),
                addressLine = req.addressLine.trim(),
                postalCode = req.postalCode?.trim(),
                isDefault = req.isDefault,
                createdAt = now,
                updatedAt = now
            )
        ).id!!
    }

    fun listBrowseHistories(): List<BrowseHistoryView> =
        browseHistoryRepository.findAllByUserIdOrderByViewedAtDesc(CurrentUser.userId()).map {
            BrowseHistoryView(it.id!!, it.productId, it.viewedAt)
        }

    @Transactional
    fun addBrowseHistory(req: BrowseHistoryCreateRequest): Long =
        browseHistoryRepository.save(
            BrowseHistoryEntity(
                userId = CurrentUser.userId(),
                productId = req.productId,
                viewedAt = OffsetDateTime.now()
            )
        ).id!!
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

    /** 用户中心：浏览历史列表。 */
    @GetMapping("/api/v1/user/browse-histories")
    fun listBrowseHistories(): ApiResponse<List<BrowseHistoryView>> = ok(userCenterService.listBrowseHistories())

    /** 商品详情页：记录浏览行为。 */
    @PostMapping("/api/v1/user/browse-histories")
    fun addBrowseHistory(@Valid @RequestBody req: BrowseHistoryCreateRequest): ApiResponse<Map<String, Long>> =
        ok(mapOf("id" to userCenterService.addBrowseHistory(req)))
}
