package com.trioForce.voyage.exchange

import org.springframework.data.jpa.repository.JpaRepository

interface ExchangeRateRepository : JpaRepository<ExchangeRateEntity, Long> {
    fun findByCurrencyCodeIgnoreCase(code: String): ExchangeRateEntity?

    fun findAllByEnabledIsTrueOrderByCurrencyCodeAsc(): List<ExchangeRateEntity>
}

interface ExchangeRateSettingsRepository : JpaRepository<ExchangeRateSettingsEntity, Long>
