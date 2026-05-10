package com.trioForce.voyage.media

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * 本地媒体存储与衍生图策略配置。
 *
 * - [storageRoot]：磁盘根目录，上传文件落在 `{root}/yyyy/MM/` 下。
 * - [fullVariant]：`original` 表示「完整图」保存上传原字节；`scaled` 表示按最大边约束缩放（仍可能比缩略图大）。
 *
 * 变更配置后须重启服务；无需改库结构。
 */
@ConfigurationProperties(prefix = "voyage.media")
data class MediaProperties(
    /** 绝对或相对路径；相对路径以 JVM 工作目录为基准 */
    var storageRoot: String = "./data/media",
    /** 单文件上传上限（字节），与服务入口 multipart 限制协同 */
    var maxUploadBytes: Long = 10L * 1024 * 1024,
    var thumbnailMaxWidth: Int = 400,
    var thumbnailMaxHeight: Int = 400,
    /**
     * `scaled`：按 [fullMaxWidth] x [fullMaxHeight] 上限缩放。
     * `original`：完整图不写衍生版本，直接保存原始字节。
     */
    var fullVariant: String = "scaled",
    var fullMaxWidth: Int = 1600,
    var fullMaxHeight: Int = 1600,
)
