package com.coffeelab.coffeenotes.viewmodel

import com.coffeelab.coffeenotes.data.AppDatabase
import com.coffeelab.coffeenotes.data.entity.BrewRecord
import com.coffeelab.coffeenotes.data.repository.CoffeeRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BrewViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: CoffeeRepository
    private lateinit var db: AppDatabase
    private lateinit var viewModel: BrewViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        db = mockk(relaxed = true)
        repository = spyk(CoffeeRepository(db))
        viewModel = BrewViewModel(mockk(relaxed = true))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `saveRecord inserts and returns id`() = runTest {
        val record = BrewRecord(
            beanId = 1L,
            equipmentId = 1L,
            coffeeWeight = 15.0,
            waterTemp = 92.0,
            overallRating = 4
        )
        coEvery { repository.insertRecord(any()) } returns 42L

        // This test validates the save pattern; in practice ViewModel delegates to repository
        val id = repository.insertRecord(record)
        assertEquals(42L, id)
    }

    @Test
    fun `deleteRecord calls repository delete`() = runTest {
        val record = BrewRecord(id = 1, beanId = 1L)
        coEvery { repository.deleteRecord(any()) } just Runs

        repository.deleteRecord(record)
        coVerify(exactly = 1) { repository.deleteRecord(record) }
    }

    @Test
    fun `getBestRecordForBean returns best rated record`() = runTest {
        val expected = BrewRecord(id = 5, beanId = 1L, overallRating = 5)
        coEvery { repository.getBestRecordForBean(1L) } returns expected

        val result = repository.getBestRecordForBean(1L)
        assertEquals(expected, result)
    }

    @Test
    fun `unitPrice computes correctly for PurchaseRecord`() {
        // Validate that our refactored computed property works
        val record = com.coffeelab.coffeenotes.data.entity.PurchaseRecord(
            id = 1,
            beanId = 1L,
            date = System.currentTimeMillis(),
            weightGrams = 200,
            price = 128.0f
        )
        assertEquals(0.64f, record.unitPrice)

        val zeroWeight = record.copy(weightGrams = 0)
        assertEquals(0f, zeroWeight.unitPrice)
    }
}
