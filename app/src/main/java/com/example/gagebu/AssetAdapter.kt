package com.example.gagebu

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.NumberFormat
import java.util.Locale

class AssetAdapter(
    private var list: List<Asset>,
    private val onItemLongClick: (Asset) -> Unit
) : RecyclerView.Adapter<AssetAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvAssetName: TextView = itemView.findViewById(R.id.tvAssetName)
        val tvAssetGroupAndMemo: TextView = itemView.findViewById(R.id.tvAssetGroupAndMemo)
        val tvAssetAmount: TextView = itemView.findViewById(R.id.tvAssetAmount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_asset_row, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val nf = NumberFormat.getInstance(Locale.KOREA)

        holder.tvAssetName.text = item.name
        holder.tvAssetGroupAndMemo.text = if (item.memo.isEmpty()) item.group else "${item.group} | ${item.memo}"

        // 부채 성격(신용카드, 대출, 마이너스통장)은 빨간색으로 표시
        val isDebt = item.group in listOf("신용카드", "대출", "마이너스통장")
        if (isDebt) {
            holder.tvAssetAmount.text = "-${nf.format(item.amount)}원"
            holder.tvAssetAmount.setTextColor(Color.parseColor("#FF647C"))
        } else {
            holder.tvAssetAmount.text = "${nf.format(item.amount)}원"
            holder.tvAssetAmount.setTextColor(Color.parseColor("#38B6FF"))
        }

        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Asset>) {
        list = newList
        notifyDataSetChanged()
    }
}