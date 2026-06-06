package com.coffeelab.coffeenotes.util.ocr

import com.coffeelab.coffeenotes.util.FlavorKeywords

/**
 * 风味关键词抽取。
 *
 * 规则：
 *  1. 按词条长度倒序优先匹配（长词优先）
 *  2. 已匹配字段中的词会被排除（避免风味标签被产地/豆名污染）
 *  3. 同位置不允许重叠（贪心去重）
 *  4. 最终按出现顺序输出
 */
object FlavorExtractor {

    fun extract(
        text: String,
        flavors: MutableList<String>,
        excludedTerms: Set<String> = emptySet()
    ) {
        val sorted = FlavorKeywords.keywords.sortedByDescending { it.length }
        val matches = mutableListOf<Triple<Int, Int, String>>()

        for (kw in sorted) {
            if (kw.length < 2) continue
            if (excludedTerms.any { excluded ->
                    excluded.length >= 2 && (excluded.contains(kw, ignoreCase = true) || kw.contains(excluded, ignoreCase = true))
                }) continue

            var pos = 0
            while (true) {
                val idx = text.indexOf(kw, pos, ignoreCase = true)
                if (idx < 0) break
                matches.add(Triple(idx, idx + kw.length, kw))
                pos = idx + 1
            }
        }

        val selected = mutableListOf<Triple<Int, Int, String>>()
        for (m in matches.sortedByDescending { it.second - it.first }) {
            if (selected.none { m.first < it.second && m.second > it.first }) selected.add(m)
        }
        for (m in selected.sortedBy { it.first }) {
            if (!flavors.contains(m.third)) flavors.add(m.third)
        }
    }
}
