package com.trioForce.voyage.exchange

import com.trioForce.voyage.common.logging.LogUtil
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 定时拉取汇率行情；实际刷新间隔由库内配置与各币种冻结策略决定。
 * 调度器每 30 分钟检查一次，满足间隔的币种才会请求外部 API。
 */
@Component
class ExchangeRateScheduler(
    private val exchangeRateService: ExchangeRateService,
) {
    private val log = LogUtil.logger<ExchangeRateScheduler>()

    @Scheduled(fixedDelayString = "\${voyage.exchange.scheduler-check-ms:1800000}", initialDelay = 60_000)
    fun checkAndRefresh() {
        try {
            exchangeRateService.scheduledRefresh()
        } catch (e: Exception) {
            log.warn("定时汇率刷新失败: {}", e.message)
        }
    }
}
