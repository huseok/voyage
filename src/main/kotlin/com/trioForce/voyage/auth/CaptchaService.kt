package com.trioForce.voyage.auth

import com.trioForce.voyage.common.logging.LogUtil
import com.wf.captcha.SpecCaptcha
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 注册用图形验证码服务。
 *
 * - **生成**：基于 [EasyCaptcha](https://github.com/whvcse/EasyCaptcha)（MIT）生成 PNG，置于内存 [store]。
 * - **有效期**：默认 [ttlSeconds] 秒内有效；过期条目由 [purgeExpired] 清理。
 * - **一次性**：[validateAndConsume] 成功或失败后均会从 [store] 移除该 `captchaId`（取出即消费），防止重复使用。
 * - **容量**：超过 [maxEntries] 时 [trimOverflow] 会丢弃一批最老的键（先清理过期再裁减）。
 *
 * **日志约定**：只记录 `captchaId` 前缀或长度等业务辅助信息，**绝不**打印正确答案或用户输入的验证码明文。
 */
@Service
class CaptchaService(
    /** 验证码存活时间（秒），过期后校验失败。 */
    private val ttlSeconds: Long = 120,
    /** 内存中最多保留的待发验证码条数上限（近似），用于防止内存无限增长。 */
    private val maxEntries: Int = 8000,
) {
    /** 本服务专用 Logger，勿改用 `println` / `System.out`。 */
    private val log = LogUtil.logger<CaptchaService>()

    /** 单条验证码记录：正确答案（仅内存）与绝对过期时刻。 */
    data class CaptchaRecord(val answer: String, val expiresAt: Instant)

    /** captchaId → 记录；进程内内存结构，重启即清空（注册页需重新拉验证码）。 */
    private val store = ConcurrentHashMap<String, CaptchaRecord>()

    /**
     * 签发新的图形验证码，供前端展示并在注册请求中回传 [CaptchaResponse.captchaId] 与用户所见字符。
     *
     * @return 包含可直接作为 `<img src>` 的 Base64 数据 URI 与本次会话 id。
     */
    fun create(): CaptchaResponse {
        purgeExpired()
        trimOverflow()
        val captcha = SpecCaptcha(WIDTH, HEIGHT, CODE_LEN)
        val answer = captcha.text().trim().uppercase()
        val id = UUID.randomUUID().toString().replace("-", "")
        store[id] = CaptchaRecord(answer = answer, expiresAt = Instant.now().plusSeconds(ttlSeconds))

        /*
         * 前端 `<img src>` 需要合法的 data URI。
         * EasyCaptcha 的 [SpecCaptcha.toBase64] 在常见版本中**已经**返回 `data:image/png;base64,...`；
         * 若再拼接一层前缀，会得到非法 URI，浏览器无法解码（表现为注册页「验证码图不显示」）。
         */
        val raw = captcha.toBase64().trim()
        val imageBase64 =
            if (raw.startsWith("data:image/", ignoreCase = true)) raw
            else "data:image/png;base64,$raw"

        log.info(
            "图形验证码已签发 captchaIdPrefix={} ttlSeconds={} storeSize={}",
            idPreview(id),
            ttlSeconds,
            store.size,
        )
        return CaptchaResponse(captchaId = id, imageBase64 = imageBase64)
    }

    /**
     * 校验用户提交的验证码是否正确；无论对错，只要曾取出记录即视为消费（同一 `captchaId` 不可再用）。
     *
     * @return `true` 表示校验通过；`false` 表示参数非法、已过期、不存在/已消费或字符不匹配。
     */
    fun validateAndConsume(captchaId: String?, captchaCode: String?): Boolean {
        if (captchaId.isNullOrBlank() || captchaCode.isNullOrBlank()) {
            log.debug("图形验证码校验未执行：captchaId 或 captchaCode 为空")
            return false
        }
        val id = captchaId.trim()
        val input = captchaCode.trim().replace("\\s+".toRegex(), "")
        if (input.length > 16 || id.length > 64) {
            log.debug(
                "图形验证码校验未执行：参数长度非法 captchaIdLen={} captchaCodeLen={}",
                id.length,
                input.length,
            )
            return false
        }
        val rec = store.remove(id)
        if (rec == null) {
            log.debug("图形验证码校验失败：记录不存在或已消费 captchaIdPrefix={}", idPreview(id))
            return false
        }
        if (Instant.now().isAfter(rec.expiresAt)) {
            log.debug("图形验证码校验失败：已过期 captchaIdPrefix={}", idPreview(id))
            return false
        }
        val match = rec.answer.equals(input, ignoreCase = true)
        if (!match) {
            log.debug("图形验证码校验失败：内容与正确答案不一致 captchaIdPrefix={}", idPreview(id))
        }
        return match
    }

    /** 扫描并删除已过期的条目，降低内存占用与校验时的无效命中。 */
    private fun purgeExpired() {
        val now = Instant.now()
        store.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }

    /**
     * 当条目数超过 [maxEntries] 时，在已 [purgeExpired] 的前提下再批量移除若干键，避免 HashMap 无限膨胀。
     * 策略简单即可：按迭代顺序删掉多余的一半量级（非严格 LRU）。
     */
    private fun trimOverflow() {
        if (store.size <= maxEntries) return
        purgeExpired()
        if (store.size <= maxEntries) return
        val overflow = store.size - maxEntries / 2
        store.keys.take(overflow).forEach { store.remove(it) }
        log.warn(
            "图形验证码内存表超限已裁减 removedApprox={} storeSizeAfter={} maxEntries={}",
            overflow,
            store.size,
            maxEntries,
        )
    }

    companion object {
        private const val WIDTH = 120
        private const val HEIGHT = 40
        private const val CODE_LEN = 4

        /** 日志中使用的 captchaId 短前缀，便于排查且不刷屏全长 hex。 */
        private fun idPreview(id: String): String = id.take(8)
    }
}
