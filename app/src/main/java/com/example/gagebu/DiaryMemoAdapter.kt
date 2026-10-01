package com.example.gagebu

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DiaryMemoAdapter(
    private var list: List<DiaryMemo>,
    private val onItemClick: (DiaryMemo) -> Unit,
    private val onItemLongClick: (DiaryMemo) -> Unit
) : RecyclerView.Adapter<DiaryMemoAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMemoDate: TextView = itemView.findViewById(R.id.tvMemoDate)
        val tvMemoTitle: TextView = itemView.findViewById(R.id.tvMemoTitle)
        val tvMemoContent: TextView = itemView.findViewById(R.id.tvMemoContent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_diary_memo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvMemoDate.text = item.date
        holder.tvMemoTitle.text = item.title
        holder.tvMemoContent.text = item.content

        holder.itemView.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnLongClickListener {
            onItemLongClick(item)
            true
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<DiaryMemo>) {
        list = newList
        notifyDataSetChanged()
    }
}