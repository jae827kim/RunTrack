package com.runtrack.ui.auth

import android.graphics.Bitmap
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.runtrack.ui.theme.RunTrackTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class AuthRenderTest {
    @get:Rule val rule = createComposeRule()

    @Test fun loginRendersAndPromptsForMissingInput() {
        rule.setContent { RunTrackTheme { Surface { LoginScreen(onContinue = { _, _ -> }, onSignup = {}) } } }
        capture("login")
        rule.onNodeWithTag("loginSubmit").performClick()
        Espresso.closeSoftKeyboard()
        rule.onNodeWithText("아이디를 입력해 주세요.").assertIsDisplayed()
        rule.onNodeWithText("비밀번호를 입력해 주세요.").assertIsDisplayed()
        capture("login-required")
    }

    @Test fun signupRendersAndScrollsToTheLastField() {
        rule.setContent { RunTrackTheme { Surface { SignupScreen(onBack = {}, onComplete = {}) } } }
        rule.onNodeWithTag("signupUsername").assertExists()
        capture("signup-top")
        rule.onNodeWithTag("signupWeight").performScrollTo()
        rule.onNodeWithTag("signupWeight").assertIsDisplayed()
        rule.onNodeWithTag("signupSubmit").assertIsDisplayed()
        capture("signup-bottom")
    }

    @Test fun signupDoesNotClaimUsernameIsAvailableBeforeServerRegistration() {
        rule.setContent { RunTrackTheme { Surface { SignupScreen(onBack = {}, onComplete = {}) } } }
        rule.onNodeWithTag("checkUsername").assertDoesNotExist()
        rule.onNodeWithTag("signupUsername").performTextInput("test_runner")
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("usernameAvailable").assertDoesNotExist()
        rule.onNodeWithTag("signupSubmit").assertIsDisplayed()
        capture("signup-server-validated")
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "auth-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
