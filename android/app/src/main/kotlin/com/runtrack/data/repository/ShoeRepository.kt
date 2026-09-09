package com.runtrack.data.repository

import com.runtrack.data.models.*
import javax.inject.Inject

class ShoeRepository @Inject constructor() {
    // TODO: ShoeService를 주입받아 CRUD 로직 구현
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
