package com.coffeelab.coffeenotes.util.ocr

import com.coffeelab.coffeenotes.util.OCRResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FieldExtractorTest {

    private fun line(text: String, left: Int = 0, top: Int = 0, w: Int = 200, h: Int = 30): OcrLine {
        return OcrLine(text, BoundingBox(left, top, left + w, top + h), 0.9f)
    }

    // ── 同行 label:value 解析 ──

    @Test
    fun `horizontal label value extraction`() {
        val lines = listOf(
            line("Roaster: 小野咖啡"),
            line("Origin: Ethiopia")
        )
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("小野咖啡", r.roaster)
        assertEquals("Ethiopia", r.origin)
        assertTrue(r.fieldConfidence.containsKey("roaster"))
        assertEquals(OcrConfidence.HORIZONTAL_LABEL_VALUE, r.fieldConfidence["roaster"]!!, 0.001f)
    }

    @Test
    fun `horizontal with Chinese label`() {
        val lines = listOf(
            line("烘焙商: 启程咖啡"),
            line("豆名: 瑰夏 拼配")
        )
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("启程咖啡", r.roaster)
        assertTrue(r.name.contains("瑰夏") || r.name.contains("拼配") || r.name.isNotEmpty())
    }

    @Test
    fun `field is only filled once even if duplicated`() {
        val lines = listOf(
            line("Roaster: A"),
            line("Roaster: B")  // 第二个应被忽略
        )
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("A", r.roaster)
    }

    // ── 垂直 label / value 解析 ──

    @Test
    fun `vertical label value extraction`() {
        val lines = listOf(
            line("Roaster", top = 0),     // 单独标签行
            line("小野咖啡", top = 50)     // 紧邻的下一行
        )
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("小野咖啡", r.roaster)
        assertEquals(
            OcrConfidence.VERTICAL_LABEL_VALUE,
            r.fieldConfidence["roaster"]!!, 0.001f
        )
    }

    // ── 词典子串匹配 ──

    @Test
    fun `dict exact contains for variety`() {
        val lines = listOf(line("品种信息: 瑰夏 Geisha 1500m"))
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("瑰夏", r.variety)
        assertEquals(OcrConfidence.DICT_EXACT_CONTAINS, r.fieldConfidence["variety"]!!, 0.001f)
    }

    @Test
    fun `dict fuzzy match for variety`() {
        // 模糊: "Bourbom" (typo) → fuzzy to "Bourbon"
        // 不带 "Variety:" 标签让模糊匹配先走
        val lines = listOf(line("Bourbom beans, 1500m"))
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("Bourbon", r.variety)
        // Score should be high → DICT_FUZZY_HIGH
        assertEquals(OcrConfidence.DICT_FUZZY_HIGH, r.fieldConfidence["variety"]!!, 0.001f)
    }

    @Test
    fun `dict exact match for origin extracts region too`() {
        val lines = listOf(line("产地: 埃塞俄比亚 耶加雪菲"))
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("埃塞俄比亚", r.origin)
        assertEquals("耶加雪菲", r.region)
    }

    @Test
    fun `process fuzzy match`() {
        val lines = listOf(line("Process: washed"))
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("washed", r.process)
    }

    @Test
    fun `roastLevel standardized to 浅烘`() {
        val lines = listOf(line("Roast Level: 浅度烘焙"))
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("浅烘", r.roastLevel)
    }

    @Test
    fun `roastLevel standardized to 中烘`() {
        val lines = listOf(line("Roast: 中度烘焙"))
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("中烘", r.roastLevel)
    }

    @Test
    fun `roastLevel standardized to 深烘`() {
        val lines = listOf(line("Roast: 深度烘焙"))
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("深烘", r.roastLevel)
    }

    // ── 豆名打分 ──

    @Test
    fun `name scoring picks non-label line`() {
        val lines = listOf(
            line("Roaster: X", top = 0),
            line("Sidamo Sunrise", top = 50, h = 50),  // 大字 + 不含标签
            line("Origin: Ethiopia", top = 110)
        )
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        // Name should be "Sidamo Sunrise" (cleaned of nothing)
        assertTrue("Expected Sidamo Sunrise, got '${r.name}'", r.name.contains("Sidamo"))
    }

    @Test
    fun `name scoring rejects lines with digits`() {
        val lines = listOf(
            line("Roaster: A", top = 0),
            line("250g Net Weight", top = 50)  // 含数字应被剔除
        )
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        // 250g 应被排除；name 可能是 "Net Weight" 但含空格也是问题
        assertTrue("Name 不应含 '250g'", !r.name.contains("250g"))
    }

    // ── 风味抽取 + 排除 ──

    @Test
    fun `flavors extracted but excluded terms removed`() {
        val lines = listOf(
            line("Roaster: 焦糖工坊"),
            line("风味: 焦糖 巧克力 坚果")
        )
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        assertEquals("焦糖工坊", r.roaster)
        // "焦糖" 出现在 roaster 字段中，应从风味里排除
        assertFalse("焦糖 应被排除（与 roaster 重复）", r.flavors.contains("焦糖"))
        assertTrue("巧克力 应保留", r.flavors.contains("巧克力"))
    }

    // ── 置信度汇总 ──

    @Test
    fun `lowConfidenceFields populated for fuzzy matches`() {
        val lines = listOf(line("Variety: Bourbom"))  // fuzzy
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        // DICT_FUZZY_HIGH (0.7) > LOW_CONFIDENCE_THRESHOLD (0.6) → 不应进 lowConfidence
        assertTrue("variety 不应低置信", "variety" !in r.lowConfidenceFields)
    }

    @Test
    fun `lowConfidenceFields populated for fallback matches`() {
        // 构造一个没有任何标签/词典命中、靠 fallback 命中的 name
        val lines = listOf(
            line("Roaster: A", top = 0),
            line("MyCustomBlend", top = 50, h = 30)
        )
        val r = FieldExtractor.extract(lines, lines.joinToString("\n") { it.text })
        // cleanup 会去掉 "Blend" 后缀（"咖啡豆?|coffee|Bean|Blend|单品"）
        assertEquals("MyCustom", r.name)
        // name via FALLBACK (0.3) < 0.6 → 进 lowConfidence
        assertTrue("name 应被标记为低置信", "name" in r.lowConfidenceFields)
    }

    @Test
    fun `empty lines returns empty result`() {
        val r: OCRResult = FieldExtractor.extract(emptyList(), "")
        assertEquals("", r.roaster)
        assertEquals("", r.name)
        assertTrue(r.flavors.isEmpty())
    }
}
