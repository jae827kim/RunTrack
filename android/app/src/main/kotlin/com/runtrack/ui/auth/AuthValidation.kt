package com.runtrack.ui.auth

data class SignupInput(
    val username: String,
    val password: String,
    val passwordConfirmation: String,
    val email: String,
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
        val email = input.email.trim()
        when {
            email.isEmpty() -> put("email", "이메일을 입력해 주세요.")
            !email.matches(Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+")) -> put("email", "올바른 이메일 주소를 입력해 주세요.")
        }
        val weight = input.weight.trim()
        when {
            weight.isEmpty() -> put("weight", "체중을 입력해 주세요.")
            !weight.matches(Regex("[0-9]+(?:\\.[0-9]+)?")) ||
                weight.toFloatOrNull()?.let { it.isFinite() && it > 0 } != true ->
                put("weight", "체중은 0보다 큰 숫자로 입력해 주세요.")
        }
    }
}
