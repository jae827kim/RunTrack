package com.runtrack.ui

import com.runtrack.BuildConfig
import com.runtrack.data.api.RetrofitClient
import com.runtrack.data.api.requiredBody
import com.runtrack.data.models.RegisterRequest
import kotlinx.coroutines.runBlocking
import java.util.UUID

data class ApiTestAccount(val username: String, val password: String, val email: String)

/** Only creates disposable accounts on an explicitly local development server. */
fun newApiTestAccount(): ApiTestAccount = runBlocking {
    check(BuildConfig.DEBUG && Regex("http://(10\\.0\\.2\\.2|127\\.0\\.0\\.1|localhost):[0-9]+/").matches(BuildConfig.API_BASE_URL))
    val username = "ui_${UUID.randomUUID().toString().replace("-", "")}"
    val account = ApiTestAccount(username, "ui-test-password", "$username@example.test")
    RetrofitClient.userService.register(RegisterRequest(username, account.email, account.password, weightKg = 70.5f)).requiredBody()
    account
}
