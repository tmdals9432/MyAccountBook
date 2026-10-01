package com.example.gagebu

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class TransactionAdapter(
    private var list: List<TransactionListItem>,
    private val onItemLongClick: (Transaction) -> Unit,
    private val onMonthRowClick: (Int) -> Unit = {},
    private val onHeaderClick: (String) -> Unit = {},
    private val onHeaderLongClick: (String) -> Unit = {}
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_ITEM = 1
        const val TYPE_MONTH_ROW = 2
        const val TYPE_WEEK_ROW = 3
    }

    var fontSizeMode: Int = 1

    class HeaderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvDailyDate: TextView = itemView.findViewById(R.id.tvDailyDate)
        val tvFoldIndicator: TextView = itemView.findViewById(R.id.tvFoldIndicator)
        val tvDailyIncome: TextView = itemView.findViewById(R.id.tvDailyIncome)
        val tvDailyExpense: TextView = itemView.findViewById(R.id.tvDailyExpense)
    }

    class ItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvItemCategory: TextView = itemView.findViewById(R.id.tvItemCategory)
        val tvItemMemo: TextView = itemView.findViewById(R.id.tvItemMemo)
        val tvItemAmount: TextView = itemView.findViewById(R.id.tvItemAmount)
    }

    class MonthRowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMonthName: TextView = itemView.findViewById(R.id.tvMonthName)
        val tvMonthPeriod: TextView = itemView.findViewById(R.id.tvMonthPeriod)
        val tvMonthIncome: TextView = itemView.findViewById(R.id.tvMonthIncome)
        val tvMonthExpense: TextView = itemView.findViewById(R.id.tvMonthExpense)
        val tvMonthSum: TextView = itemView.findViewById(R.id.tvMonthSum)
    }

    class WeekRowViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvWeekPeriod: TextView = itemView.findViewById(R.id.tvWeekPeriod)
        val tvWeekIncome: TextView = itemView.findViewById(R.id.tvWeekIncome)
        val tvWeekExpense: TextView = itemView.findViewById(R.id.tvWeekExpense)
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is TransactionListItem.DailyHeader -> TYPE_HEADER
            is TransactionListItem.TransactionItem -> TYPE_ITEM
            is TransactionListItem.MonthRow -> TYPE_MONTH_ROW
            is TransactionListItem.WeekRow -> TYPE_WEEK_ROW
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            TYPE_HEADER -> HeaderViewHolder(inflater.inflate(R.layout.item_daily_header, parent, false))
            TYPE_ITEM -> ItemViewHolder(inflater.inflate(R.layout.item_transaction, parent, false))
            TYPE_MONTH_ROW -> MonthRowViewHolder(inflater.inflate(R.layout.item_month_row, parent, false))
            TYPE_WEEK_ROW -> WeekRowViewHolder(inflater.inflate(R.layout.item_week_row, parent, false))
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val nf = NumberFormat.getInstance(Locale.KOREA)

        val baseSize = when (fontSizeMode) {
            0 -> 12f
            2 -> 17f
            else -> 14f
        }

        when (val item = list[position]) {
            is TransactionListItem.DailyHeader -> {
                val h = holder as HeaderViewHolder
                h.tvDailyDate.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSize)
                h.tvDailyDate.text = "${item.date} (${item.dayOfWeek})"
                h.tvFoldIndicator.text = if (item.isCollapsed) "▸" else "▾"

                if (item.income > 0) {
                    h.tvDailyIncome.visibility = View.VISIBLE
                    h.tvDailyIncome.text = "+${nf.format(item.income)}원"
                    h.tvDailyIncome.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSize - 1)
                } else {
                    h.tvDailyIncome.visibility = View.GONE
                }

                if (item.expense > 0) {
                    h.tvDailyExpense.visibility = View.VISIBLE
                    h.tvDailyExpense.text = "-${nf.format(item.expense)}원"
                    h.tvDailyExpense.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSize - 1)
                } else {
                    h.tvDailyExpense.visibility = View.GONE
                }

                h.itemView.setOnClickListener {
                    onHeaderClick(item.date)
                }

                h.itemView.setOnLongClickListener {
                    onHeaderLongClick(item.date)
                    true
                }
            }

            is TransactionListItem.TransactionItem -> {
                val h = holder as ItemViewHolder
                val t = item.transaction

                h.tvItemCategory.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSize)
                h.tvItemMemo.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSize - 2)
                h.tvItemAmount.setTextSize(TypedValue.COMPLEX_UNIT_SP, baseSize + 1)

                h.tvItemCategory.text = t.category
                h.tvItemMemo.text = t.memo
                h.tvItemMemo.visibility = if (t.memo.isNotEmpty()) View.VISIBLE else View.GONE

                if (t.type == "INCOME") {
                    h.tvItemAmount.text = "+${nf.format(t.amount)}원"
                    h.tvItemAmount.setTextColor(Color.parseColor("#38B6FF"))
                } else {
                    h.tvItemAmount.text = "-${nf.format(t.amount)}원"
                    h.tvItemAmount.setTextColor(Color.parseColor("#FF647C"))
                }

                h.itemView.setOnLongClickListener {
                    onItemLongClick(t)
                    true
                }
            }

            is TransactionListItem.MonthRow -> {
                val h = holder as MonthRowViewHolder
                h.tvMonthName.text = item.monthName
                h.tvMonthPeriod.text = item.periodStr
                h.tvMonthIncome.text = nf.format(item.income)
                h.tvMonthExpense.text = nf.format(item.expense)
                val sum = item.income - item.expense
                h.tvMonthSum.text = nf.format(sum)

                h.itemView.setOnClickListener {
                    onMonthRowClick(item.month)
                }
            }

            is TransactionListItem.WeekRow -> {
                val h = holder as WeekRowViewHolder
                h.tvWeekPeriod.text = item.periodStr
                h.tvWeekIncome.text = nf.format(item.income)
                h.tvWeekExpense.text = nf.format(item.expense)
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<TransactionListItem>) {
        list = newList
        notifyDataSetChanged()
    }
}