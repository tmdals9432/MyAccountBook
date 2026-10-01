package com.example.gagebu

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "assets")
data class Asset(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val group: String,      // 현금, 은행, 신용카드, 체크카드, 선불식카드, 저축, 투자, 마이너스통장, 대출, 보험, 기타
    val name: String,       // 자산 이름 (예: KB주거래)
    val amount: Long,       // 잔액/금액
    val memo: String = ""   // 메모
)