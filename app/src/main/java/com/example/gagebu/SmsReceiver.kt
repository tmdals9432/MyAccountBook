package com.example.gagebu

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.concurrent.thread

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val body = sms.displayMessageBody ?: continue

                if (body.contains("승인") || body.contains("결제") || body.contains("출금") || body.contains("입금")) {
                    parseAndSaveSms(context, body)
                }
            }
        }
    }

    private fun parseAndSaveSms(context: Context, body: String) {
        try {
            val amountPattern = Pattern.compile("([0-9,]+)\\s*원")
            val amountMatcher = amountPattern.matcher(body)

            var amount = 0L
            if (amountMatcher.find()) {
                val amountStr = amountMatcher.group(1)?.replace(",", "") ?: "0"
                amount = amountStr.toLongOrNull() ?: 0L
            }

            if (amount <= 0) return

            val type = if (body.contains("입금")) "INCOME" else "EXPENSE"

            // 💡 금융 알림 리스너의 중복 검사 로직 공유 (문자-앱푸시 동시 도착 방어)
            if (FinancialNotificationListener.isDuplicate(amount, type)) return

            var detectedCategory = "기타"
            var detectedBrand = ""

            for ((brand, category) in FinancialNotificationListener.BRAND_CATEGORY_MAP) {
                if (body.contains(brand, ignoreCase = true)) {
                    detectedCategory = category
                    detectedBrand = brand
                    break
                }
            }

            val memo = if (detectedBrand.isNotEmpty()) "$detectedBrand (SMS)" else "카드 결제"
            val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            val transaction = Transaction(
                type = type,
                amount = amount,
                category = detectedCategory,
                memo = memo,
                date = currentDate
            )

            val db = AppDatabase.getDatabase(context)
            thread {
                db.transactionDao().insertTransaction(transaction)

                val updateIntent = Intent(FinancialNotificationListener.ACTION_DATA_UPDATED).apply {
                    setPackage(context.packageName)
                }
                context.sendBroadcast(updateIntent)
            }

            android.os.Handler(android.os.Looper.getMainLooper()).post {
                Toast.makeText(context, "💳 [$detectedCategory] $memo ${amount}원 자동 등록!", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}