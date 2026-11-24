package com.runtrack.data.api

import com.google.gson.JsonParser
import retrofit2.Response
import java.io.IOException

class ApiFailure(val status: Int, message: String) : Exception(message)

fun Response<*>.requireSuccess() {
    if (isSuccessful) return
    val detail = runCatching { JsonParser.parseString(errorBody()?.string().orEmpty()).asJsonObject.get("detail") }.getOrNull()
    val text = if (detail?.isJsonPrimitive == true) detail.asString else ""
    val message = when (code()) {
        401 -> if (text == "Invalid username or password") "아이디 또는 비밀번호가 올바르지 않아요." else "로그인이 만료됐어요. 다시 로그인해 주세요."
        409 -> if (text.contains("Username or email")) "이미 사용 중인 아이디 또는 이메일이에요." else "다른 요청과 충돌했어요. 목록을 새로 불러와 주세요."
        404 -> "기록 또는 신발을 찾을 수 없어요. 목록을 새로 불러와 주세요."
        422 -> "서버가 입력값을 저장할 수 없어요. 입력 형식과 필수 항목을 확인해 주세요."
        501 -> "아직 준비 중인 서버 기능이에요."
        else -> "서버 요청에 실패했어요. 잠시 후 다시 시도해 주세요."
    }
    throw ApiFailure(code(), message)
}

fun <T : Any> Response<T>.requiredBody(): T {
    requireSuccess()
    return body() ?: throw ApiFailure(0, "서버 응답이 비어 있어요. 다시 시도해 주세요.")
}

fun Throwable.userMessage(): String = when (this) {
    is ApiFailure -> message.orEmpty()
    is IOException -> "서버에 연결하지 못했어요. 서버 실행 상태와 네트워크를 확인해 주세요."
    else -> "응답을 처리하지 못했어요. 다시 시도해 주세요."
}
