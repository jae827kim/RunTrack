package com.runtrack.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runtrack.MainActivity
import com.runtrack.data.api.ApiSession
import com.runtrack.data.repository.RemoteTrackRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrackFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private fun data() = runBlocking { RemoteTrackRepository().load() }
    private fun ready() = rule.waitUntil(30000) { rule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }

    @Before fun loginToLocalApi() {
        ApiSession.clear()
        val account = newApiTestAccount()
        rule.onNodeWithTag("loginUsername").performTextInput(account.username)
        rule.onNodeWithTag("loginPassword").performTextInput(account.password)
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("loginSubmit").performScrollTo().performClick()
        ready()
    }
    @After fun logout() { ApiSession.clear() }

    @Test fun savesEditsAndDeletesRecordsOnTheRealServer() {
        rule.onNodeWithTag("nav_shoes").performClick()
        rule.onNodeWithTag("headerAction").performClick()
        rule.onNodeWithTag("save").performClick()
        rule.onNodeWithText("브랜드를 입력해 주세요.").assertIsDisplayed()
        rule.onNodeWithTag("brand").performScrollTo().performTextInput("API 테스트 브랜드")
        rule.onNodeWithTag("model").performScrollTo().performTextInput("API 테스트 모델")
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("size").performScrollTo().performClick()
        rule.onNodeWithText("260 mm").performScrollTo().performClick()
        rule.onNodeWithTag("save").performClick(); ready()
        val shoe = data().shoes.single()
        assertEquals("260 mm", shoe.size)
        assertTrue(shoe.id.toInt() > 0)

        rule.onNodeWithTag("nav_runEditor").performClick()
        rule.onNodeWithTag("save").performClick()
        rule.onNodeWithText("0보다 큰 러닝 시간을 입력해 주세요.").assertIsDisplayed()
        rule.onNodeWithTag("duration").performScrollTo().performTextInput("32")
        rule.onNodeWithTag("runDistance").performScrollTo().performTextInput("5.2")
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("runShoe").performScrollTo().performClick()
        rule.onNodeWithText(shoe.displayName).performClick()
        rule.onNodeWithTag("rating").performScrollTo().performClick()
        rule.onNodeWithText("great").performClick()
        rule.onNodeWithTag("save").performClick(); ready()
        val saved = data()
        assertEquals(5.2, saved.shoeDistance(saved.shoes.single()), 0.00001)
        assertEquals(1, saved.shoeRunCount(saved.shoes.single()))
        assertEquals("great", saved.runs.single().rating)

        rule.onNodeWithTag("nav_home").performClick()
        rule.onNodeWithTag("weekDistance").assert(hasAnyDescendant(hasText("5.2 km")))
        rule.activityRule.scenario.recreate(); ready()
        rule.onNodeWithTag("weekDistance").assert(hasAnyDescendant(hasText("5.2 km")))
        rule.onNodeWithTag("nav_runs").performClick()
        rule.onNodeWithText("편집").performScrollTo().performClick()
        rule.onNodeWithTag("duration").performScrollTo().performTextReplacement("42")
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("save").performClick(); ready()
        assertEquals(42.0, data().runs.single().durationMinutes, 0.0)
        rule.onNodeWithText("삭제").performScrollTo().performClick()
        rule.onAllNodesWithText("삭제").onLast().performClick()
        rule.waitUntil(30000) { data().runs.isEmpty() }; ready()
        val deleted = data()
        assertEquals(0.0, deleted.shoeDistance(deleted.shoes.single()), 0.0)
        assertEquals(0, deleted.shoeRunCount(deleted.shoes.single()))
        rule.onNodeWithTag("nav_shoes").performClick()
        rule.onNodeWithText("삭제").performScrollTo().performClick()
        rule.onAllNodesWithText("삭제").onLast().performClick()
        rule.waitUntil(30000) { data().shoes.isEmpty() }
    }

    @Test fun startsAndFinishesARunningSessionThroughTheHomeScreen() {
        rule.onNodeWithTag("startRunning").performScrollTo().performClick()
        rule.waitUntil(30000) { rule.onAllNodesWithTag("finishRunning").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("finishRunning").performScrollTo().performClick()
        rule.onNodeWithTag("finishDistance").performTextInput("5.2")
        rule.onNodeWithTag("finishNotes").performTextInput("세션 연동 테스트")
        rule.onNodeWithTag("finishSessionConfirm").performClick()
        rule.waitUntil(30000) { data().runs.size == 1 }

        val saved = data().runs.single()
        assertEquals(5.2, saved.distanceKm, 0.00001)
        assertEquals("세션 연동 테스트", saved.notes)
        rule.onNodeWithTag("startRunning").assertExists()
    }
}
