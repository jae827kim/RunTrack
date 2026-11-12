package com.runtrack.ui.auth

import org.junit.Assert.*
import org.junit.Test

class AuthValidationTest {
    private val valid = SignupInput("demo_user", "test-pass", "test-pass", "runner@example.com", "70.5")

    @Test fun blankLoginPromptsForBothFields() {
        assertEquals(mapOf("username" to "아이디를 입력해 주세요.", "password" to "비밀번호를 입력해 주세요."), AuthValidation.login(" ", " "))
    }

    @Test fun loginChecksRequiredInputBeforeCallingTheServer() {
        assertTrue(AuthValidation.login("demo_user", "sample").isEmpty())
        assertEquals("아이디는 공백 없이 입력해 주세요.", AuthValidation.login("demo user", "sample")["username"])
    }

    @Test fun emptySignupIdentifiesEveryMissingField() {
        assertEquals(setOf("username", "password", "confirmation", "email", "weight"), AuthValidation.signup(SignupInput("", "", "", "", "")).keys)
    }

    @Test fun mismatchedConfirmationIsRejected() {
        assertEquals("비밀번호가 일치하지 않아요.", AuthValidation.signup(valid.copy(passwordConfirmation = "different"))["confirmation"])
        assertTrue(AuthValidation.signup(valid).isEmpty())
    }

    @Test fun passwordLengthIsCheckedBeforeTheConfirmation() {
        assertEquals("비밀번호는 8자 이상 입력해 주세요.", AuthValidation.signup(valid.copy(password = "short", passwordConfirmation = "short"))["password"])
    }

    @Test fun emailMustHaveAnAddressAndDomain() {
        listOf("runner@example.com", " user+run@example.co.kr ").forEach { assertFalse(AuthValidation.signup(valid.copy(email = it)).containsKey("email")) }
        listOf("", "runner", "@example.com", "user@", "user name@example.com").forEach { assertTrue(AuthValidation.signup(valid.copy(email = it)).containsKey("email")) }
    }
    @Test fun weightRejectsNonNumbersNonFiniteNumbersAndZero() {
        listOf("NaN", "Infinity", "1e2", "70kg", "-1", "0", "0.0").forEach {
            assertTrue("Expected invalid weight: $it", AuthValidation.signup(valid.copy(weight = it)).containsKey("weight"))
        }
        listOf("70", "70.5", " 70.5 ").forEach { assertFalse(AuthValidation.signup(valid.copy(weight = it)).containsKey("weight")) }
    }

    @Test fun usernameCheckNeverClaimsAccountAvailability() {
        assertNull(AuthValidation.usernameError("demo_user"))
        assertEquals("아이디를 입력해 주세요.", AuthValidation.usernameError(""))
        assertNotNull(AuthValidation.usernameError("demo user"))
    }
}
