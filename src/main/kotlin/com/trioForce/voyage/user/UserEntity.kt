package com.trioForce.voyage.user

import jakarta.persistence.*
import org.hibernate.annotations.Where
import java.time.OffsetDateTime

@Entity
@Table(name = "t_users")
@Where(clause = "is_deleted = false")
class UserEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    // 存储 BCrypt 哈希，不保存明文密码
    @Column(name = "password_hash", nullable = false, length = 255)
    var passwordHash: String,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(name = "first_name", nullable = false, length = 80)
    var firstName: String = "",

    @Column(name = "last_name", nullable = false, length = 80)
    var lastName: String = "",

    @Column(name = "company_name", length = 200)
    var companyName: String? = null,

    /** 可选称谓：Mr / Ms / Dr 等；欧美注册常见，非必填 */
    @Column(nullable = false, length = 80)
    var salutation: String = "",

    @Column(name = "terms_accepted_at")
    var termsAcceptedAt: OffsetDateTime? = null,

    @Column(name = "terms_version", length = 32)
    var termsVersion: String? = null,

    @Column(name = "privacy_version", length = 32)
    var privacyVersion: String? = null,

    @Column(name = "admin_note", columnDefinition = "text")
    var adminNote: String? = null,

    @Column(columnDefinition = "text")
    var preferences: String? = null,

    @Column(length = 30)
    var phone: String? = null,

    @Column(length = 60)
    var country: String? = null,

    @Column(nullable = false, length = 30)
    var role: String = "CUSTOMER",

    @Column(nullable = false, length = 20)
    var status: String = "ACTIVE",

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
