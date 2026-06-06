package com.coffeelab.coffeenotes.util.ocr

import com.coffeelab.coffeenotes.util.ocr.dictionary.CoffeeDictionary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CoffeeDictionaryTest {

    @Test
    fun `no empty strings in any list`() {
        val allLists = mapOf(
            "roasterLabels" to CoffeeDictionary.roasterLabels,
            "originLabels" to CoffeeDictionary.originLabels,
            "regionLabels" to CoffeeDictionary.regionLabels,
            "varietyLabels" to CoffeeDictionary.varietyLabels,
            "processLabels" to CoffeeDictionary.processLabels,
            "roastLevelLabels" to CoffeeDictionary.roastLevelLabels,
            "estateLabels" to CoffeeDictionary.estateLabels,
            "knownVarieties" to CoffeeDictionary.knownVarieties,
            "coffeeNameKeywords" to CoffeeDictionary.coffeeNameKeywords,
            "commonOrigins" to CoffeeDictionary.commonOrigins,
            "knownProcesses" to CoffeeDictionary.knownProcesses,
            "knownRoastLevels" to CoffeeDictionary.knownRoastLevels
        )
        for ((name, list) in allLists) {
            assertTrue("$name 应非空", list.isNotEmpty())
            for (s in list) {
                assertTrue("$name 包含空字符串: $list", s.isNotBlank())
            }
        }
    }

    @Test
    fun `no duplicates within each list`() {
        val lists = listOf(
            CoffeeDictionary.roasterLabels,
            CoffeeDictionary.originLabels,
            CoffeeDictionary.regionLabels,
            CoffeeDictionary.varietyLabels,
            CoffeeDictionary.processLabels,
            CoffeeDictionary.roastLevelLabels,
            CoffeeDictionary.estateLabels,
            CoffeeDictionary.knownVarieties,
            CoffeeDictionary.coffeeNameKeywords,
            CoffeeDictionary.commonOrigins,
            CoffeeDictionary.knownProcesses,
            CoffeeDictionary.knownRoastLevels
        )
        for (list in lists) {
            val seen = mutableSetOf<String>()
            for (s in list) {
                assertFalse("重复词条: $s in $list", s in seen)
                seen.add(s)
            }
        }
    }

    @Test
    fun `UTF-8 Chinese characters preserved`() {
        assertTrue(CoffeeDictionary.roasterLabels.contains("烘焙商"))
        assertTrue(CoffeeDictionary.originLabels.contains("产地"))
        assertTrue(CoffeeDictionary.varietyLabels.contains("品种"))
        assertTrue(CoffeeDictionary.knownVarieties.contains("瑰夏"))
        assertTrue(CoffeeDictionary.commonOrigins.contains("埃塞俄比亚"))
        assertTrue(CoffeeDictionary.knownProcesses.contains("水洗"))
    }

    @Test
    fun `Phase 4_4 expansions are present`() {
        // 新增产地亚区
        assertTrue(CoffeeDictionary.commonOrigins.contains("Yirgacheffe"))
        assertTrue(CoffeeDictionary.commonOrigins.contains("Antigua"))
        assertTrue(CoffeeDictionary.commonOrigins.contains("Tarrazu"))
        assertTrue(CoffeeDictionary.commonOrigins.contains("Sidamo"))

        // 新增豆种
        assertTrue(CoffeeDictionary.knownVarieties.contains("Wush Wush"))
        assertTrue(CoffeeDictionary.knownVarieties.contains("Pink Bourbon"))
        assertTrue(CoffeeDictionary.knownVarieties.contains("Tabi"))
        assertTrue(CoffeeDictionary.knownVarieties.contains("Castillo"))

        // 新增处理法
        assertTrue(CoffeeDictionary.knownProcesses.contains("carbonic maceration"))
        assertTrue(CoffeeDictionary.knownProcesses.contains("anaerobic"))
        assertTrue(CoffeeDictionary.knownProcesses.contains("白蜜"))
        assertTrue(CoffeeDictionary.knownProcesses.contains("红酒处理"))

        // 新增庄园关键词
        assertTrue(CoffeeDictionary.estateLabels.contains("Mill"))
        assertTrue(CoffeeDictionary.estateLabels.contains("Co-op"))
        assertTrue(CoffeeDictionary.estateLabels.contains("Washing Station"))

        // 新增烘焙度
        assertTrue(CoffeeDictionary.knownRoastLevels.contains("Light"))
        assertTrue(CoffeeDictionary.knownRoastLevels.contains("City"))
        assertTrue(CoffeeDictionary.knownRoastLevels.contains("French"))
        assertTrue(CoffeeDictionary.knownRoastLevels.contains("意式烘焙"))
    }

    @Test
    fun `nameSkipPrefixes contains label words`() {
        val skip = CoffeeDictionary.nameSkipPrefixes
        assertTrue(skip.contains("Roaster"))
        assertTrue(skip.contains("烘焙商"))
        assertTrue(skip.contains("Origin"))
        assertTrue(skip.contains("庄园"))
    }
}
