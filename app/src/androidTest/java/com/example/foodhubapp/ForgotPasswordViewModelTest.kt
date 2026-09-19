package com.example.foodhubapp

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.foodhubapp.feature.auth.data.AuthRepository
import com.example.foodhubapp.feature.auth.model.AuthResponse
import com.example.foodhubapp.feature.auth.model.LoginRequest
import com.example.foodhubapp.feature.auth.model.RegisterRequest
import com.example.foodhubapp.feature.auth.viewmodel.ForgotPasswordViewModel
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ForgotPasswordViewModelTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val application get() = instrumentation.targetContext.applicationContext as Application

    private class FakeRepository : AuthRepository {
        var calls = 0
        var submittedEmail = ""
        val response = CompletableDeferred<Result<String>>()
        override suspend fun forgotPassword(email: String): Result<String> {
            calls++
            submittedEmail = email
            return response.await()
        }
        override suspend fun login(request: LoginRequest): Result<AuthResponse> = error("Not used")
        override suspend fun register(request: RegisterRequest): Result<AuthResponse> = error("Not used")
    }

    @Test
    fun invalidEmailDoesNotSendRequest() {
        val repository = FakeRepository()
        instrumentation.runOnMainSync {
            val model = ForgotPasswordViewModel(application, SavedStateHandle(), repository)
            model.updateEmail("invalid")
            model.sendResetLink()
            assertEquals(0, repository.calls)
            assertFalse(model.uiState.value.canSend)
            assertNotNull(model.uiState.value.errorMessage)
        }
    }

    @Test
    fun loadingBlocksDuplicateRequestAndSuccessIsRestored() {
        val repository = FakeRepository()
        val savedState = SavedStateHandle()
        lateinit var model: ForgotPasswordViewModel
        instrumentation.runOnMainSync {
            model = ForgotPasswordViewModel(application, savedState, repository)
            model.updateEmail(" user@example.com ")
            model.sendResetLink()
            model.sendResetLink()
            model.updateEmail("other@example.com")
            assertTrue(model.uiState.value.isLoading)
            assertFalse(model.uiState.value.canSend)
        }
        instrumentation.waitForIdleSync()
        assertEquals(1, repository.calls)
        assertEquals("user@example.com", repository.submittedEmail)
        repository.response.complete(Result.success("Accepted"))
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertFalse(model.uiState.value.isLoading)
            assertTrue(model.uiState.value.isRequestSent)
            val restored = ForgotPasswordViewModel(application, savedState, repository)
            assertTrue(restored.uiState.value.isRequestSent)
            assertEquals(" user@example.com ", restored.uiState.value.email)
            model.updateEmail("other@example.com")
            assertFalse(model.uiState.value.isRequestSent)
        }
    }

    @Test
    fun failureShowsErrorAndAllowsRetry() {
        val repository = FakeRepository()
        lateinit var model: ForgotPasswordViewModel
        instrumentation.runOnMainSync {
            model = ForgotPasswordViewModel(application, SavedStateHandle(), repository)
            model.updateEmail("user@example.com")
            model.sendResetLink()
        }
        instrumentation.waitForIdleSync()
        repository.response.complete(Result.failure(IllegalStateException("Server unavailable")))
        instrumentation.waitForIdleSync()
        instrumentation.runOnMainSync {
            assertEquals("Server unavailable", model.uiState.value.errorMessage)
            assertFalse(model.uiState.value.isLoading)
            assertFalse(model.uiState.value.isRequestSent)
            assertTrue(model.uiState.value.canSend)
            model.sendResetLink()
        }
        instrumentation.waitForIdleSync()
        assertEquals(2, repository.calls)
    }
}
