package com.trioForce.voyage.i18n

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class I18nLocaleSupportTest {
    @Test
    fun resolveText_fallsBackToEnThenZh() {
        val map = mapOf("en-US" to "Tools", "zh-CN" to "工具", "es-ES" to "Herramientas")
        assertEquals("Herramientas", I18nLocaleSupport.resolveText(map, "es-ES"))
        assertEquals("Tools", I18nLocaleSupport.resolveText(map, "fr-FR"))
    }

    @Test
    fun resolveProductFields_usesLocaleBucket() {
        val i18n = mapOf(
            "en-US" to mapOf("title" to "Widget", "description" to "Desc EN"),
            "zh-CN" to mapOf("title" to "小部件", "description" to "中文描述"),
        )
        val (title, desc) = I18nLocaleSupport.resolveProductFields(i18n, "zh-CN")
        assertEquals("小部件", title)
        assertEquals("中文描述", desc)
    }
}
