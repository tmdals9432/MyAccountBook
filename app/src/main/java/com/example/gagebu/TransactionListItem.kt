package com.example.gagebu

sealed class TransactionListItem {

    // 💡 요일(dayOfWeek)과 접힘 상태(isCollapsed)가 포함된 최신 DailyHeader
    data class DailyHeader(
        val date: String,
        val dayOfWeek: String = "",
        val income: Long = 0L,
        val expense: Long = 0L,
        var isCollapsed: Boolean = false
    ) : TransactionListItem()

    data class TransactionItem(
        val transaction: Transaction
    ) : TransactionListItem()

    data class MonthRow(
        val month: Int,
        val monthName: String,
        val periodStr: String,
        val income: Long,
        val expense: Long,
        var isExpanded: Boolean = false
    ) : TransactionListItem()

    data class WeekRow(
        val periodStr: String,
        val income: Long,
        val expense: Long
    ) : TransactionListItem()
}