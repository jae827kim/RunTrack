package com.runtrack.ui.auth

import com.runtrack.data.api.*
import com.runtrack.data.models.*
import com.runtrack.data.repository.AuthRepository
import org.junit.*
import org.junit.Assert.*
import retrofit2.Response

class OfflineAdminTest {
    private val forbiddenService = object : UserService {
        override suspend fun register(request: RegisterRequest): Response<User> = error("Unexpected server registration")
        override suspend fun login(request: LoginRequest): Response<LoginResponse> = error("Unexpected server login")
        override suspend fun getProfile(authorization: String?): Response<User> = error("Unexpected server profile")
    }
    @Before fun reset() = ApiSession.clear()
    @After fun clear() = ApiSession.clear()

    @Test fun correctAdminCredentialsEnterWithoutAnyServerCallOrToken() {
        val model = AuthViewModel(AuthRepository(forbiddenService))
        model.login("admin", "admin1234")
        assertTrue(model.session.value!!.isOfflineAdmin)
        assertEquals("admin", model.session.value!!.user.username)
        assertNull(ApiSession.token(nowMillis = Long.MAX_VALUE))
        assertNotNull(model.session.value)
        assertNull(model.error)
        model.signOut()
        assertNull(model.session.value)
    }

    @Test fun wrongAdminPasswordIsRejectedLocally() {
        val model = AuthViewModel(AuthRepository(forbiddenService))
        model.login("admin", "wrong-password")
        assertNull(model.session.value)
        assertEquals("아이디 또는 비밀번호가 올바르지 않아요.", model.error)
    }

    @Test fun offlineCredentialsCannotBeUsedWhenDeveloperModeIsDisabled() {
        assertFalse(OfflineAdminAccount.matches("admin", "admin1234", enabled = false))
        assertFalse(OfflineAdminAccount.matches("other", "admin1234", enabled = true))
    }

    @Test fun serverAccountNamedAdminDoesNotGainOfflineMode() {
        ApiSession.start(LoginResponse("server-token", "bearer", 1800),
            User(1, "admin", "admin@example.test", createdAt = "", updatedAt = ""))
        assertFalse(ApiSession.state.value!!.isOfflineAdmin)
        assertEquals("server-token", ApiSession.token())
    }
}
