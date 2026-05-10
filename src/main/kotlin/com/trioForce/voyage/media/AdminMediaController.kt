package com.trioForce.voyage.media

import com.trioForce.voyage.common.ApiResponse
import com.trioForce.voyage.common.ok
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

data class MediaUploadView(
    val thumbUrl: String,
    val fullUrl: String,
)

/**
 * 管理端上传接口：返回可供写入商品表的相对 URL。
 */
@RestController
@Tag(name = "AdminMedia", description = "后台媒体上传")
class AdminMediaController(
    private val imageStorageService: ImageStorageService,
) {
    @PostMapping("/api/v1/admin/media/upload", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun upload(@RequestPart("file") file: MultipartFile): ApiResponse<MediaUploadView> {
        val (thumb, full) = imageStorageService.storeImage(file)
        return ok(MediaUploadView(thumbUrl = thumb, fullUrl = full))
    }
}
