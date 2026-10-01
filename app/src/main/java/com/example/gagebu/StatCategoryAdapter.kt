package com.example.gagebu

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

data class StatItem(
    val category: String,
    val amount: Long,
    val percentage: Float,
    val budget: Long = 0L // 카테고리별 예산
)

class StatCategoryAdapter(
    private var list: List<StatItem>,
    private val onItemClick: ((StatItem) -> Unit)? = null
) : RecyclerView.Adapter<StatCategoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStatPercent: TextView = itemView.findViewById(R.id.tvStatPercent)
        val tvStatCategory: TextView = itemView.findViewById(R.id.tvStatCategory)
        val tvStatBudgetSub: TextView = itemView.findViewById(R.id.tvStatBudgetSub)
        val tvStatAmount: TextView = itemView.findViewById(R.id.tvStatAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_stat_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val nf = NumberFormat.getInstance(Locale.KOREA)

        holder.tvStatPercent.text = String.format(Locale.getDefault(), "%.1f%%", item.percentage)
        holder.tvStatCategory.text = item.category
        holder.tvStatAmount.text = "${nf.format(item.amount)}원"

        // 예산 비교 상태 출력
        if (item.budget > 0) {
            val remain = item.budget - item.amount
            if (remain >= 0) {
                holder.tvStatBudgetSub.text = "남은 예산: ${nf.format(remain)}원 (예산: ${nf.format(item.budget)}원)"
                holder.tvStatBudgetSub.setTextColor(Color.parseColor("#4D96FF")) // 파란색 (안전)
            } else {
                holder.tvStatBudgetSub.text = "🚨 초과 지출: +${nf.format(-remain)}원 (예산: ${nf.format(item.budget)}원)"
                holder.tvStatBudgetSub.setTextColor(Color.parseColor("#FF647C")) // 빨간색 (초과)
            }
        } else {
            holder.tvStatBudgetSub.text = "터치하여 이 카테고리 예산 설정"
            holder.tvStatBudgetSub.setTextColor(Color.parseColor("#8E8E93"))
        }

        holder.itemView.setOnClickListener {
            onItemClick?.invoke(item)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<StatItem>) {
        list = newList
        notifyDataSetChanged()
    }
}