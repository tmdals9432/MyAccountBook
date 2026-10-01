package com.example.gagebu

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface TransactionDao {
    // 전체 내역 최신순 조회
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactions(): List<Transaction>

    // 내역 추가
    @Insert
    fun insertTransaction(transaction: Transaction)

    // 내역 삭제
    @Delete
    fun deleteTransaction(transaction: Transaction)
}