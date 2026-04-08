package com.trioForce.voyage.user

import jakarta.persistence.*
import java.time.OffsetDateTime

@Entity
@Table(name = "t_users")
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
    var updatedAt: OffsetDateTime = OffsetDateTime.now()
)
