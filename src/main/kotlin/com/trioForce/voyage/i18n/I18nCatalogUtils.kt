package com.trioForce.voyage.i18n

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

/**
 * UI 文案目录工具：扁平化 key 路径、按点分路径写入、深度合并。
 */
object I18nCatalogUtils {
  private val objectMapper = ObjectMapper()

  /** 将嵌套 JSON 叶子节点扁平为 `a.b.c` → 字符串值 */
  fun flatten(obj: Any?, prefix: String = ""): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    when (obj) {
      is Map<*, *> -> {
        obj.forEach { (rawKey, value) ->
          val key = rawKey?.toString() ?: return@forEach
          val path = if (prefix.isEmpty()) key else "$prefix.$key"
          when (value) {
            is Map<*, *> -> result.addAll(flatten(value, path))
            is List<*> -> result.addAll(flattenList(value, path))
            null -> Unit
            else -> result.add(path to value.toString())
          }
        }
      }
      is List<*> -> result.addAll(flattenList(obj, prefix))
      else -> if (obj != null && prefix.isNotEmpty()) result.add(prefix to obj.toString())
    }
    return result
  }

  private fun flattenList(list: List<*>, prefix: String): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    list.forEachIndexed { index, value ->
      val path = "$prefix[$index]"
      when (value) {
        is Map<*, *> -> result.addAll(flatten(value, path))
        is List<*> -> result.addAll(flattenList(value, path))
        null -> Unit
        else -> result.add(path to value.toString())
      }
    }
    return result
  }

  /** 按 `a.b.c` 路径写入字符串叶子（中间节点自动创建） */
  fun setByPath(root: ObjectNode, path: String, value: String) {
    val parts = path.split('.')
    var current: ObjectNode = root
    for (i in 0 until parts.lastIndex) {
      val part = parts[i]
      val child = current.get(part)
      current = if (child != null && child.isObject) {
        child as ObjectNode
      } else {
        val created = objectMapper.createObjectNode()
        current.set<ObjectNode>(part, created)
        created
      }
    }
    current.put(parts.last(), value)
  }

  /** 批量 patch：返回新的 content Map */
  fun patchContent(content: Map<String, Any>, updates: Map<String, String>): Map<String, Any> {
    if (updates.isEmpty()) return content
    val root = objectMapper.valueToTree<ObjectNode>(content)
    updates.forEach { (path, value) -> setByPath(root, path, value) }
    @Suppress("UNCHECKED_CAST")
    return objectMapper.convertValue(root, Map::class.java) as Map<String, Any>
  }

  /**
   * 深度合并目录。
   * @param overwrite true 时 overlay 覆盖同 key；false 时仅填充 base 中缺失的 key。
   */
  fun mergeContent(base: Map<String, Any>, overlay: Map<String, Any>, overwrite: Boolean): Map<String, Any> {
    val result = base.toMutableMap()
    overlay.forEach { (key, overlayValue) ->
      val baseValue = result[key]
      if (baseValue is Map<*, *> && overlayValue is Map<*, *>) {
        @Suppress("UNCHECKED_CAST")
        result[key] = mergeContent(
          baseValue as Map<String, Any>,
          overlayValue as Map<String, Any>,
          overwrite,
        )
      } else if (overwrite || !result.containsKey(key)) {
        result[key] = overlayValue
      }
    }
    return result
  }

  /** 统计叶子节点数量 */
  fun countLeaves(obj: Any?): Int = flatten(obj).size
}
