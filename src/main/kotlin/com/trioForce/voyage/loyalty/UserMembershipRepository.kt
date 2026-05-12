package com.trioForce.voyage.loyalty

import org.springframework.data.jpa.repository.JpaRepository

interface UserMembershipRepository : JpaRepository<UserMembershipEntity, Long>
