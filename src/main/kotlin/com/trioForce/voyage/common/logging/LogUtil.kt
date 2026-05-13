package com.trioForce.voyage.common.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * 全项目统一日志入口。
 *
 * ## 为何存在
 * - 禁止在业务代码中使用 `println`、`print`、`System.out/err` 输出诊断信息（无法控制级别、无法按环境聚合、易泄露敏感上下文）。
 * - 通过本工具类集中获取 [Logger]，便于后续替换实现（例如统一 MDC、脱敏、指标）而无需全仓改 import。
 *
 * ## 用法
 * ```kotlin
 * class FooService {
 *     private val log = LogUtil.logger<FooService>()
 *     fun bar() {
 *         log.debug("enter bar userId={}", userId)
 *     }
 * }
 * ```
 *
 * ## 底层
 * 使用 SLF4J API；具体实现由 Spring Boot 默认引入的 Logback（或 `logging.config` 指定的配置）负责。
 *
 * @see org.slf4j.Logger
 */
object LogUtil {

    /**
     * 按运行时的 [KClass] 绑定 Logger 名称（一般为全限定类名），**推荐**在 Spring Bean / 普通类中使用。
     */
    inline fun <reified T : Any> logger(): Logger = LoggerFactory.getLogger(T::class.java)

    /**
     * Java 互操作或非 reified 场景：直接传入 [Class]。
     */
    fun logger(clazz: Class<*>): Logger = LoggerFactory.getLogger(clazz)

    /**
     * 按任意字符串作为 Logger 名（用于脚本式组件、Flyway 迁移类等无单一宿主类名的场景）。
     */
    fun logger(name: String): Logger = LoggerFactory.getLogger(name)
}
