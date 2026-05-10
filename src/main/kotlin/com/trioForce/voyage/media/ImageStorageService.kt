package com.trioForce.voyage.media

import com.trioForce.voyage.common.BizException
import net.coobird.thumbnailator.Thumbnails
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.imageio.ImageIO

/**
 * 处理管理员上传的图片：生成缩略图与「完整」图（策略见 [MediaProperties]）。
 */
@Service
class ImageStorageService(
    private val props: MediaProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * @return Pair(thumbUrl, fullUrl) 均为以 `/media/` 开头的站点相对路径
     */
    fun storeImage(file: MultipartFile): Pair<String, String> {
        if (file.isEmpty) throw BizException("empty file")
        val size = file.size
        if (size > props.maxUploadBytes) {
            throw BizException("file too large (max ${props.maxUploadBytes} bytes)")
        }
        val contentType = file.contentType ?: ""
        if (!isAllowedImageContentType(contentType)) {
            throw BizException("unsupported content type")
        }
        val ext = resolveExtension(contentType, file.originalFilename)
        val bytes = file.bytes
        val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: throw BizException("unsupported or corrupt image")

        val ym = YearMonth.now().format(DateTimeFormatter.ofPattern("yyyy/MM"))
        val ymParts = ym.split('/').toTypedArray()
        val dir = Paths.get(props.storageRoot, *ymParts)
        Files.createDirectories(dir)

        val id = UUID.randomUUID().toString()
        val thumbName = "${id}_thumb$ext"
        val fullName = "${id}_full$ext"
        val thumbPath = dir.resolve(thumbName)
        val fullPath = dir.resolve(fullName)

        try {
            Thumbnails.of(image)
                .size(props.thumbnailMaxWidth, props.thumbnailMaxHeight)
                .keepAspectRatio(true)
                .toFile(thumbPath.toFile())

            when (props.fullVariant.lowercase()) {
                "original" -> Files.write(fullPath, bytes)
                "scaled" -> {
                    Thumbnails.of(image)
                        .size(props.fullMaxWidth, props.fullMaxHeight)
                        .keepAspectRatio(true)
                        .toFile(fullPath.toFile())
                }
                else -> throw BizException("invalid voyage.media.full-variant (use scaled|original)")
            }
        } catch (ex: BizException) {
            cleanupQuietly(thumbPath, fullPath)
            throw ex
        } catch (ex: Exception) {
            log.warn("image pipeline failed: {}", ex.message)
            cleanupQuietly(thumbPath, fullPath)
            throw BizException("failed to process image")
        }

        val base = "/media/$ym/"
        return Pair(base + thumbName, base + fullName)
    }

    private fun cleanupQuietly(vararg paths: Path) {
        paths.forEach { p ->
            try {
                Files.deleteIfExists(p)
            } catch (ex: Exception) {
                log.debug("cleanup skip {}: {}", p, ex.message)
            }
        }
    }

    private fun isAllowedImageContentType(ct: String): Boolean {
        val c = ct.lowercase()
        return c.startsWith("image/jpeg") || c.startsWith("image/png") ||
            c.startsWith("image/webp") || c.startsWith("image/gif")
    }

    private fun resolveExtension(contentType: String, filename: String?): String {
        val lower = filename?.lowercase().orEmpty()
        return when {
            contentType.contains("png") || lower.endsWith(".png") -> ".png"
            contentType.contains("webp") || lower.endsWith(".webp") -> ".webp"
            contentType.contains("gif") || lower.endsWith(".gif") -> ".gif"
            else -> ".jpg"
        }
    }
}
