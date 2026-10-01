package com.example.gagebu

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import java.text.DecimalFormat

class NumberTextWatcher(private val editText: EditText) : TextWatcher {

    private val df = DecimalFormat("#,###")
    private var isFormatting = false

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

    override fun afterTextChanged(s: Editable?) {
        if (isFormatting) return

        val originalStr = s.toString().replace(",", "").trim()
        if (originalStr.isEmpty()) return

        try {
            isFormatting = true
            val parsed = originalStr.toLong()
            val formatted = df.format(parsed)

            editText.setText(formatted)
            editText.setSelection(formatted.length) // 항상 커서를 맨 뒤로 이동
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isFormatting = false
        }
    }
}