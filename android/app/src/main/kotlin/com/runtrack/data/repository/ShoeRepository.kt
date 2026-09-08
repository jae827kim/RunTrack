package com.runtrack.data.repository

import com.runtrack.data.api.RetrofitClient
import com.runtrack.data.models.*
import javax.inject.Inject

class ShoeRepository @Inject constructor() {
    private val shoeService = RetrofitClient.shoeService

    suspend fun createShoe(request: ShoeCreateRequest): Result<Shoe> = try {
        val response = shoeService.createShoe(request)
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("신발 등록 실패: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getShoes(): Result<List<Shoe>> = try {
        val response = shoeService.getShoes()
        if (response.isSuccessful) {
            Result.success(response.body() ?: emptyList())
        } else {
            Result.failure(Exception("신발 목록 조회 실패: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getShoe(id: Int): Result<Shoe> = try {
        val response = shoeService.getShoe(id)
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("신발 조회 실패: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun updateShoe(id: Int, request: ShoeUpdateRequest): Result<Shoe> = try {
        val response = shoeService.updateShoe(id, request)
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("신발 수정 실패: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun deleteShoe(id: Int): Result<Unit> = try {
        val response = shoeService.deleteShoe(id)
        if (response.isSuccessful) {
            Result.success(Unit)
        } else {
            Result.failure(Exception("신발 삭제 실패: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }

    suspend fun getShoeStats(id: Int): Result<ShoeStats> = try {
        val response = shoeService.getShoeStats(id)
        if (response.isSuccessful) {
            Result.success(response.body()!!)
        } else {
            Result.failure(Exception("신발 통계 조회 실패: ${response.message()}"))
        }
    } catch (e: Exception) {
        Result.failure(e)
    }
}
