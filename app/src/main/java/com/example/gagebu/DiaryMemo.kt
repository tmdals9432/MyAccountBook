package com.example.gagebu

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diary_memos")
data class DiaryMemo(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val date: String,       // yyyy-MM-dd
    val title: String,      // 제목
    val content: String     // 본문 내용
)