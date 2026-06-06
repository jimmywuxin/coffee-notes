package com.coffeelab.coffeenotes.util.ocr

/**
 * 词典模糊匹配：基于编辑距离（Levenshtein）。
 *
 * 仅依赖 Kotlin 标准库，可纯 JVM 单测。
 */
object DictionaryMatcher {

    private fun levenshteinDistance(s1: String, s2: String): Int {
        val m = s1.length; val n = s2.length
        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 0..m) dp[i][0] = i
        for (j in 0..n) dp[0][j] = j
        for (i in 1..m) for (j in 1..n) {
            val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
            dp[i][j] = minOf(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost)
        }
        return dp[m][n]
    }

    fun levenshteinSimilarity(s1: String, s2: String): Float {
        val maxLen = maxOf(s1.length, s2.length)
        if (maxLen == 0) return 1f
        return 1f - levenshteinDistance(s1.lowercase(), s2.lowercase()).toFloat() / maxLen
    }

    private val SPLIT_REGEX = Regex("[\\s,，、：:()（）]")

    /** 模糊匹配结果。空 word 表示未命中。 */
    data class MatchResult(val word: String, val score: Float)

    /**
     * 完整模糊匹配：返回命中词条 + 相似度。
     * 比 [fuzzyMatchFirst] 多返回一个 score，便于调用方按分数选择置信度档位。
     */
    fun fuzzyMatchWithScore(
        lines: List<String>, dictionary: List<String>, threshold: Float
    ): MatchResult {
        for (lineText in lines) {
            val words = lineText.split(SPLIT_REGEX).filter { it.length >= 2 }
            var bestWord = ""; var bestScore = threshold
            for (word in words) {
                for (dictWord in dictionary) {
                    val score = levenshteinSimilarity(word, dictWord)
                    if (score > bestScore) {
                        bestScore = score
                        bestWord = dictWord
                    }
                }
            }
            if (bestWord.isNotEmpty()) return MatchResult(bestWord, bestScore)
        }
        return MatchResult("", 0f)
    }

    /**
     * 从文本行列表中模糊匹配第一个命中已知词条的行。
     *
     * 行为：把行按分隔符拆成单个词，对每个词做编辑距离匹配，
     * 超过 [threshold] 相似度的命中即返回对应词典词。
     */
    fun fuzzyMatchFirst(lines: List<String>, dictionary: List<String>, threshold: Float): String {
        return fuzzyMatchWithScore(lines, dictionary, threshold).word
    }

    /**
     * 词典子串精确匹配（不区分大小写）：返回首个 [lineText] 中包含的 [dictionary] 词条。
     * 与 [fuzzyMatchFirst] 不同：直接走 `indexOf`，不做编辑距离。
     */
    fun exactContainsFirst(lines: List<String>, dictionary: List<String>, minLength: Int = 2): String {
        for (lineText in lines) {
            for (kw in dictionary) {
                if (kw.length < minLength) continue
                if (lineText.contains(kw, ignoreCase = true)) return kw
            }
        }
        return ""
    }
}
