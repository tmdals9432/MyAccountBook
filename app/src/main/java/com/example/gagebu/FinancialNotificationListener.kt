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
import java.util.concurrent.ConcurrentHashMap
import java.util.regex.Pattern
import kotlin.concurrent.thread

class FinancialNotificationListener : NotificationListenerService() {

    companion object {
        const val ACTION_DATA_UPDATED = "com.example.gagebu.ACTION_DATA_UPDATED"
        private const val TAG = "FinanceListener"

        private val recentTransactionCache = ConcurrentHashMap<String, Long>()
        private const val DUPLICATE_PREVENTION_WINDOW_MS = 3000L

        private val SUPPORTED_FINANCE_PACKAGES = setOf(
            "com.kakao.talk",                             // 카카오톡
            "viva.republica.toss",                        // 토스
            "com.kakaobank.channel",                      // 카카오뱅크
            "com.kbstar.kbbank",                          // KB스타뱅킹
            "com.shinhan.sbanking",                       // 신한SOL뱅크
            "com.wooribank.smart.npib",                   // 우리WON뱅킹
            "com.hanabank.ebk.channel.android.hananbank", // 하나원큐
            "nh.smart.banking",                           // NH스마트뱅킹
            "com.samsung.android.spay",                   // 삼성페이/삼성월렛
            "com.kbcard.cxh.appcard",                     // KB Pay
            "com.shcard.smartpay",                        // 신한카드
            "com.hyundaicard.appcard"                     // 현대카드
        )

        // 🚫 1. 스팸/이벤트/오픈채팅 블랙리스트 키워드 (포함 시 즉시 무시)
        private val SPAM_BLACKLIST = listOf(
            "추천인", "삽니다", "팝니다", "보답", "이벤트", "이벱", "수익", "부업", "재택",
            "선착순", "입장", "링크", "오픈채팅", "옵챗", "방장", "지급", "에어드랍",
            "무료", "텔레", "카톡", "문의", "텔레그램", "페이백", "꽁돈", "당첨", "축하"
        )

        // 🏪 브랜드/상호명 키워드 매핑 테이블
        val BRAND_CATEGORY_MAP = mapOf(
            "스타벅스" to "식비", "스벅" to "식비", "메가커피" to "식비", "빽다방" to "식비",
            "투썸" to "식비", "이디야" to "식비", "컴포즈" to "식비", "할리스" to "식비",
            "맥도날드" to "식비", "버거킹" to "식비", "롯데리아" to "식비", "맘스터치" to "식비",
            "배달의민족" to "식비", "배민" to "식비", "요기요" to "식비", "쿠팡이츠" to "식비",
            "파리바게뜨" to "식비", "뚜레쥬르" to "식비", "서브웨이" to "식비", "식당" to "식비", "카페" to "식비",

            "CU" to "생활용품", "씨유" to "생활용품", "GS25" to "생활용품", "지에스" to "생활용품",
            "세븐일레븐" to "생활용품", "이마트24" to "생활용품", "다이소" to "생활용품",
            "올리브영" to "생활용품", "이마트" to "생활용품", "홈플러스" to "생활용품", "롯데마트" to "생활용품",

            "쿠팡" to "쇼핑", "네이버페이" to "쇼핑", "11번가" to "쇼핑", "무신사" to "쇼핑",
            "지마켓" to "쇼핑", "옥션" to "쇼핑", "에이블리" to "쇼핑", "지그재그" to "쇼핑",

            "티머니" to "교통비", "카카오택시" to "교통비", "카카오 T" to "교통비", "코레일" to "교통비",
            "택시" to "교통비", "주유소" to "교통비", "GS칼텍스" to "교통비", "SK에너지" to "교통비", "S-OIL" to "교통비",

            "CGV" to "여가/취미", "메가박스" to "여가/취미", "롯데시네마" to "여가/취미",
            "넷플릭스" to "여가/취미", "유튜브" to "여가/취미", "PC방" to "여가/취미",

            "약국" to "의료/건강", "병원" to "의료/건강", "의원" to "의료/건강", "치과" to "의료/건강"
        )

        fun isDuplicate(amount: Long, type: String): Boolean {
            val now = System.currentTimeMillis()
            val cacheKey = "${amount}_$type"
            val lastTime = recentTransactionCache[cacheKey]

            if (lastTime != null && (now - lastTime) < DUPLICATE_PREVENTION_WINDOW_MS) {
                Log.d(TAG, "🚨 중복 알림 차단: $cacheKey")
                return true
            }

            recentTransactionCache[cacheKey] = now
            return false
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        val packageName = sbn?.packageName ?: return

        if (SUPPORTED_FINANCE_PACKAGES.contains(packageName)) {
            val extras = sbn.notification.extras
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""

            // 💡 1단계: 스팸/오픈채팅 블랙리스트 검사 (단 하나라도 걸리면 즉시 무시)
            val fullMsg = "$title $text"
            for (spamKeyword in SPAM_BLACKLIST) {
                if (fullMsg.contains(spamKeyword, ignoreCase = true)) {
                    Log.d(TAG, "🚫 스팸/오픈채팅 키워드 감지되어 무시: '$spamKeyword' in '$fullMsg'")
                    return
                }
            }

            // 💡 2단계: 카카오톡인 경우 발신자가 금융/공식 채널이거나 정식 결제 포맷인지 확인
            if (packageName == "com.kakao.talk") {
                val isFinanceSender = title.contains("카카오페이") ||
                        title.contains("카카오뱅크") ||
                        title.contains("카드") ||
                        title.contains("은행") ||
                        title.contains("페이") ||
                        title.contains("KB") ||
                        title.contains("신한") ||
                        title.contains("토스")

                // 공식 금융 발신자가 아니면 대화방에서 '입금' 언급해도 절대 입금 처리 안함 (결제/승인만 엄격히 확인)
                val isLegitPayment = text.contains("결제완료") || text.contains("승인") || text.contains("체크카드")
                if (!isFinanceSender && !isLegitPayment) {
                    Log.d(TAG, "🚫 일반 카톡방 대화는 결제/입금 처리 제외: $title")
                    return
                }
            }

            // 💡 3단계: 유효한 금융 결제/출금/공식입금 키워드 확인
            val isPayment = text.contains("결제") || text.contains("승인") || text.contains("출금")
            val isOfficialIncome = text.contains("입금완료") || text.contains("입금되었습니다") || text.contains("타행입금") || text.contains("급여") || text.contains("송금받음")

            if ((isPayment || isOfficialIncome) && text.contains("원")) {
                processFinanceNotification(packageName, title, text, isOfficialIncome)
            }
        }
    }

    private fun processFinanceNotification(pkg: String, title: String, body: String, isIncome: Boolean) {
        try {
            val amountPattern = Pattern.compile("([0-9,]+)\\s*원")
            val amountMatcher = amountPattern.matcher(body)

            var amount = 0L
            if (amountMatcher.find()) {
                val amountStr = amountMatcher.group(1)?.replace(",", "") ?: "0"
                amount = amountStr.toLongOrNull() ?: 0L
            }

            if (amount <= 0 || amount > 100_000_000L) return

            val type = if (isIncome) "INCOME" else "EXPENSE"

            // 중복 방지
            if (isDuplicate(amount, type)) return

            var detectedCategory = if (type == "INCOME") "기타" else "기타"
            var detectedBrand = ""
            val fullText = "$title $body"

            for ((brand, category) in BRAND_CATEGORY_MAP) {
                if (fullText.contains(brand, ignoreCase = true)) {
                    detectedCategory = category
                    detectedBrand = brand
                    break
                }
            }

            val sourceName = when (pkg) {
                "com.kakao.talk" -> "카톡"
                "viva.republica.toss" -> "토스"
                "com.kakaobank.channel" -> "카뱅"
                "com.kbstar.kbbank", "com.kbcard.cxh.appcard" -> "KB"
                "com.shinhan.sbanking", "com.shcard.smartpay" -> "신한"
                "com.samsung.android.spay" -> "삼페"
                "com.hyundaicard.appcard" -> "현대"
                else -> "금융앱"
            }

            val memo = if (detectedBrand.isNotEmpty()) {
                "$detectedBrand ($sourceName)"
            } else if (title.isNotEmpty()) {
                "$title ($sourceName)"
            } else {
                if (type == "INCOME") "입금 ($sourceName)" else "결제 ($sourceName)"
            }

            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val transaction = Transaction(
                type = type,
                amount = amount,
                category = detectedCategory,
                memo = memo,
                date = currentDate
            )

            val db = AppDatabase.getDatabase(applicationContext)
            thread {
                db.transactionDao().insertTransaction(transaction)
                Log.d(TAG, "정상 금융 거래 등록 완료: $transaction")

                val intent = Intent(ACTION_DATA_UPDATED).apply {
                    setPackage(packageName)
                }
                sendBroadcast(intent)
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                val typeStr = if (type == "INCOME") "입금" else "결제"
                Toast.makeText(applicationContext, "💳 [$detectedCategory] $memo ${amount}원 자동 등록!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Log.e(TAG, "파싱 에러: ${e.message}")
        }
    }
}