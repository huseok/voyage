package com.trioForce.voyage.i18n

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.OffsetDateTime

interface I18nLocaleCatalogRepository : JpaRepository<I18nLocaleCatalogEntity, String> {
    /** 用于前台缓存失效：取所有目录的最大更新时间 */
    @Query("select coalesce(max(e.updatedAt), current_timestamp) from I18nLocaleCatalogEntity e")
    fun maxUpdatedAt(): OffsetDateTime
}
