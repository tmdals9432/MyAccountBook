package com.example.gagebu

import androidx.room.*

@Dao
interface DiaryMemoDao {
    @Query("SELECT * FROM diary_memos ORDER BY date DESC, id DESC")
    fun getAllMemos(): List<DiaryMemo>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMemo(memo: DiaryMemo)

    @Update
    fun updateMemo(memo: DiaryMemo)

    @Delete
    fun deleteMemo(memo: DiaryMemo)
}