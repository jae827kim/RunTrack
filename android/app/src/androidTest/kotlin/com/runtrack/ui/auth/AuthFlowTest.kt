package com.runtrack.ui.auth

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runtrack.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuthFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun appStartsAtLoginValidatesSignupAndEntersDemoHome() {
        rule.onNodeWithTag("loginScreen").assertIsDisplayed()
        rule.onNodeWithTag("nav_home").assertDoesNotExist()
        rule.onNodeWithTag("loginSubmit").performClick()
        rule.onNodeWithText("아이디를 입력해 주세요.").assertExists()
        rule.onNodeWithText("비밀번호를 입력해 주세요.").assertExists()
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("openSignup").performScrollTo().performClick()
        rule.onNodeWithTag("signupSubmit").performClick()
        rule.onNodeWithText("이름을 입력해 주세요.").assertExists()
        rule.onNodeWithText("체중을 입력해 주세요.").assertExists()
        rule.onNodeWithTag("signupUsername").performTextInput("demo_user")
        rule.onNodeWithTag("signupPassword").performScrollTo().performTextInput("test-pass")
        rule.onNodeWithTag("signupConfirmation").performScrollTo().performTextInput("different")
        rule.onNodeWithTag("signupName").performScrollTo().performTextInput("김재원")
        rule.onNodeWithTag("signupWeight").performScrollTo().performTextInput("70.5")
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("signupSubmit").performClick()
        rule.onNodeWithText("비밀번호가 일치하지 않아요.").assertExists()
        rule.onNodeWithTag("signupConfirmation").performScrollTo().performTextReplacement("test-pass")
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("signupSubmit").performClick()
        rule.onNodeWithTag("registrationReviewed").assertExists()
        rule.onNodeWithTag("loginUsername").assertTextContains("demo_user")
        rule.onNodeWithTag("loginSubmit").performClick()
        rule.onNodeWithText("비밀번호를 입력해 주세요.").assertExists()
        rule.onNodeWithTag("loginPassword").performTextInput("demo-only")
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("loginSubmit").performClick()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10000) { rule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("logout").performClick()
        rule.onNodeWithTag("loginScreen").assertIsDisplayed()
        rule.onNodeWithTag("nav_home").assertDoesNotExist()
    }
}
