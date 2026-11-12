package com.runtrack.ui.auth

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.runtrack.MainActivity
import com.runtrack.data.api.ApiSession
import com.runtrack.data.local.LocalTrackStore
import com.runtrack.data.repository.LocalAdminTrackRepository
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.File
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class OfflineAdminFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()
    private val modelName = "Offline_${UUID.randomUUID()}"
    private val runNote = "OfflineRun_${UUID.randomUUID()}"
    private fun repository() = LocalAdminTrackRepository(LocalTrackStore(rule.activity, LocalAdminTrackRepository.PREFERENCES_NAME))
    private fun data() = runBlocking { repository().load() }
    private fun ready() = rule.waitUntil(10000) { rule.onAllNodesWithTag("nav_home").fetchSemanticsNodes().isNotEmpty() }
    @Before fun resetSession() { ApiSession.clear() }
    @After fun cleanupOnlyThisTestsRecords() {
        runBlocking {
            val repository = repository()
            repository.load().runs.filter { it.notes == runNote }.forEach { repository.deleteRun(it.id) }
            repository.load().shoes.filter { it.model == modelName }.forEach { repository.deleteShoe(it.id) }
        }
        ApiSession.clear()
    }
    private fun login(password: String = "admin1234") {
        rule.onNodeWithTag("loginUsername").performScrollTo().performTextReplacement("admin")
        rule.onNodeWithTag("loginPassword").performScrollTo().performTextReplacement(password)
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("loginSubmit").performScrollTo().performClick()
    }

    @Test fun offlineLoginRejectsWrongPasswordAndPersistsRecordsAfterLogoutAndRotation() {
        login("wrong-password")
        rule.onNodeWithTag("authError").assertIsDisplayed()
        rule.onNodeWithTag("nav_home").assertDoesNotExist()
        login(); ready()
        rule.onNodeWithTag("adminMode").assertIsDisplayed()
        rule.onNodeWithText("관리자 계정").assertIsDisplayed()
        assertTrue(ApiSession.state.value!!.isOfflineAdmin)
        assertNull(ApiSession.token())
        capture("admin-home")

        rule.onNodeWithTag("nav_shoes").performClick()
        rule.onNodeWithTag("headerAction").performClick()
        rule.onNodeWithTag("brand").performScrollTo().performTextInput("Local")
        rule.onNodeWithTag("model").performScrollTo().performTextInput(modelName)
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("save").performClick(); ready()
        val shoe = data().shoes.single { it.model == modelName }
        UUID.fromString(shoe.id)

        rule.onNodeWithTag("nav_runEditor").performClick()
        rule.onNodeWithTag("duration").performScrollTo().performTextInput("32.5")
        rule.onNodeWithTag("runDistance").performScrollTo().performTextInput("5.2")
        rule.onNodeWithTag("runNotes").performScrollTo().performTextInput(runNote)
        Espresso.closeSoftKeyboard()
        rule.onNodeWithTag("runShoe").performScrollTo().performClick()
        rule.onNodeWithText(shoe.displayName).performScrollTo().performClick()
        rule.onNodeWithTag("save").performClick(); ready()
        val run = data().runs.single { it.notes == runNote }
        assertEquals(shoe.id, run.shoeId)
        assertEquals(32.5, run.durationMinutes, 0.0)
        assertEquals(5.2, data().shoeDistance(shoe), 0.00001)
        rule.activityRule.scenario.recreate(); ready()
        rule.onNodeWithTag("adminMode").assertIsDisplayed()
        rule.onNodeWithTag("nav_home").performClick()
        rule.onNodeWithTag("settings").performClick()
        rule.onNodeWithTag("logout").performClick()
        rule.onNodeWithTag("loginScreen").assertIsDisplayed()
        login(); ready()
        assertEquals(run, data().runs.single { it.id == run.id })
        assertEquals(shoe, data().shoes.single { it.id == shoe.id })
        rule.onNodeWithTag("nav_shoes").performClick()
        rule.onNodeWithText(modelName).performScrollTo().assertIsDisplayed()
    }

    private fun capture(name: String) {
        rule.waitForIdle()
        val directory = File(rule.activity.getExternalFilesDir(null), "admin-review").apply { mkdirs() }
        File(directory, "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
