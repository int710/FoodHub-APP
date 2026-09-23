package com.example.foodhubapp

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.foodhubapp.core.network.FoodHubApiException
import com.example.foodhubapp.feature.menu.data.*
import com.example.foodhubapp.feature.menu.ui.*
import com.example.foodhubapp.feature.menu.viewmodel.FoodDetailViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FoodDetailViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application get() = instrumentation.targetContext.applicationContext as Application

    private class FakeRepository : FoodRepository {
        var loadedId = ""
        var addCalls = 0
        var submitted: FoodCartSelection? = null
        var detail = CompletableDeferred<FoodDetail>()
        val cart = CompletableDeferred<Unit>()
        override suspend fun getMenu(): List<MenuCategory> = error("Not used")
        override suspend fun getFood(id: String): FoodDetail { loadedId = id; return detail.await() }
        override suspend fun addToCart(selection: FoodCartSelection) { addCalls++; submitted = selection; cart.await() }
    }

    @Test fun loadingUsesRouteIdAndRetryClearsFailure() {
        val repository = FakeRepository()
        lateinit var model: FoodDetailViewModel
        instrumentation.runOnMainSync {
            model = FoodDetailViewModel(application, SavedStateHandle(mapOf("foodId" to "actual-id")), repository)
            assertTrue(model.uiState.value.isLoading)
            model.loadFood()
        }
        instrumentation.waitForIdleSync()
        assertEquals("actual-id", repository.loadedId)
        repository.detail.completeExceptionally(IllegalStateException("Offline"))
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertFalse(model.uiState.value.isLoading)
            assertEquals("Offline", model.uiState.value.error)
            assertNull(model.uiState.value.food)
            repository.detail = CompletableDeferred()
            model.loadFood()
            assertTrue(model.uiState.value.isLoading)
            assertNull(model.uiState.value.error)
        }
        repository.detail.complete(PreviewBurger.copy(id = "actual-id"))
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertEquals("actual-id", model.uiState.value.food?.id)
            assertNull(model.uiState.value.error)
            assertFalse(model.uiState.value.isLoading)
        }
    }

    @Test fun validSelectionBlocksDuplicateAndOnlyReportsSuccessAfterResponse() {
        val repository = FakeRepository()
        repository.detail.complete(PreviewBurger)
        lateinit var model: FoodDetailViewModel
        val selection = FoodCartSelection(PreviewBurger.id, 2, listOf("double", "egg"), "No onion")
        instrumentation.runOnMainSync {
            model = FoodDetailViewModel(application, SavedStateHandle(mapOf("foodId" to PreviewBurger.id)), repository)
            model.addToCart(selection)
            model.addToCart(selection)
            assertTrue(model.uiState.value.isAdding)
            assertNull(model.uiState.value.message)
        }
        instrumentation.waitForIdleSync()
        assertEquals(1, repository.addCalls)
        assertEquals(selection, repository.submitted)
        repository.cart.complete(Unit)
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertFalse(model.uiState.value.isAdding)
            assertEquals("Đã thêm món vào giỏ mang đi.", model.uiState.value.message)
            model.consumeMessage()
            assertNull(model.uiState.value.message)
        }
    }

    @Test fun invalidRequiredOptionNeverCallsCartApi() {
        val repository = FakeRepository()
        repository.detail.complete(PreviewBurger)
        instrumentation.runOnMainSync {
            val model = FoodDetailViewModel(application, SavedStateHandle(mapOf("foodId" to PreviewBurger.id)), repository)
            model.addToCart(FoodCartSelection(PreviewBurger.id, 1, listOf("egg"), ""))
            assertEquals(0, repository.addCalls)
            assertNotNull(model.uiState.value.message)
            assertFalse(model.uiState.value.isAdding)
        }
    }

    @Test fun expiredTokenPromptsLoginWithoutFalseSuccess() {
        val repository = FakeRepository()
        repository.detail.complete(PreviewBurger)
        repository.cart.completeExceptionally(FoodHubApiException(401, "Expired"))
        instrumentation.runOnMainSync {
            val model = FoodDetailViewModel(application, SavedStateHandle(mapOf("foodId" to PreviewBurger.id)), repository)
            model.addToCart(FoodCartSelection(PreviewBurger.id, 1, listOf("single"), ""))
            assertTrue(model.uiState.value.requiresLogin)
            assertFalse(model.uiState.value.isAdding)
            assertNull(model.uiState.value.message)
            model.dismissLogin()
            assertFalse(model.uiState.value.requiresLogin)
        }
    }
}
