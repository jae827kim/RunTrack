package com.runtrack.data.api

import com.runtrack.data.models.LoginResponse
import com.runtrack.data.models.User
import com.runtrack.ui.auth.OfflineAdminAccount
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class SignedInSession(val user: User, val accessToken: String, val expiresAtMillis: Long,
                           val instanceId: String = UUID.randomUUID().toString(), val isOfflineAdmin: Boolean = false)

/** Memory only: a fresh app launch always requires login; credentials never enter preferences. */
object ApiSession {
    private val mutable = MutableStateFlow<SignedInSession?>(null)
    val state = mutable.asStateFlow()

    fun start(token: LoginResponse, user: User, nowMillis: Long = System.currentTimeMillis()) {
        require(!token.accessToken.isNullOrBlank() && token.tokenType.equals("bearer", true) && token.expiresIn > 0)
        require(user.id > 0 && !user.username.isNullOrBlank())
        mutable.value = SignedInSession(user, token.accessToken, nowMillis + token.expiresIn.toLong() * 1000)
    }

    fun token(nowMillis: Long = System.currentTimeMillis()): String? {
        val current = mutable.value ?: return null
        if (current.isOfflineAdmin) return null
        if (nowMillis >= current.expiresAtMillis) {
            invalidate(current.accessToken)
            return null
        }
        return current.accessToken
    }

    fun invalidate(token: String) {
        val current = mutable.value
        if (current?.accessToken == token) mutable.compareAndSet(current, null)
    }

    fun clear() { mutable.value = null }

    fun startOfflineAdmin() {
        check(OfflineAdminAccount.enabled)
        mutable.value = SignedInSession(User(-1, OfflineAdminAccount.USERNAME, "", createdAt = "", updatedAt = ""),
            "", Long.MAX_VALUE, isOfflineAdmin = true)
    }
}
