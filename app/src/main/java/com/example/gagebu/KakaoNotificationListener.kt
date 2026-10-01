package com.example.gagebu

import android.app.Notification
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.concurrent.thread

class KakaoNotificationListener : NotificationListenerService() {

    companion object {
        const val ACTION_DATA_UPDATED = "com.example.gagebu.ACTION_DATA_UPDATED"
        private const val TAG = "KakaoListener"

        // 🏪 브랜드 및 상호명 키워드 매핑 테이블 (상호명 -> 카테고리)
        val BRAND_CATEGORY_MAP = mapOf(
            // 식비 (카페, 프랜차이즈, 배달, 외식)
            "스타벅스" to "식비", "스벅" to "식비", "메가커피" to "식비", "빽다방" to "식비",
            "투썸" to "식비", "이디야" to "식비", "컴포즈" to "식비", "할리스" to "식비",
            "맥도날드" to "식비", "버거킹" to "식비", "롯데리아" to "식비", "맘스터치" to "식비",
            "배달의민족" to "식비", "배민" to "식비", "요기요" to "식비", "쿠팡이츠" to "식비",
            "파리바게뜨" to "식비", "뚜레쥬르" to "식비", "서브웨이" to "식비", "식당" to "식비", "카페" to "식비",

            // 생활용품 (편의점, 마트, 잡화)
            "CU" to "생활용품", "씨유" to "생활용품", "GS25" to "생활용품", "지에스" to "생활용품",
            "세븐일레븐" to "생활용품", "이마트24" to "생활용품", "다이소" to "생활용품",
            "올리브영" to "생활용품", "이마트" to "생활용품", "홈플러스" to "생활용품", "롯데마트" to "생활용품",

            // 쇼핑 (이커머스, 패션)
            "쿠팡" to "쇼핑", "네이버페이" to "쇼핑", "11번가" to "쇼핑", "무신사" to "쇼핑",
            "지마켓" to "쇼핑", "옥션" to "쇼핑", "에이블리" to "쇼핑", "지그재그" to "쇼핑",

            // 교통비 (대중교통, 택시, 주유)
            "티머니" to "교통비", "카카오택시" to "교통비", "카카오 T" to "교통비", "코레일" to "교통비",
            "택시" to "교통비", "주유소" to "교통비", "GS칼텍스" to "교통비", "SK에너지" to "교통비", "S-OIL" to "교통비",

            // 여가/취미
            "CGV" to "여가/취미", "메가박스" to "여가/취미", "롯데시네마" to "여가/취미",
            "넷플릭스" to "여가/취미", "유튜브" to "여가/취미", "PC방" to "여가/취미",

            // 의료/건강
            "약국" to "의료/건강", "병원" to "의료/건강", "의원" to "의료/건강", "치과" to "의료/건강"
        )
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val packageName = sbn?.packageName ?: return

        if (packageName == "com.kakao.talk") {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            Log.d(TAG, "카톡 알림 수신 -> 발신자: $title, 내용: $text")

            val isPaymentMessage = (text.contains("결제") || text.contains("승인") || text.contains("출금") || text.contains("입금")) && text.contains("원")

            if (isPaymentMessage) {
                parseAndSaveKakaoPay(title, text)
            }
        }
    }

    private fun parseAndSaveKakaoPay(title: String, body: String) {
        try {
            // 1. 금액 파싱
            val amountPattern = Pattern.compile("([0-9,]+)\\s*원")
            val amountMatcher = amountPattern.matcher(body)

            var amount = 0L
            if (amountMatcher.find()) {
                val amountStr = amountMatcher.group(1)?.replace(",", "") ?: "0"
                amount = amountStr.toLongOrNull() ?: 0L
            }

            if (amount <= 0 || amount > 100_000_000L) return

            val type = if (body.contains("입금")) "INCOME" else "EXPENSE"

            // 💡 2. 상호명 매칭 및 카테고리 자동 판별
            var detectedCategory = if (type == "INCOME") "기타" else "기타"
            var detectedBrand = ""

            // 알림 본문 + 발신자명 전체에서 매핑 키워드 검색
            val fullText = "$title $body"
            for ((brand, category) in BRAND_CATEGORY_MAP) {
                if (fullText.contains(brand, ignoreCase = true)) {
                    detectedCategory = category
                    detectedBrand = brand
                    break
                }
            }

            // 💡 3. 메모 생성 (인식된 상호명이 있으면 상호명 우선, 없으면 발신자명)
            val memo = if (detectedBrand.isNotEmpty()) {
                "$detectedBrand (카톡)"
            } else if (title.isNotEmpty()) {
                "$title (카톡)"
            } else {
                "카카오페이 결제"
            }

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val transaction = Transaction(
                type = type,
                amount = amount,
                category = detectedCategory,
                memo = memo,
                date = currentDate
            )

            // DB 저장
            val db = AppDatabase.getDatabase(applicationContext)
            thread {
                db.transactionDao().insertTransaction(transaction)
                Log.d(TAG, "DB 저장 완료: $transaction")

                val intent = Intent(ACTION_DATA_UPDATED).apply {
                    setPackage(packageName)
                }
                sendBroadcast(intent)
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val typeStr = if (type == "INCOME") "입금" else "결제"
                Toast.makeText(applicationContext, "💬 [$detectedCategory] $memo ${amount}원 자동 등록!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "파싱 에러: ${e.message}")
        }
    }
}