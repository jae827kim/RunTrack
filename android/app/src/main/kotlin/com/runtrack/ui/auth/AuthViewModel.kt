package com.runtrack.ui.auth

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.runtrack.data.api.ApiSession
import com.runtrack.data.api.userMessage
import com.runtrack.data.repository.AuthRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

class AuthViewModel(private val repository: AuthRepository = AuthRepository()) : ViewModel() {
    val session = ApiSession.state
    var signingUp by mutableStateOf(false)
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var suggestedUsername by mutableStateOf("")
        private set
    var registrationReviewed by mutableStateOf(false)
        private set

    fun showSignup() { if (!busy) { signingUp = true; error = null; registrationReviewed = false } }
    fun showLogin() { if (!busy) { signingUp = false; error = null } }

    fun login(username: String, password: String) {
        if (busy) return
        if (OfflineAdminAccount.isReserved(username)) {
            error = null
            if (OfflineAdminAccount.matches(username, password)) {
                ApiSession.startOfflineAdmin()
                registrationReviewed = false
            } else error = "아이디 또는 비밀번호가 올바르지 않아요."
            return
        }
        perform {
            repository.login(username, password)
            registrationReviewed = false
        }
    }

    fun register(input: SignupInput) = perform {
        repository.register(input.username.trim(), input.password, input.email.trim(), input.weight.trim().toFloat())
        suggestedUsername = input.username.trim()
        registrationReviewed = true
        signingUp = false
    }

    fun signOut() { ApiSession.clear(); error = null; registrationReviewed = false; signingUp = false }

    private fun perform(request: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        viewModelScope.launch {
            try { request() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Exception) { error = failure.userMessage() }
            finally { busy = false }
        }
    }
}
