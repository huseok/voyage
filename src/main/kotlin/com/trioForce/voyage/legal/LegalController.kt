package com.trioForce.voyage.legal

import com.trioForce.voyage.auth.LegalVersions
import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

/** 对外公布当前生效的法律文档版本，供注册页与静态页对齐。 */
@RestController
@Tag(name = "Legal", description = "服务条款与隐私政策版本")
class LegalController {
    @GetMapping("/api/v1/legal/versions")
    fun versions(): ApiResponse<Map<String, String>> =
        ok(
            mapOf(
                "termsVersion" to LegalVersions.TERMS,
                "privacyVersion" to LegalVersions.PRIVACY,
            ),
        )
}
