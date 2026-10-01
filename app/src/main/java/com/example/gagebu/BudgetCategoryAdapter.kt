package com.example.gagebu

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

data class BudgetCategoryItem(
    val category: String,
    val budget: Long,
    val spent: Long
)

class BudgetCategoryAdapter(
    private var list: List<BudgetCategoryItem>,
    private val onItemClick: (BudgetCategoryItem) -> Unit
) : RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvBudgetCategoryTitle: TextView = itemView.findViewById(R.id.tvBudgetCategoryTitle)
        val pbCategoryProgress: ProgressBar = itemView.findViewById(R.id.pbCategoryProgress)
        val tvCategoryPercent: TextView = itemView.findViewById(R.id.tvCategoryPercent)
        val tvCategoryBudgetTotal: TextView = itemView.findViewById(R.id.tvCategoryBudgetTotal)
        val tvCategorySpent: TextView = itemView.findViewById(R.id.tvCategorySpent)
        val tvCategoryRemain: TextView = itemView.findViewById(R.id.tvCategoryRemain)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_budget_category, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val nf = NumberFormat.getInstance(Locale.KOREA)

        holder.tvBudgetCategoryTitle.text = item.category
        holder.tvCategoryBudgetTotal.text = "${nf.format(item.budget)}원"
        holder.tvCategorySpent.text = nf.format(item.spent)

        val remain = item.budget - item.spent

        // 💡 수치에 따른 정확한 퍼센트 계산
        val percent = if (item.budget > 0) {
            ((item.spent.toDouble() / item.budget.toDouble()) * 100.0).toInt()
        } else {
            0
        }

        holder.tvCategoryPercent.text = "$percent%"

        // 0%일 때는 완전히 비우고, 최대 100%까지만 채움
        val progressVal = percent.coerceIn(0, 100)
        holder.pbCategoryProgress.progress = progressVal

        if (remain >= 0) {
            // 정상 예산 범위 (파란색)
            holder.tvCategoryRemain.text = nf.format(remain)
            holder.tvCategoryRemain.setTextColor(Color.parseColor("#8E8E93"))
            holder.tvCategorySpent.setTextColor(Color.parseColor("#38B6FF"))
            holder.pbCategoryProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#38B6FF"))
        } else {
            // 예산 초과 (빨간색)
            holder.tvCategoryRemain.text = "-${nf.format(-remain)}"
            holder.tvCategoryRemain.setTextColor(Color.parseColor("#FF647C"))
            holder.tvCategorySpent.setTextColor(Color.parseColor("#FF647C"))
            holder.pbCategoryProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#FF647C"))
        }

        holder.itemView.setOnClickListener {
            onItemClick(item)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<BudgetCategoryItem>) {
        list = newList
        notifyDataSetChanged()
    }
}