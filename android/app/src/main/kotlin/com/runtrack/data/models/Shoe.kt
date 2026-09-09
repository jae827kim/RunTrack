package com.runtrack.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Shoe(
    // TODO: PM을 참고하여 신발 모델 단연 정의
)

@Serializable
data class ShoeCreateRequest(
    // TODO: 신발 등록 숲고 단연 정의
)
    val notes: String? = null,
)

@Serializable
data class ShoeUpdateRequest(
    val brand: String? = null,
    val model: String? = null,
    val size: String? = null,
    val color: String? = null,
    val purchasePriceWon: Int? = null,
    val condition: String? = null,
    val notes: String? = null,
)

@Serializable
data class ShoeStats(
    val cumulativeKm: Float,
    val runCount: Int,
    val condition: String,
)
