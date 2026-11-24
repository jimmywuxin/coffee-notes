package com.coffeelab.coffeenotes.data.dao

import org.junit.Assert.assertEquals
import org.junit.Test

class BeanInventoryTest {

    @Test
    fun `remaining is purchase plus adjustment minus consumed`() {
        val inv = BeanInventory(
            beanId = 1,
            totalPurchased = 200,
            totalAdjusted = 20.0,
            totalConsumed = 30.0
        )
        assertEquals(190.0, inv.remaining, 0.0001)
    }

    @Test
    fun `remaining clamps to zero when consumed exceeds stock`() {
        val inv = BeanInventory(
            beanId = 1,
            totalPurchased = 200,
            totalAdjusted = -10.0,
            totalConsumed = 250.0
        )
        assertEquals(0.0, inv.remaining, 0.0001)
    }

    @Test
    fun `remaining clamps to zero when full deduction exhausts stock`() {
        val inv = BeanInventory(
            beanId = 1,
            totalPurchased = 50,
            totalAdjusted = -50.0,
            totalConsumed = 0.0
        )
        assertEquals(0.0, inv.remaining, 0.0001)
    }
}
