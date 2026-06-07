package com.trioForce.voyage.i18n

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.stereotype.Component

/** 应用启动后若文案目录为空，自动从 classpath 种子导入中英目录 */
@Component
class I18nCatalogBootstrap(
    private val catalogService: I18nCatalogService,
) : ApplicationRunner {
    override fun run(args: ApplicationArguments?) {
        catalogService.bootstrapIfEmpty()
    }
}
