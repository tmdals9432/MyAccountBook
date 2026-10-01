package com.example.gagebu

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val type: String,        // "EXPENSE"(지출) 또는 "INCOME"(수입)
    val amount: Long,        // 금액
    val category: String,    // 카테고리 (예: 식비, 교통 등)
    val memo: String,        // 메모
    val date: String         // 날짜 (예: "2026-08-12")
)