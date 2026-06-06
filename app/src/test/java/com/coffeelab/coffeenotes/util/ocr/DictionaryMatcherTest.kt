package com.coffeelab.coffeenotes.util.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DictionaryMatcherTest {

    @Test
    fun `levenshteinSimilarity exact match returns 1`() {
        assertEquals(1f, DictionaryMatcher.levenshteinSimilarity("Bourbon", "Bourbon"), 0.001f)
    }

    @Test
    fun `levenshteinSimilarity case insensitive`() {
        assertEquals(1f, DictionaryMatcher.levenshteinSimilarity("BOURBON", "bourbon"), 0.001f)
    }

    @Test
    fun `levenshteinSimilarity one char diff`() {
        // "Bourbon" vs "Bourbaon" (1 edit) = 7/8 = 0.875
        val s = DictionaryMatcher.levenshteinSimilarity("Bourbon", "Bourbaon")
        assertTrue("expected ~0.875, got $s", s in 0.85f..0.90f)
    }

    @Test
    fun `levenshteinSimilarity completely different is low`() {
        val s = DictionaryMatcher.levenshteinSimilarity("abc", "xyz")
        assertTrue("expected low, got $s", s < 0.5f)
    }

    @Test
    fun `levenshteinSimilarity empty strings return 1`() {
        assertEquals(1f, DictionaryMatcher.levenshteinSimilarity("", ""), 0.001f)
    }

    @Test
    fun `fuzzyMatchFirst finds known variety in line`() {
        val lines = listOf("品种: 瑰夏 Geisha 1500m")
        val result = DictionaryMatcher.fuzzyMatchFirst(lines, listOf("瑰夏", "Geisha"), 0.7f)
        assertTrue(result in listOf("瑰夏", "Geisha"))
    }

    @Test
    fun `fuzzyMatchFirst returns empty when below threshold`() {
        val lines = listOf("产地: 巴西")
        val result = DictionaryMatcher.fuzzyMatchFirst(lines, listOf("Bourbon"), 0.7f)
        assertEquals("", result)
    }

    @Test
    fun `fuzzyMatchFirst splits by punctuation`() {
        val lines = listOf("风味: 焦糖、坚果、巧克力")
        // Should find "焦糖" since it's 2+ chars and matches exactly
        val result = DictionaryMatcher.fuzzyMatchFirst(lines, listOf("焦糖", "坚果"), 0.7f)
        assertEquals("焦糖", result)
    }

    @Test
    fun `fuzzyMatchFirst skips words shorter than 2 chars`() {
        val lines = listOf("Brand X")
        val result = DictionaryMatcher.fuzzyMatchFirst(lines, listOf("X"), 0.5f)
        assertEquals("", result)
    }

    @Test
    fun `fuzzyMatchWithScore returns score for exact match`() {
        val result = DictionaryMatcher.fuzzyMatchWithScore(
            listOf("Bourbon"), listOf("Bourbon"), 0.5f
        )
        assertEquals("Bourbon", result.word)
        assertEquals(1f, result.score, 0.001f)
    }

    @Test
    fun `fuzzyMatchWithScore returns empty when below threshold`() {
        val result = DictionaryMatcher.fuzzyMatchWithScore(
            listOf("Ethiopia"), listOf("Bourbon"), 0.7f
        )
        assertEquals("", result.word)
    }

    @Test
    fun `exactContainsFirst finds keyword case insensitive`() {
        val lines = listOf("Roasted by: 烘焙商A")
        val result = DictionaryMatcher.exactContainsFirst(lines, listOf("roasted by"), 2)
        assertEquals("roasted by", result)
    }

    @Test
    fun `exactContainsFirst skips short keywords`() {
        val lines = listOf("A B C")
        val result = DictionaryMatcher.exactContainsFirst(lines, listOf("A"), minLength = 2)
        assertEquals("", result)
    }

    @Test
    fun `exactContainsFirst returns first match`() {
        val lines = listOf("Origin: Ethiopia", "Roaster: X")
        val result = DictionaryMatcher.exactContainsFirst(
            lines, listOf("Roaster", "Origin"), 2
        )
        assertEquals("Origin", result)
    }
}
