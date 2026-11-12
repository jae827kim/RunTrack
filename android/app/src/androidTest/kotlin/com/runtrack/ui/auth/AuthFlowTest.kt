package com.runtrack.ui.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runtrack.MainActivity
import com.runtrack.data.api.ApiSession
import com.runtrack.data.local.ShoeDraft
import com.runtrack.data.repository.RemoteTrackRepository
import com.runtrack.ui.*
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AuthFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    @Before fun clearSession() { ApiSession.clear() }
    @After fun logout() { ApiSession.clear() }
    private fun home() = rule.waitUntil(30000) { rule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }
    private fun login(account: ApiTestAccount) {
        rule.onNodeWithTag("loginUsername").performScrollTo().performTextReplacement(account.username)
        rule.onNodeWithTag("loginPassword").performScrollTo().performTextReplacement(account.password)
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("loginSubmit").performScrollTo().performClick(); home()
    }

    @Test fun signupAndLoginUseTheServerAndRejectWrongPasswords() {
        rule.onNodeWithTag("loginScreen").assertIsDisplayed()
        rule.onNodeWithTag("loginSubmit").performClick()
        rule.onNodeWithText("아이디를 입력해 주세요.").assertExists()
        rule.onNodeWithText("비밀번호를 입력해 주세요.").assertExists()
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("openSignup").performScrollTo().performClick()
        rule.onNodeWithTag("signupSubmit").performClick()
        rule.onNodeWithText("이메일을 입력해 주세요.").assertExists()
        val username = "signup_${UUID.randomUUID().toString().replace("-", "")}"
        rule.onNodeWithTag("signupUsername").performScrollTo().performTextInput(username)
        rule.onNodeWithTag("signupPassword").performScrollTo().performTextInput("test-pass")
        rule.onNodeWithTag("signupConfirmation").performScrollTo().performTextInput("different")
        rule.onNodeWithTag("signupEmail").performScrollTo().performTextInput("$username@example.test")
        rule.onNodeWithTag("signupWeight").performScrollTo().performTextInput("70.5")
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("signupSubmit").performClick()
        rule.onNodeWithText("비밀번호가 일치하지 않아요.").assertExists()
        rule.onNodeWithTag("signupConfirmation").performScrollTo().performTextReplacement("test-pass")
        Espresso.closeSoftKeyboard(); rule.onNodeWithTag("signupSubmit").performClick()
        rule.waitUntil(30000) { rule.onAllNodesWithTag("registrationReviewed").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("loginUsername").assertTextContains(username)
        rule.onNodeWithTag("loginPassword").performScrollTo().performTextInput("wrong-password")
        Espresso.closeSoftKeyboard(); rule.onNodeWithTag("loginSubmit").performScrollTo().performClick()
        rule.waitUntil(30000) { rule.onAllNodesWithTag("authError").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("nav_home").assertDoesNotExist()
        login(ApiTestAccount(username, "test-pass", "$username@example.test"))
        Assert.assertEquals(username, ApiSession.state.value!!.user.username)
        rule.activityRule.scenario.recreate(); home()
        rule.onNodeWithTag("settings").performClick(); rule.onNodeWithTag("logout").performClick()
        rule.onNodeWithTag("loginScreen").assertIsDisplayed()
    }

    @Test fun switchingAccountsDoesNotShowAnotherUsersRecords() {
        val first = newApiTestAccount()
        val second = newApiTestAccount()
        login(first)
        runBlocking { RemoteTrackRepository().saveShoe(ShoeDraft(brand = "Private", model = "First account"), null) }
        rule.activityRule.scenario.recreate(); home()
        rule.onNodeWithTag("nav_shoes").performClick()
        rule.onNodeWithText("First account").assertIsDisplayed()
        rule.onNodeWithTag("nav_home").performClick()
        rule.onNodeWithTag("settings").performClick(); rule.onNodeWithTag("logout").performClick()
        login(second)
        rule.onNodeWithTag("nav_shoes").performClick()
        rule.onNodeWithText("First account").assertDoesNotExist()
        rule.onNodeWithText("등록된 신발이 없어요.").assertIsDisplayed()
    }
}
