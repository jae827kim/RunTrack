package com.runtrack.ui.auth

data class SignupInput(
    val username: String,
    val password: String,
    val passwordConfirmation: String,
    val name: String,
    val weight: String
)

object AuthValidation {
    fun usernameError(value: String): String? = when {
        value.isBlank() -> "아이디를 입력해 주세요."
        value.trim().any { it.isWhitespace() } -> "아이디는 공백 없이 입력해 주세요."
        else -> null
    }

    fun login(username: String, password: String): Map<String, String> = buildMap {
        usernameError(username)?.let { put("username", it) }
        if (password.isBlank()) put("password", "비밀번호를 입력해 주세요.")
    }

    fun signup(input: SignupInput): Map<String, String> = buildMap {
        usernameError(input.username)?.let { put("username", it) }
        when {
            input.password.isBlank() -> put("password", "비밀번호를 입력해 주세요.")
            input.password.length < 8 -> put("password", "비밀번호는 8자 이상 입력해 주세요.")
        }
        when {
            input.passwordConfirmation.isBlank() -> put("confirmation", "비밀번호 확인을 입력해 주세요.")
            input.passwordConfirmation != input.password -> put("confirmation", "비밀번호가 일치하지 않아요.")
        }
        val name = input.name.trim()
        when {
            name.isEmpty() -> put("name", "이름을 입력해 주세요.")
            name.length > 50 -> put("name", "이름은 50자 이내로 입력해 주세요.")
            !name.matches(Regex("[가-힣A-Za-z ]+")) -> put("name", "이름은 한글 또는 영문으로 입력해 주세요.")
        }
        val weight = input.weight.trim()
        when {
            weight.isEmpty() -> put("weight", "체중을 입력해 주세요.")
            !weight.matches(Regex("[0-9]+(?:\\.[0-9]+)?")) ||
                weight.toDoubleOrNull()?.let { it.isFinite() && it > 0 } != true ->
                put("weight", "체중은 0보다 큰 숫자로 입력해 주세요.")
        }
    }
}
