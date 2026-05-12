package com.trioForce.voyage.user

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.OffsetDateTime
import java.util.Optional

interface UserRepository : JpaRepository<UserEntity, Long>, JpaSpecificationExecutor<UserEntity> {
    fun findByEmail(email: String): Optional<UserEntity>

    @Query("SELECT COUNT(u) FROM UserEntity u WHERE u.createdAt >= :since")
    fun countCreatedSince(@Param("since") since: OffsetDateTime): Long
}
