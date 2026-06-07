package com.trioForce.voyage.payment

import org.springframework.data.jpa.repository.JpaRepository

interface PaymentSettingsRepository : JpaRepository<PaymentSettingsEntity, Long>
