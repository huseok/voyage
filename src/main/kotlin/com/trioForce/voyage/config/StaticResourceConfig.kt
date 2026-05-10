package com.trioForce.voyage.config

import com.trioForce.voyage.media.MediaProperties
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

/**
 * 将磁盘目录映射为公开「/media/」路径，供前台加载商品缩略图与完整图。
 */
@Configuration
class StaticResourceConfig(
    private val mediaProperties: MediaProperties,
) : WebMvcConfigurer {
    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        val root = mediaProperties.storageRoot.trimEnd('/', '\\')
        val location = if (root.startsWith("file:")) "$root/" else "file:$root/"
        registry.addResourceHandler("/media/**")
            .addResourceLocations(location)
            .setCachePeriod(3600)
    }
}
