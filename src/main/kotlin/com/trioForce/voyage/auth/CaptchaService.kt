package com.trioForce.voyage.auth

import com.wf.captcha.SpecCaptcha
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 注册用图形验证码：基于 [EasyCaptcha](https://github.com/whvcse/EasyCaptcha)（MIT）生成 PNG，
 * 内存存储短时过期、校验成功后一次性消费。
 */
@Service
class CaptchaService(
    private val ttlSeconds: Long = 120,
    private val maxEntries: Int = 8000,
) {
    data class CaptchaRecord(val answer: String, val expiresAt: Instant)

    private val store = ConcurrentHashMap<String, CaptchaRecord>()

    fun create(): CaptchaResponse {
        purgeExpired()
        trimOverflow()
        val captcha = SpecCaptcha(WIDTH, HEIGHT, CODE_LEN)
        val answer = captcha.text().trim().uppercase()
        val id = UUID.randomUUID().toString().replace("-", "")
        store[id] = CaptchaRecord(answer = answer, expiresAt = Instant.now().plusSeconds(ttlSeconds))
        val b64 = captcha.toBase64()
        return CaptchaResponse(captchaId = id, imageBase64 = "data:image/png;base64,$b64")
    }

    fun validateAndConsume(captchaId: String?, captchaCode: String?): Boolean {
        if (captchaId.isNullOrBlank() || captchaCode.isNullOrBlank()) return false
        val id = captchaId.trim()
        val input = captchaCode.trim().replace("\\s+".toRegex(), "")
        if (input.length > 16 || id.length > 64) return false
        val rec = store.remove(id) ?: return false
        if (Instant.now().isAfter(rec.expiresAt)) return false
        return rec.answer.equals(input, ignoreCase = true)
    }

    private fun purgeExpired() {
        val now = Instant.now()
        store.entries.removeIf { it.value.expiresAt.isBefore(now) }
    }

    private fun trimOverflow() {
        if (store.size <= maxEntries) return
        purgeExpired()
        if (store.size <= maxEntries) return
        val overflow = store.size - maxEntries / 2
        store.keys.take(overflow).forEach { store.remove(it) }
    }

    companion object {
        private const val WIDTH = 120
        private const val HEIGHT = 40
        private const val CODE_LEN = 4
    }
}
