package com.coffeelab.coffeenotes.util.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DateAndParamParserTest {

    private val fmt = DateTimeFormatter.ofPattern("yyyy/MM/dd")

    // ── 日期 ──

    @Test
    fun `parse ISO date with slashes`() {
        val text = "Roasted: 2024/01/15"
        val date = DateAndParamParser.extractRoastDate(text)
        assertEquals("2024/01/15", date)
    }

    @Test
    fun `parse ISO date with dashes`() {
        val date = DateAndParamParser.extractRoastDate("Roast 2024-01-15")
        assertEquals("2024/01/15", date)
    }

    @Test
    fun `parse ISO date with dots`() {
        val date = DateAndParamParser.extractRoastDate("Roast 2024.01.15")
        assertEquals("2024/01/15", date)
    }

    @Test
    fun `parse Chinese date format`() {
        val date = DateAndParamParser.extractRoastDate("烘焙日期 2024年1月15日")
        assertEquals("2024/01/15", date)
    }

    @Test
    fun `parse English day-month-year`() {
        val date = DateAndParamParser.extractRoastDate("Roasted on 15 Jan 2024")
        assertEquals("2024/01/15", date)
    }

    @Test
    fun `parse English month-day-year`() {
        val date = DateAndParamParser.extractRoastDate("Roast Date: Jan 15, 2024")
        assertEquals("2024/01/15", date)
    }

    @Test
    fun `BBE date is reduced by 1 year for roast date`() {
        // BBE 2025/03/01 → roastDate 2024/03/01
        val date = DateAndParamParser.extractRoastDate("BBE: 2025/03/01")
        assertEquals("2024/03/01", date)
    }

    @Test
    fun `returns null when no date found`() {
        assertNull(DateAndParamParser.extractRoastDate("Origin: Ethiopia"))
    }

    @Test
    fun `chooses most recent date when multiple`() {
        val text = "BBE: 2025/01/01\nRoasted: 2024/06/15"
        val date = DateAndParamParser.extractRoastDate(text)
        assertNotNull(date)
        // 2024/06/15 距今更近
        val parsed = LocalDate.parse(date, fmt)
        assertTrue(parsed >= LocalDate.of(2024, 6, 15))
    }

    // ── 萃取参数 ──

    @Test
    fun `extract dose in grams`() {
        val p = DateAndParamParser.extractBrewParams("Dose: 18g")
        assertEquals(18f, p.dose!!, 0.001f)
    }

    @Test
    fun `extract dose in Chinese 克`() {
        val p = DateAndParamParser.extractBrewParams("粉量 18 克")
        assertEquals(18f, p.dose!!, 0.001f)
    }

    @Test
    fun `extract brew ratio`() {
        val p = DateAndParamParser.extractBrewParams("Ratio 1:15")
        assertEquals("1:15", p.brewRatio)
    }

    @Test
    fun `extract water amount in ml`() {
        val p = DateAndParamParser.extractBrewParams("Water 250ml")
        assertEquals(250f, p.waterAmount!!, 0.001f)
    }

    @Test
    fun `extract water amount in 毫升`() {
        val p = DateAndParamParser.extractBrewParams("注水量 250 毫升")
        assertEquals(250f, p.waterAmount!!, 0.001f)
    }

    @Test
    fun `extract brew time in seconds`() {
        val p = DateAndParamParser.extractBrewParams("Time: 30s")
        assertEquals(30, p.brewTime)
    }

    @Test
    fun `extract brew time in 秒`() {
        val p = DateAndParamParser.extractBrewParams("萃取时长 30 秒")
        assertEquals(30, p.brewTime)
    }

    @Test
    fun `extract water temp in Celsius`() {
        val p = DateAndParamParser.extractBrewParams("Temp 92°C")
        assertEquals(92, p.waterTemp)
    }

    @Test
    fun `extract water temp with degree symbol`() {
        val p = DateAndParamParser.extractBrewParams("水温 92度")
        assertEquals(92, p.waterTemp)
    }

    @Test
    fun `extracts all params from mixed text`() {
        val p = DateAndParamParser.extractBrewParams(
            "Espresso Recipe: 18g, 1:2, 36g out, 28s, 93°C"
        )
        assertEquals(18f, p.dose!!, 0.001f)
        assertEquals("1:2", p.brewRatio)
        assertEquals(28, p.brewTime)
        assertEquals(93, p.waterTemp)
    }

    @Test
    fun `returns empty params for non-coffee text`() {
        val p = DateAndParamParser.extractBrewParams("Hello World")
        assertNull(p.dose)
        assertEquals("", p.brewRatio)
        assertNull(p.waterAmount)
        assertNull(p.brewTime)
        assertNull(p.waterTemp)
    }
}
