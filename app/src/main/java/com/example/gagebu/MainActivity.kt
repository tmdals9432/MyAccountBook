package com.example.gagebu

import android.Manifest
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CalendarView
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.io.FileWriter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private lateinit var adapter: TransactionAdapter
    private lateinit var calendarAdapter: TransactionAdapter
    private lateinit var statAdapter: StatCategoryAdapter
    private lateinit var budgetCategoryAdapter: BudgetCategoryAdapter
    private lateinit var assetAdapter: AssetAdapter
    private lateinit var diaryMemoAdapter: DiaryMemoAdapter

    private lateinit var screenAccountBook: LinearLayout
    private lateinit var screenStats: LinearLayout
    private lateinit var screenAssets: LinearLayout
    private lateinit var screenMore: ScrollView

    private lateinit var rvTransactions: RecyclerView
    private lateinit var rvCalendarTransactions: RecyclerView
    private lateinit var rvAssetList: RecyclerView
    private lateinit var rvDiaryMemos: RecyclerView
    private lateinit var layoutCalendar: LinearLayout
    private lateinit var layoutSummaryTab: ScrollView
    private lateinit var layoutYearSummaryHeader: LinearLayout
    private lateinit var calendarView: CalendarView
    private lateinit var tvSelectedDateSummary: TextView
    private lateinit var tvCalendarSelectedDayTotal: TextView
    private lateinit var tvCurrentMonth: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fabAdd: FloatingActionButton

    private lateinit var tvYearIncome: TextView
    private lateinit var tvYearExpense: TextView
    private lateinit var tvYearTotal: TextView

    // 요약 탭 뷰
    private lateinit var tvTabSummaryIncome: TextView
    private lateinit var tvTabSummaryExpense: TextView
    private lateinit var tvTabSummaryTotal: TextView
    private lateinit var tvTabSummaryAssetSpent: TextView
    private lateinit var pbSummaryBudgetProgress: ProgressBar
    private lateinit var tvSummaryBudgetPercentage: TextView
    private lateinit var tvSummaryBudgetTotal: TextView
    private lateinit var tvSummaryBudgetSpent: TextView
    private lateinit var tvSummaryBudgetRemain: TextView
    private lateinit var cardBudgetSummaryClick: CardView

    // 통계 뷰 & 상단 수입/지출 탭
    private lateinit var btnSegmentStat: TextView
    private lateinit var btnSegmentBudget: TextView
    private lateinit var layoutStatSubScreen: LinearLayout
    private lateinit var layoutBudgetSubScreen: LinearLayout
    private lateinit var tvStatCurrentMonth: TextView
    private lateinit var layoutStatIncomeTab: LinearLayout
    private lateinit var layoutStatExpenseTab: LinearLayout
    private lateinit var tvStatIncomeTab: TextView
    private lateinit var tvStatExpenseTab: TextView
    private lateinit var indicatorStatIncome: View
    private lateinit var indicatorStatExpense: View
    private lateinit var pieChartStats: PieChart
    private lateinit var rvStatCategories: RecyclerView

    // 예산 서브 뷰
    private lateinit var tvBudgetScreenRemainBig: TextView
    private lateinit var btnOpenBudgetSettingDialog: TextView
    private lateinit var pbBudgetScreenTotalProgress: ProgressBar
    private lateinit var tvBudgetScreenTotalPercent: TextView
    private lateinit var tvBudgetScreenTotalAmount: TextView
    private lateinit var tvBudgetScreenSpentAmount: TextView
    private lateinit var tvBudgetScreenRemainAmount: TextView
    private lateinit var rvBudgetCategories: RecyclerView

    // 자산 뷰
    private lateinit var tvAssetSummaryAsset: TextView
    private lateinit var tvAssetSummaryDebt: TextView
    private lateinit var tvAssetSummaryTotal: TextView
    private lateinit var btnAssetAdd: ImageView
    private lateinit var btnAssetEdit: ImageView

    // 더보기 뷰
    private lateinit var tvCurrentThemeLabel: TextView
    private lateinit var tvCurrentFontSizeLabel: TextView

    private var allTransactions: List<Transaction> = emptyList()
    private var allAssets: List<Asset> = emptyList()
    private var allDiaryMemos: List<DiaryMemo> = emptyList()
    private val currentCalendar: Calendar = Calendar.getInstance()
    private val expandedMonths = mutableSetOf<Int>()
    private val collapsedDates = mutableSetOf<String>()
    private var lastSelectedDbDate: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    private var statViewType: String = "EXPENSE"
    private var currentFontSizeMode: Int = 1

    private val assetGroupList = arrayOf(
        "현금", "은행", "신용카드", "체크카드", "선불식카드",
        "저축", "투자", "마이너스통장", "대출", "보험", "기타"
    )

    private val dataUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == FinancialNotificationListener.ACTION_DATA_UPDATED) {
                loadData()
            }
        }
    }

    private val incomeCategories = listOf(
        CategoryItem("월급", "💰", "#38B6FF"), CategoryItem("용돈", "👛", "#95D13C"),
        CategoryItem("이월", "↪️", "#9B72CF"), CategoryItem("기타", "➕", "#38C2B3"),
        CategoryItem("자산인출", "🏧", "#FF8A5C"), CategoryItem("잔액조정", "🏦", "#44BEC7")
    )

    private val expenseCategories = listOf(
        CategoryItem("식비", "🍽️", "#44C78B"), CategoryItem("교통비", "🚌", "#FF6B6B"),
        CategoryItem("여가/취미", "🎵", "#FF6584"), CategoryItem("생활용품", "🛒", "#FFA14A"),
        CategoryItem("쇼핑", "🛍️", "#38B6FF"), CategoryItem("미용", "🧖", "#FF4F79"),
        CategoryItem("의료/건강", "💊", "#38C2B3"), CategoryItem("교육", "📚", "#8F72CF"),
        CategoryItem("통신비", "📱", "#FAD02C"), CategoryItem("주거/공과금", "🏢", "#FF8A5C"),
        CategoryItem("경조사", "🎁", "#36D1B2"), CategoryItem("저축", "🐷", "#FAD02C"),
        CategoryItem("카드대금", "💳", "#E8507E"), CategoryItem("보험", "🏠", "#D97B66"),
        CategoryItem("세금", "💵", "#44BEC7"), CategoryItem("기타", "➖", "#FF5252")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        applySavedTheme()
        loadSavedFontSize()

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val rootView = findViewById<View>(android.R.id.content)
        ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
            val statusBarHeight = insets.getInsets(WindowInsetsCompat.Type.statusBars()).top
            view.setPadding(0, statusBarHeight, 0, 0)
            insets
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 100)
        }

        db = AppDatabase.getDatabase(this)

        screenAccountBook = findViewById(R.id.screenAccountBook)
        screenStats = findViewById(R.id.screenStats)
        screenAssets = findViewById(R.id.screenAssets)
        screenMore = findViewById(R.id.screenMore)

        rvTransactions = findViewById(R.id.rvTransactions)
        rvCalendarTransactions = findViewById(R.id.rvCalendarTransactions)
        rvAssetList = findViewById(R.id.rvAssetList)
        rvDiaryMemos = findViewById(R.id.rvDiaryMemos)
        layoutCalendar = findViewById(R.id.layoutCalendar)
        layoutSummaryTab = findViewById(R.id.layoutSummaryTab)
        layoutYearSummaryHeader = findViewById(R.id.layoutYearSummaryHeader)
        calendarView = findViewById(R.id.calendarView)
        tvSelectedDateSummary = findViewById(R.id.tvSelectedDateSummary)
        tvCalendarSelectedDayTotal = findViewById(R.id.tvCalendarSelectedDayTotal)
        tvCurrentMonth = findViewById(R.id.tvCurrentMonth)
        tabLayout = findViewById(R.id.tabLayout)
        bottomNav = findViewById(R.id.bottomNav)
        fabAdd = findViewById(R.id.fabAdd)

        tvYearIncome = findViewById(R.id.tvYearIncome)
        tvYearExpense = findViewById(R.id.tvYearExpense)
        tvYearTotal = findViewById(R.id.tvYearTotal)

        tvTabSummaryIncome = findViewById(R.id.tvTabSummaryIncome)
        tvTabSummaryExpense = findViewById(R.id.tvTabSummaryExpense)
        tvTabSummaryTotal = findViewById(R.id.tvTabSummaryTotal)
        tvTabSummaryAssetSpent = findViewById(R.id.tvTabSummaryAssetSpent)
        pbSummaryBudgetProgress = findViewById(R.id.pbSummaryBudgetProgress)
        tvSummaryBudgetPercentage = findViewById(R.id.tvSummaryBudgetPercentage)
        tvSummaryBudgetTotal = findViewById(R.id.tvSummaryBudgetTotal)
        tvSummaryBudgetSpent = findViewById(R.id.tvSummaryBudgetSpent)
        tvSummaryBudgetRemain = findViewById(R.id.tvSummaryBudgetRemain)
        cardBudgetSummaryClick = findViewById(R.id.cardBudgetSummaryClick)

        btnSegmentStat = findViewById(R.id.btnSegmentStat)
        btnSegmentBudget = findViewById(R.id.btnSegmentBudget)
        layoutStatSubScreen = findViewById(R.id.layoutStatSubScreen)
        layoutBudgetSubScreen = findViewById(R.id.layoutBudgetSubScreen)
        tvStatCurrentMonth = findViewById(R.id.tvStatCurrentMonth)

        layoutStatIncomeTab = findViewById(R.id.layoutStatIncomeTab)
        layoutStatExpenseTab = findViewById(R.id.layoutStatExpenseTab)
        tvStatIncomeTab = findViewById(R.id.tvStatIncomeTab)
        tvStatExpenseTab = findViewById(R.id.tvStatExpenseTab)
        indicatorStatIncome = findViewById(R.id.indicatorStatIncome)
        indicatorStatExpense = findViewById(R.id.indicatorStatExpense)

        pieChartStats = findViewById(R.id.pieChartStats)
        rvStatCategories = findViewById(R.id.rvStatCategories)

        tvBudgetScreenRemainBig = findViewById(R.id.tvBudgetScreenRemainBig)
        btnOpenBudgetSettingDialog = findViewById(R.id.btnOpenBudgetSettingDialog)
        pbBudgetScreenTotalProgress = findViewById(R.id.pbBudgetScreenTotalProgress)
        tvBudgetScreenTotalPercent = findViewById(R.id.tvBudgetScreenTotalPercent)
        tvBudgetScreenTotalAmount = findViewById(R.id.tvBudgetScreenTotalAmount)
        tvBudgetScreenSpentAmount = findViewById(R.id.tvBudgetScreenSpentAmount)
        tvBudgetScreenRemainAmount = findViewById(R.id.tvBudgetScreenRemainAmount)
        rvBudgetCategories = findViewById(R.id.rvBudgetCategories)

        // 자산 뷰
        tvAssetSummaryAsset = findViewById(R.id.tvAssetSummaryAsset)
        tvAssetSummaryDebt = findViewById(R.id.tvAssetSummaryDebt)
        tvAssetSummaryTotal = findViewById(R.id.tvAssetSummaryTotal)
        btnAssetAdd = findViewById(R.id.btnAssetAdd)
        btnAssetEdit = findViewById(R.id.btnAssetEdit)

        // 더보기 뷰
        tvCurrentThemeLabel = findViewById(R.id.tvCurrentThemeLabel)
        tvCurrentFontSizeLabel = findViewById(R.id.tvCurrentFontSizeLabel)
        val btnMoreNotice = findViewById<LinearLayout>(R.id.btnMoreNotice)
        val btnMoreTheme = findViewById<LinearLayout>(R.id.btnMoreTheme)
        val btnMoreFontSize = findViewById<LinearLayout>(R.id.btnMoreFontSize)
        val btnMoreSmsPermission = findViewById<LinearLayout>(R.id.btnMoreSmsPermission)
        val btnMoreKakaoPermission = findViewById<LinearLayout>(R.id.btnMoreKakaoPermission)
        val btnMoreExportCsv = findViewById<LinearLayout>(R.id.btnMoreExportCsv)
        val btnMoreClearData = findViewById<LinearLayout>(R.id.btnMoreClearData)
        val btnMoreContact = findViewById<LinearLayout>(R.id.btnMoreContact)

        val btnSearch = findViewById<ImageView>(R.id.btnSearch)
        val btnPrevMonth = findViewById<TextView>(R.id.btnPrevMonth)
        val btnNextMonth = findViewById<TextView>(R.id.btnNextMonth)
        val btnStatPrevMonth = findViewById<TextView>(R.id.btnStatPrevMonth)
        val btnStatNextMonth = findViewById<TextView>(R.id.btnStatNextMonth)
        val btnExportExcel = findViewById<Button>(R.id.btnExportExcel)

        adapter = TransactionAdapter(
            list = emptyList(),
            onItemLongClick = { transaction -> showDeleteDialog(transaction) },
            onMonthRowClick = { month ->
                if (expandedMonths.contains(month)) expandedMonths.remove(month)
                else expandedMonths.add(month)
                displayYearlyMonthList()
            },
            onHeaderClick = { date ->
                lastSelectedDbDate = date
                showTwoStepAddDialog()
            },
            onHeaderLongClick = { date ->
                if (collapsedDates.contains(date)) collapsedDates.remove(date)
                else collapsedDates.add(date)
                filterAndDisplayDailyData()
            }
        )
        adapter.fontSizeMode = currentFontSizeMode
        rvTransactions.layoutManager = LinearLayoutManager(this)
        rvTransactions.adapter = adapter

        calendarAdapter = TransactionAdapter(
            list = emptyList(),
            onItemLongClick = { transaction -> showDeleteDialog(transaction) }
        )
        calendarAdapter.fontSizeMode = currentFontSizeMode
        rvCalendarTransactions.layoutManager = LinearLayoutManager(this)
        rvCalendarTransactions.adapter = calendarAdapter

        diaryMemoAdapter = DiaryMemoAdapter(
            list = emptyList(),
            onItemClick = { memo -> showAddDiaryMemoDialog(memo) },
            onItemLongClick = { memo -> showDeleteDiaryMemoDialog(memo) }
        )
        rvDiaryMemos.layoutManager = LinearLayoutManager(this)
        rvDiaryMemos.adapter = diaryMemoAdapter

        statAdapter = StatCategoryAdapter(emptyList())
        rvStatCategories.layoutManager = LinearLayoutManager(this)
        rvStatCategories.adapter = statAdapter

        budgetCategoryAdapter = BudgetCategoryAdapter(emptyList()) { item ->
            showCategoryBudgetDialog(item.category)
        }
        rvBudgetCategories.layoutManager = LinearLayoutManager(this)
        rvBudgetCategories.adapter = budgetCategoryAdapter

        assetAdapter = AssetAdapter(emptyList()) { asset ->
            showAssetOptionsDialog(asset)
        }
        rvAssetList.layoutManager = LinearLayoutManager(this)
        rvAssetList.adapter = assetAdapter

        updateDateHeaderDisplay()
        updateThemeLabel()
        updateFontSizeLabel()
        loadData()

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            lastSelectedDbDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
            displayCalendarDateData(lastSelectedDbDate)
        }

        btnPrevMonth.setOnClickListener {
            if (tabLayout.selectedTabPosition == 2) currentCalendar.add(Calendar.YEAR, -1)
            else currentCalendar.add(Calendar.MONTH, -1)
            updateDateHeaderDisplay()
            updateTabContent(tabLayout.selectedTabPosition)
        }

        btnNextMonth.setOnClickListener {
            if (tabLayout.selectedTabPosition == 2) currentCalendar.add(Calendar.YEAR, 1)
            else currentCalendar.add(Calendar.MONTH, 1)
            updateDateHeaderDisplay()
            updateTabContent(tabLayout.selectedTabPosition)
        }

        btnStatPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateDateHeaderDisplay()
            refreshAllSyncedData()
        }

        btnStatNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateDateHeaderDisplay()
            refreshAllSyncedData()
        }

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                updateDateHeaderDisplay()
                updateTabContent(tab?.position ?: 0)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        cardBudgetSummaryClick.setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_chart
            switchToBudgetSubTab()
        }

        btnSegmentStat.setOnClickListener { switchToStatSubTab() }
        btnSegmentBudget.setOnClickListener { switchToBudgetSubTab() }

        layoutStatIncomeTab.setOnClickListener {
            statViewType = "INCOME"
            updateStatIncomeExpenseToggle()
            updateStatsScreenData()
        }

        layoutStatExpenseTab.setOnClickListener {
            statViewType = "EXPENSE"
            updateStatIncomeExpenseToggle()
            updateStatsScreenData()
        }

        btnOpenBudgetSettingDialog.setOnClickListener { showTotalBudgetDialog() }

        btnAssetAdd.setOnClickListener { showAddAssetDialog() }
        btnAssetEdit.setOnClickListener {
            Toast.makeText(this, "수정하거나 삭제할 자산 항목을 길게 눌러주세요.", Toast.LENGTH_SHORT).show()
        }

        btnMoreNotice.setOnClickListener { showNoticeDialog() }
        btnMoreTheme.setOnClickListener { showThemeSelectionDialog() }
        btnMoreFontSize.setOnClickListener { showFontSizeSelectionDialog() }
        btnMoreSmsPermission.setOnClickListener { checkSmsPermissionStatus() }
        btnMoreKakaoPermission.setOnClickListener { checkFinancialNotificationPermission() }
        btnMoreExportCsv.setOnClickListener { exportDataToCsv() }
        btnMoreClearData.setOnClickListener { showClearAllDataDialog() }
        btnMoreContact.setOnClickListener { sendFeedbackEmail() }

        fabAdd.setOnClickListener {
            if (tabLayout.selectedTabPosition == 4) {
                showAddDiaryMemoDialog()
            } else {
                showTwoStepAddDialog()
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_account -> {
                    screenAccountBook.visibility = View.VISIBLE
                    screenStats.visibility = View.GONE
                    screenAssets.visibility = View.GONE
                    screenMore.visibility = View.GONE

                    updateTabContent(tabLayout.selectedTabPosition)
                    val currentSubTab = tabLayout.selectedTabPosition
                    if (currentSubTab == 0 || currentSubTab == 2 || currentSubTab == 4) {
                        fabAdd.show()
                    } else {
                        fabAdd.hide()
                    }
                    true
                }
                R.id.nav_chart -> {
                    screenAccountBook.visibility = View.GONE
                    screenStats.visibility = View.VISIBLE
                    screenAssets.visibility = View.GONE
                    screenMore.visibility = View.GONE
                    fabAdd.hide()
                    updateStatsScreenData()
                    updateBudgetScreenData()
                    true
                }
                R.id.nav_asset -> {
                    screenAccountBook.visibility = View.GONE
                    screenStats.visibility = View.GONE
                    screenAssets.visibility = View.VISIBLE
                    screenMore.visibility = View.GONE
                    fabAdd.hide()
                    updateAssetsScreenData()
                    true
                }
                R.id.nav_more -> {
                    screenAccountBook.visibility = View.GONE
                    screenStats.visibility = View.GONE
                    screenAssets.visibility = View.GONE
                    screenMore.visibility = View.VISIBLE
                    fabAdd.hide()
                    true
                }
                else -> false
            }
        }

        btnExportExcel.setOnClickListener { exportDataToCsv() }
        btnSearch.setOnClickListener { showSearchDialog() }
    }

    override fun onResume() {
        super.onResume()
        val filter = IntentFilter(FinancialNotificationListener.ACTION_DATA_UPDATED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(dataUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(dataUpdateReceiver, filter)
        }
        loadData()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(dataUpdateReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateFabVisibility(tabPosition: Int) {
        if (bottomNav.selectedItemId == R.id.nav_account) {
            when (tabPosition) {
                0, 2, 4 -> fabAdd.show()
                else -> fabAdd.hide()
            }
        } else {
            fabAdd.hide()
        }
    }

    private fun showTwoStepAddDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_transaction, null)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val layoutStep1 = dialogView.findViewById<LinearLayout>(R.id.layoutStep1)
        val layoutStep2 = dialogView.findViewById<LinearLayout>(R.id.layoutStep2)

        val tvDateStep1 = dialogView.findViewById<TextView>(R.id.tvDateStep1)
        val btnCloseStep1 = dialogView.findViewById<ImageView>(R.id.btnCloseStep1)
        val etAmountInput = dialogView.findViewById<EditText>(R.id.etAmountInput)
        val btnIncomeType = dialogView.findViewById<Button>(R.id.btnIncomeType)
        val btnExpenseType = dialogView.findViewById<Button>(R.id.btnExpenseType)

        val btnBackStep2 = dialogView.findViewById<TextView>(R.id.btnBackStep2)
        val tvAmountStep2 = dialogView.findViewById<TextView>(R.id.tvAmountStep2)
        val btnCloseStep2 = dialogView.findViewById<ImageView>(R.id.btnCloseStep2)
        val rvCategories = dialogView.findViewById<RecyclerView>(R.id.rvCategories)
        val etMemoStep2 = dialogView.findViewById<EditText>(R.id.etMemoStep2)

        etAmountInput.addTextChangedListener(NumberTextWatcher(etAmountInput))

        tvDateStep1.text = lastSelectedDbDate.replace("-", "/")

        tvDateStep1.setOnClickListener {
            val parts = lastSelectedDbDate.split("-")
            val y = parts[0].toInt()
            val m = parts[1].toInt() - 1
            val d = parts[2].toInt()

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    lastSelectedDbDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    tvDateStep1.text = lastSelectedDbDate.replace("-", "/")
                },
                y, m, d
            ).show()
        }

        btnCloseStep1.setOnClickListener { dialog.dismiss() }
        btnCloseStep2.setOnClickListener { dialog.dismiss() }

        var currentType = "EXPENSE"
        var currentAmount = 0L
        val numberFormat = NumberFormat.getInstance(Locale.KOREA)

        fun moveToStep2(type: String) {
            val amountStr = etAmountInput.text.toString().replace(",", "").trim()
            if (amountStr.isEmpty() || amountStr.toLongOrNull() == null || amountStr.toLong() <= 0) {
                Toast.makeText(this, "금액을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return
            }
            currentType = type
            currentAmount = amountStr.toLong()

            val prefix = if (currentType == "INCOME") "+ " else "- "
            tvAmountStep2.text = "$prefix${numberFormat.format(currentAmount)}원"
            if (currentType == "INCOME") {
                tvAmountStep2.setTextColor(Color.parseColor("#38B6FF"))
            } else {
                tvAmountStep2.setTextColor(Color.parseColor("#FF647C"))
            }

            val categories = if (currentType == "INCOME") incomeCategories else expenseCategories
            rvCategories.layoutManager = GridLayoutManager(this, 5)
            rvCategories.adapter = CategoryAdapter(categories) { categoryItem ->
                val memo = etMemoStep2.text.toString().trim()
                val transaction = Transaction(
                    type = currentType,
                    amount = currentAmount,
                    category = categoryItem.name,
                    memo = memo,
                    date = lastSelectedDbDate
                )
                thread {
                    db.transactionDao().insertTransaction(transaction)
                    runOnUiThread {
                        Toast.makeText(this, "${categoryItem.name} 저장 완료!", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadData()
                    }
                }
            }

            layoutStep1.visibility = View.GONE
            layoutStep2.visibility = View.VISIBLE
        }

        btnIncomeType.setOnClickListener { moveToStep2("INCOME") }
        btnExpenseType.setOnClickListener { moveToStep2("EXPENSE") }

        btnBackStep2.setOnClickListener {
            layoutStep2.visibility = View.GONE
            layoutStep1.visibility = View.VISIBLE
        }

        dialog.show()
    }

    private fun showAddAssetDialog(assetToEdit: Asset? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_asset, null, false)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnBack = dialogView.findViewById<TextView>(R.id.btnAssetDialogBack)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvAssetDialogTitle)
        val spGroup = dialogView.findViewById<Spinner>(R.id.spAssetGroup)
        val etName = dialogView.findViewById<EditText>(R.id.etAssetName)
        val etAmount = dialogView.findViewById<EditText>(R.id.etAssetAmount)
        val etMemo = dialogView.findViewById<EditText>(R.id.etAssetMemo)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveAsset)

        etAmount.addTextChangedListener(NumberTextWatcher(etAmount))

        tvTitle.text = if (assetToEdit == null) "추가" else "수정"

        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, assetGroupList)
        spGroup.adapter = spinnerAdapter

        if (assetToEdit != null) {
            val groupIndex = assetGroupList.indexOf(assetToEdit.group)
            if (groupIndex >= 0) spGroup.setSelection(groupIndex)
            etName.setText(assetToEdit.name)
            etAmount.setText(NumberFormat.getInstance(Locale.KOREA).format(assetToEdit.amount))
            etMemo.setText(assetToEdit.memo)
        }

        btnBack.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val group = spGroup.selectedItem?.toString() ?: "현금"
            val name = etName.text.toString().trim()
            val amountStr = etAmount.text.toString().replace(",", "").trim()
            val memo = etMemo.text.toString().trim()

            if (name.isEmpty()) {
                Toast.makeText(this, "자산 이름을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val amount = amountStr.toLongOrNull() ?: 0L
            val newAsset = Asset(
                id = assetToEdit?.id ?: 0,
                group = group,
                name = name,
                amount = amount,
                memo = memo
            )

            thread {
                if (assetToEdit == null) db.assetDao().insertAsset(newAsset)
                else db.assetDao().updateAsset(newAsset)

                runOnUiThread {
                    Toast.makeText(this, "자산이 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadData()
                }
            }
        }

        dialog.show()
    }

    private fun showTotalBudgetDialog() {
        val sharedPref = getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        val currentBudget = sharedPref.getLong("total_monthly_budget", 0L)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_budget, null, false)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnBack = dialogView.findViewById<TextView>(R.id.btnBudgetDialogBack)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvBudgetDialogTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etBudgetAmount)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveBudget)

        etAmount.addTextChangedListener(NumberTextWatcher(etAmount))

        tvTitle.text = "전체 예산 설정"
        if (currentBudget > 0) {
            etAmount.setText(NumberFormat.getInstance(Locale.KOREA).format(currentBudget))
        }

        btnBack.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newBudget = etAmount.text.toString().replace(",", "").trim().toLongOrNull() ?: 0L
            sharedPref.edit().putLong("total_monthly_budget", newBudget).apply()
            Toast.makeText(this, "전체 예산이 설정되었습니다.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            refreshAllSyncedData()
        }
        dialog.show()
    }

    private fun showCategoryBudgetDialog(categoryWithIcon: String) {
        val categoryName = categoryWithIcon.split(" ").last()
        val sharedPref = getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        val key = "budget_cat_$categoryName"
        val currentBudget = sharedPref.getLong(key, 0L)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_set_budget, null, false)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnBack = dialogView.findViewById<TextView>(R.id.btnBudgetDialogBack)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvBudgetDialogTitle)
        val etAmount = dialogView.findViewById<EditText>(R.id.etBudgetAmount)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveBudget)

        etAmount.addTextChangedListener(NumberTextWatcher(etAmount))

        tvTitle.text = "'$categoryName' 예산 설정"
        if (currentBudget > 0) {
            etAmount.setText(NumberFormat.getInstance(Locale.KOREA).format(currentBudget))
        }

        btnBack.setOnClickListener { dialog.dismiss() }
        btnSave.setOnClickListener {
            val newBudget = etAmount.text.toString().replace(",", "").trim().toLongOrNull() ?: 0L
            sharedPref.edit().putLong(key, newBudget).apply()
            Toast.makeText(this, "'$categoryName' 예산이 설정되었습니다.", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            refreshAllSyncedData()
        }
        dialog.show()
    }

    private fun showAddDiaryMemoDialog(memoToEdit: DiaryMemo? = null) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_diary_memo, null, false)
        val dialog = AlertDialog.Builder(this).setView(dialogView).create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        val btnBack = dialogView.findViewById<TextView>(R.id.btnDiaryDialogBack)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDiaryDialogTitle)
        val tvDatePick = dialogView.findViewById<TextView>(R.id.tvDiaryDatePick)
        val etTitle = dialogView.findViewById<EditText>(R.id.etDiaryTitle)
        val etContent = dialogView.findViewById<EditText>(R.id.etDiaryContent)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSaveDiary)

        tvTitle.text = if (memoToEdit == null) "메모 작성" else "메모 수정"

        var selectedDate = memoToEdit?.date ?: lastSelectedDbDate
        tvDatePick.text = "$selectedDate ▾"

        tvDatePick.setOnClickListener {
            val parts = selectedDate.split("-")
            val y = parts[0].toInt()
            val m = parts[1].toInt() - 1
            val d = parts[2].toInt()

            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                    tvDatePick.text = "$selectedDate ▾"
                },
                y, m, d
            ).show()
        }

        if (memoToEdit != null) {
            etTitle.setText(memoToEdit.title)
            etContent.setText(memoToEdit.content)
        }

        btnBack.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val content = etContent.text.toString().trim()

            if (title.isEmpty() && content.isEmpty()) {
                Toast.makeText(this, "제목 또는 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newMemo = DiaryMemo(
                id = memoToEdit?.id ?: 0,
                date = selectedDate,
                title = if (title.isNotEmpty()) title else "제목 없음",
                content = content
            )

            thread {
                if (memoToEdit == null) db.diaryMemoDao().insertMemo(newMemo)
                else db.diaryMemoDao().updateMemo(newMemo)

                runOnUiThread {
                    Toast.makeText(this, "메모가 저장되었습니다.", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                    loadData()
                }
            }
        }

        dialog.show()
    }

    private fun showDeleteDiaryMemoDialog(memo: DiaryMemo) {
        AlertDialog.Builder(this)
            .setTitle("메모 삭제")
            .setMessage("'${memo.title}' 메모를 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                thread {
                    db.diaryMemoDao().deleteMemo(memo)
                    runOnUiThread {
                        Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun displayCalendarDateData(targetDate: String) {
        val filtered = allTransactions.filter { it.date == targetDate }
        val nf = NumberFormat.getInstance(Locale.KOREA)
        val dayOfWeek = getDayOfWeek(targetDate)

        tvSelectedDateSummary.text = "$targetDate ($dayOfWeek)"

        var income = 0L
        var expense = 0L
        val displayList = mutableListOf<TransactionListItem>()

        for (item in filtered) {
            if (item.type == "INCOME") income += item.amount
            else if (item.type == "EXPENSE") expense += item.amount
            displayList.add(TransactionListItem.TransactionItem(item))
        }

        if (expense > 0 && income > 0) {
            tvCalendarSelectedDayTotal.text = "+${nf.format(income)} / -${nf.format(expense)}원"
            tvCalendarSelectedDayTotal.setTextColor(Color.parseColor("#38B6FF"))
        } else if (expense > 0) {
            tvCalendarSelectedDayTotal.text = "지출 ${nf.format(expense)}원"
            tvCalendarSelectedDayTotal.setTextColor(Color.parseColor("#FF647C"))
        } else if (income > 0) {
            tvCalendarSelectedDayTotal.text = "수입 ${nf.format(income)}원"
            tvCalendarSelectedDayTotal.setTextColor(Color.parseColor("#38B6FF"))
        } else {
            tvCalendarSelectedDayTotal.text = "내역 없음"
            tvCalendarSelectedDayTotal.setTextColor(Color.parseColor("#8E8E93"))
        }

        calendarAdapter.updateData(displayList)
    }

    private fun getDayOfWeek(dateStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val date = sdf.parse(dateStr) ?: return ""
            val cal = Calendar.getInstance().apply { time = date }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.SUNDAY -> "일"
                Calendar.MONDAY -> "월"
                Calendar.TUESDAY -> "화"
                Calendar.WEDNESDAY -> "수"
                Calendar.THURSDAY -> "목"
                Calendar.FRIDAY -> "금"
                Calendar.SATURDAY -> "토"
                else -> ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun filterAndDisplayDailyData() {
        val monthlyList = getCurrentMonthTransactions()
        val displayList = mutableListOf<TransactionListItem>()
        val groupedMap = monthlyList.groupBy { it.date }

        for ((date, items) in groupedMap) {
            var dayIncome = 0L
            var dayExpense = 0L
            for (item in items) {
                if (item.type == "INCOME") dayIncome += item.amount
                else if (item.type == "EXPENSE") dayExpense += item.amount
            }

            val isCollapsed = collapsedDates.contains(date)
            val dayOfWeek = getDayOfWeek(date)

            displayList.add(
                TransactionListItem.DailyHeader(
                    date = date,
                    dayOfWeek = dayOfWeek,
                    income = dayIncome,
                    expense = dayExpense,
                    isCollapsed = isCollapsed
                )
            )

            if (!isCollapsed) {
                for (item in items) {
                    displayList.add(TransactionListItem.TransactionItem(item))
                }
            }
        }
        adapter.updateData(displayList)
    }

    private fun loadSavedFontSize() {
        val sharedPref = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        currentFontSizeMode = sharedPref.getInt("font_size_mode", 1)
    }

    private fun updateFontSizeLabel() {
        tvCurrentFontSizeLabel.text = when (currentFontSizeMode) {
            0 -> "작게 〉"
            2 -> "크게 〉"
            else -> "보통 〉"
        }
    }

    private fun showFontSizeSelectionDialog() {
        val fontSizes = arrayOf("작게 (더 많은 내역 한눈에)", "보통 (기본값)", "크게 (시원한 큰 글자)")
        AlertDialog.Builder(this)
            .setTitle("글자/항목 크기 설정")
            .setSingleChoiceItems(fontSizes, currentFontSizeMode) { dialog, which ->
                currentFontSizeMode = which
                getSharedPreferences("app_settings", Context.MODE_PRIVATE)
                    .edit().putInt("font_size_mode", currentFontSizeMode).apply()

                adapter.fontSizeMode = currentFontSizeMode
                calendarAdapter.fontSizeMode = currentFontSizeMode
                updateFontSizeLabel()
                adapter.notifyDataSetChanged()
                calendarAdapter.notifyDataSetChanged()
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun switchToStatSubTab() {
        btnSegmentStat.setBackgroundResource(R.drawable.bg_segment_box)
        btnSegmentStat.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.card_bg, theme))
        btnSegmentStat.setTextColor(resources.getColor(R.color.text_primary, theme))

        btnSegmentBudget.background = null
        btnSegmentBudget.setTextColor(resources.getColor(R.color.text_secondary, theme))

        layoutStatSubScreen.visibility = View.VISIBLE
        layoutBudgetSubScreen.visibility = View.GONE
        updateStatsScreenData()
    }

    private fun switchToBudgetSubTab() {
        btnSegmentBudget.setBackgroundResource(R.drawable.bg_segment_box)
        btnSegmentBudget.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.card_bg, theme))
        btnSegmentBudget.setTextColor(resources.getColor(R.color.text_primary, theme))

        btnSegmentStat.background = null
        btnSegmentStat.setTextColor(resources.getColor(R.color.text_secondary, theme))

        layoutStatSubScreen.visibility = View.GONE
        layoutBudgetSubScreen.visibility = View.VISIBLE
        updateBudgetScreenData()
    }

    private fun updateStatIncomeExpenseToggle() {
        val monthlyList = getCurrentMonthTransactions()
        var income = 0L
        var expense = 0L
        for (item in monthlyList) {
            if (item.type == "INCOME") income += item.amount
            else if (item.type == "EXPENSE") expense += item.amount
        }
        val nf = NumberFormat.getInstance(Locale.KOREA)

        tvStatIncomeTab.text = "수입 +${nf.format(income)}원"
        tvStatExpenseTab.text = "지출 -${nf.format(expense)}원"

        if (statViewType == "INCOME") {
            tvStatIncomeTab.setTextColor(Color.parseColor("#38B6FF"))
            tvStatIncomeTab.paint.isFakeBoldText = true
            indicatorStatIncome.visibility = View.VISIBLE

            tvStatExpenseTab.setTextColor(Color.parseColor("#8E8E93"))
            tvStatExpenseTab.paint.isFakeBoldText = false
            indicatorStatExpense.visibility = View.INVISIBLE
        } else {
            tvStatExpenseTab.setTextColor(Color.parseColor("#FF647C"))
            tvStatExpenseTab.paint.isFakeBoldText = true
            indicatorStatExpense.visibility = View.VISIBLE

            tvStatIncomeTab.setTextColor(Color.parseColor("#8E8E93"))
            tvStatIncomeTab.paint.isFakeBoldText = false
            indicatorStatIncome.visibility = View.INVISIBLE
        }
    }

    private fun showAssetOptionsDialog(asset: Asset) {
        val options = arrayOf("✏️ 자산 정보 수정", "🗑️ 자산 삭제")
        AlertDialog.Builder(this)
            .setTitle("${asset.name} 관리")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showAddAssetDialog(asset)
                    1 -> {
                        AlertDialog.Builder(this)
                            .setTitle("자산 삭제")
                            .setMessage("'${asset.name}' 자산을 삭제하시겠습니까?")
                            .setPositiveButton("삭제") { _, _ ->
                                thread {
                                    db.assetDao().deleteAsset(asset)
                                    runOnUiThread {
                                        Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                                        loadData()
                                    }
                                }
                            }
                            .setNegativeButton("취소", null)
                            .show()
                    }
                }
            }
            .show()
    }

    private fun updateAssetsScreenData() {
        var totalAsset = 0L
        var totalDebt = 0L

        for (asset in allAssets) {
            val isDebt = asset.group in listOf("신용카드", "대출", "마이너스통장")
            if (isDebt) totalDebt += asset.amount
            else totalAsset += asset.amount
        }

        val totalNet = totalAsset - totalDebt
        val nf = NumberFormat.getInstance(Locale.KOREA)

        tvAssetSummaryAsset.text = nf.format(totalAsset)
        tvAssetSummaryDebt.text = if (totalDebt > 0) "-${nf.format(totalDebt)}" else "0"
        tvAssetSummaryTotal.text = nf.format(totalNet)

        assetAdapter.updateData(allAssets)
    }

    private fun updateSummaryTabData() {
        val monthlyList = getCurrentMonthTransactions()
        var income = 0L
        var expense = 0L
        for (item in monthlyList) {
            if (item.type == "INCOME") income += item.amount
            else if (item.type == "EXPENSE") expense += item.amount
        }
        val total = income - expense
        val nf = NumberFormat.getInstance(Locale.KOREA)

        tvTabSummaryIncome.text = nf.format(income)
        tvTabSummaryExpense.text = nf.format(expense)
        tvTabSummaryTotal.text = nf.format(total)
        tvTabSummaryAssetSpent.text = nf.format(expense)

        val sharedPref = getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        val totalBudget = sharedPref.getLong("total_monthly_budget", 0L)

        val remain = totalBudget - expense
        val percent = if (totalBudget > 0) {
            ((expense.toDouble() / totalBudget.toDouble()) * 100.0).toInt()
        } else {
            0
        }

        tvSummaryBudgetTotal.text = "${nf.format(totalBudget)}원"
        tvSummaryBudgetSpent.text = nf.format(expense)
        tvSummaryBudgetRemain.text = nf.format(remain)
        tvSummaryBudgetPercentage.text = "$percent%"

        val progressVal = percent.coerceIn(0, 100)
        pbSummaryBudgetProgress.progress = progressVal

        if (remain >= 0) {
            tvSummaryBudgetRemain.setTextColor(Color.parseColor("#333333"))
            pbSummaryBudgetProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#38B6FF"))
        } else {
            tvSummaryBudgetRemain.setTextColor(Color.parseColor("#FF647C"))
            pbSummaryBudgetProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#FF647C"))
        }
    }

    private fun updateBudgetScreenData() {
        val monthlyList = getCurrentMonthTransactions()
        var totalExpense = 0L
        val categoryExpenseMap = mutableMapOf<String, Long>()

        for (item in monthlyList) {
            if (item.type == "EXPENSE") {
                totalExpense += item.amount
                categoryExpenseMap[item.category] = (categoryExpenseMap[item.category] ?: 0L) + item.amount
            }
        }

        val sharedPref = getSharedPreferences("budget_prefs", Context.MODE_PRIVATE)
        val totalBudget = sharedPref.getLong("total_monthly_budget", 0L)
        val remain = totalBudget - totalExpense
        val percent = if (totalBudget > 0) {
            ((totalExpense.toDouble() / totalBudget.toDouble()) * 100.0).toInt()
        } else {
            0
        }
        val nf = NumberFormat.getInstance(Locale.KOREA)

        tvBudgetScreenRemainBig.text = "${nf.format(remain)}원"
        tvBudgetScreenTotalAmount.text = "${nf.format(totalBudget)}원"
        tvBudgetScreenSpentAmount.text = nf.format(totalExpense)
        tvBudgetScreenRemainAmount.text = nf.format(remain)
        tvBudgetScreenTotalPercent.text = "$percent%"

        val progressVal = percent.coerceIn(0, 100)
        pbBudgetScreenTotalProgress.progress = progressVal

        if (remain >= 0) {
            tvBudgetScreenRemainBig.setTextColor(Color.parseColor("#333333"))
            pbBudgetScreenTotalProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#38B6FF"))
        } else {
            tvBudgetScreenRemainBig.setTextColor(Color.parseColor("#FF647C"))
            pbBudgetScreenTotalProgress.progressTintList = ColorStateList.valueOf(Color.parseColor("#FF647C"))
        }

        val budgetList = mutableListOf<BudgetCategoryItem>()
        for (cat in expenseCategories) {
            val catBudget = sharedPref.getLong("budget_cat_${cat.name}", 0L)
            val catSpent = categoryExpenseMap[cat.name] ?: 0L

            if (catBudget > 0 || catSpent > 0) {
                budgetList.add(BudgetCategoryItem("${cat.icon} ${cat.name}", catBudget, catSpent))
            }
        }
        budgetCategoryAdapter.updateData(budgetList)
    }

    private fun updateStatsScreenData() {
        val monthlyList = getCurrentMonthTransactions()
        val categoryMap = mutableMapOf<String, Long>()
        var targetTotal = 0L

        for (item in monthlyList) {
            if (item.type == statViewType) {
                categoryMap[item.category] = (categoryMap[item.category] ?: 0L) + item.amount
                targetTotal += item.amount
            }
        }

        updateStatIncomeExpenseToggle()

        val pieEntries = ArrayList<PieEntry>()
        val statList = mutableListOf<StatItem>()

        for ((cat, amount) in categoryMap) {
            val percent = if (targetTotal > 0) (amount.toFloat() / targetTotal.toFloat()) * 100f else 0f
            pieEntries.add(PieEntry(amount.toFloat(), cat))
            statList.add(StatItem(cat, amount, percent))
        }

        if (pieEntries.isNotEmpty()) {
            val dataSet = PieDataSet(pieEntries, "").apply {
                colors = ColorTemplate.MATERIAL_COLORS.toList()
                valueTextColor = Color.WHITE
                valueTextSize = 12f
            }
            pieChartStats.data = PieData(dataSet)
            pieChartStats.description.isEnabled = false
            pieChartStats.animateY(800)
            pieChartStats.invalidate()
        } else {
            pieChartStats.clear()
            pieChartStats.invalidate()
        }

        statAdapter.updateData(statList)
    }

    private fun updateDateHeaderDisplay() {
        val year = currentCalendar.get(Calendar.YEAR)
        if (tabLayout.selectedTabPosition == 2) {
            tvCurrentMonth.text = "${year}년"
        } else {
            val sdf = SimpleDateFormat("yyyy년 M월", Locale.getDefault())
            tvCurrentMonth.text = sdf.format(currentCalendar.time)
            tvStatCurrentMonth.text = sdf.format(currentCalendar.time)
        }
    }

    private fun updateTabContent(position: Int) {
        updateFabVisibility(position)

        when (position) {
            0 -> {
                layoutYearSummaryHeader.visibility = View.GONE
                rvTransactions.visibility = View.VISIBLE
                layoutCalendar.visibility = View.GONE
                layoutSummaryTab.visibility = View.GONE
                rvDiaryMemos.visibility = View.GONE
                filterAndDisplayDailyData()
            }
            1 -> {
                layoutYearSummaryHeader.visibility = View.GONE
                rvTransactions.visibility = View.GONE
                layoutCalendar.visibility = View.VISIBLE
                layoutSummaryTab.visibility = View.GONE
                rvDiaryMemos.visibility = View.GONE
                displayCalendarDateData(lastSelectedDbDate)
            }
            2 -> {
                layoutYearSummaryHeader.visibility = View.VISIBLE
                rvTransactions.visibility = View.VISIBLE
                layoutCalendar.visibility = View.GONE
                layoutSummaryTab.visibility = View.GONE
                rvDiaryMemos.visibility = View.GONE
                displayYearlyMonthList()
            }
            3 -> {
                layoutYearSummaryHeader.visibility = View.GONE
                rvTransactions.visibility = View.GONE
                layoutCalendar.visibility = View.GONE
                layoutSummaryTab.visibility = View.VISIBLE
                rvDiaryMemos.visibility = View.GONE
                updateSummaryTabData()
            }
            4 -> {
                layoutYearSummaryHeader.visibility = View.GONE
                rvTransactions.visibility = View.GONE
                layoutCalendar.visibility = View.GONE
                layoutSummaryTab.visibility = View.GONE
                rvDiaryMemos.visibility = View.VISIBLE
                diaryMemoAdapter.updateData(allDiaryMemos)
            }
        }
    }

    private fun getCurrentMonthTransactions(): List<Transaction> {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentTargetMonth = sdf.format(currentCalendar.time)
        return allTransactions.filter { it.date.startsWith(currentTargetMonth) }
    }

    private fun refreshAllSyncedData() {
        updateSummaryTabData()
        updateStatsScreenData()
        updateBudgetScreenData()
        updateAssetsScreenData()
    }

    private fun displayYearlyMonthList() {
        val year = currentCalendar.get(Calendar.YEAR)
        val displayList = mutableListOf<TransactionListItem>()
        val numberFormat = NumberFormat.getInstance(Locale.KOREA)

        var totalYearIncome = 0L
        var totalYearExpense = 0L
        val yearTransactions = allTransactions.filter { it.date.startsWith("$year-") }

        for (m in 12 downTo 1) {
            val monthPrefix = String.format(Locale.getDefault(), "%04d-%02d", year, m)
            val monthItems = yearTransactions.filter { it.date.startsWith(monthPrefix) }
            val cal = Calendar.getInstance().apply { set(year, m - 1, 1) }
            val lastDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            var monthIncome = 0L
            var monthExpense = 0L
            for (t in monthItems) {
                if (t.type == "INCOME") monthIncome += t.amount
                else if (t.type == "EXPENSE") monthExpense += t.amount
            }

            totalYearIncome += monthIncome
            totalYearExpense += monthExpense

            displayList.add(
                TransactionListItem.MonthRow(
                    month = m,
                    monthName = "${m}월",
                    periodStr = "$m. 1. ~ $m. $lastDay.",
                    income = monthIncome,
                    expense = monthExpense,
                    isExpanded = expandedMonths.contains(m)
                )
            )

            if (expandedMonths.contains(m)) {
                val weekRanges = listOf(Pair(26, lastDay), Pair(19, 25), Pair(12, 18), Pair(5, 11), Pair(1, 4))
                for (range in weekRanges) {
                    val start = range.first
                    val end = range.second
                    val weekItems = monthItems.filter {
                        val day = it.date.split("-").last().toIntOrNull() ?: 1
                        day in start..end
                    }
                    var wIncome = 0L
                    var wExpense = 0L
                    for (t in weekItems) {
                        if (t.type == "INCOME") wIncome += t.amount
                        else if (t.type == "EXPENSE") wExpense += t.amount
                    }
                    displayList.add(TransactionListItem.WeekRow("$m. $start. ~ $m. $end.", wIncome, wExpense))
                }
            }
        }

        val totalYearSum = totalYearIncome - totalYearExpense
        tvYearIncome.text = numberFormat.format(totalYearIncome)
        tvYearExpense.text = numberFormat.format(totalYearExpense)
        tvYearTotal.text = numberFormat.format(totalYearSum)

        adapter.updateData(displayList)
    }

    private fun showDeleteDialog(transaction: Transaction) {
        AlertDialog.Builder(this)
            .setTitle("내역 삭제")
            .setMessage("'${transaction.category} - ${transaction.amount}원' 내역을 삭제하시겠습니까?")
            .setPositiveButton("삭제") { _, _ ->
                thread {
                    db.transactionDao().deleteTransaction(transaction)
                    runOnUiThread {
                        Toast.makeText(this, "삭제되었습니다.", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun showSearchDialog() {
        val etSearch = EditText(this).apply {
            hint = "검색어 입력 (카테고리, 메모, 금액)"
            setPadding(50, 40, 50, 40)
        }

        AlertDialog.Builder(this)
            .setTitle("내역 검색")
            .setView(etSearch)
            .setPositiveButton("검색") { _, _ ->
                val query = etSearch.text.toString().trim()
                if (query.isEmpty()) {
                    updateTabContent(tabLayout.selectedTabPosition)
                } else {
                    val filteredList = allTransactions.filter {
                        it.category.contains(query, ignoreCase = true) ||
                                it.memo.contains(query, ignoreCase = true) ||
                                it.amount.toString().contains(query)
                    }

                    val displayList = mutableListOf<TransactionListItem>()
                    val groupedMap = filteredList.groupBy { it.date }
                    for ((date, items) in groupedMap) {
                        var dayIncome = 0L
                        var dayExpense = 0L
                        for (item in items) {
                            if (item.type == "INCOME") dayIncome += item.amount
                            else if (item.type == "EXPENSE") dayExpense += item.amount
                        }
                        val dayOfWeek = getDayOfWeek(date)
                        displayList.add(TransactionListItem.DailyHeader(date, dayOfWeek, dayIncome, dayExpense))
                        for (item in items) displayList.add(TransactionListItem.TransactionItem(item))
                    }

                    adapter.updateData(displayList)
                    Toast.makeText(this, "'$query' 검색 결과: ${filteredList.size}건", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("전체 보기") { _, _ ->
                updateTabContent(tabLayout.selectedTabPosition)
            }
            .show()
    }

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val themeMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        AppCompatDelegate.setDefaultNightMode(themeMode)
    }

    private fun showThemeSelectionDialog() {
        val themes = arrayOf("시스템 설정값에 따름", "라이트 모드", "다크 모드")
        val sharedPref = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val currentMode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)

        var selectedIndex = when (currentMode) {
            AppCompatDelegate.MODE_NIGHT_NO -> 1
            AppCompatDelegate.MODE_NIGHT_YES -> 2
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("테마 설정")
            .setSingleChoiceItems(themes, selectedIndex) { _, which -> selectedIndex = which }
            .setPositiveButton("확인") { dialog, _ ->
                val newMode = when (selectedIndex) {
                    1 -> AppCompatDelegate.MODE_NIGHT_NO
                    2 -> AppCompatDelegate.MODE_NIGHT_YES
                    else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                }
                sharedPref.edit().putInt("theme_mode", newMode).apply()
                AppCompatDelegate.setDefaultNightMode(newMode)
                updateThemeLabel()
                dialog.dismiss()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun checkFinancialNotificationPermission() {
        val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(this)
        val isGranted = enabledPackages.contains(packageName)

        if (isGranted) {
            Toast.makeText(this, "금융/은행/카톡 결제 알림 접근 권한이 정상 허용되어 있습니다! 👍", Toast.LENGTH_SHORT).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("💳 결제 알림 접근 권한")
                .setMessage("은행/카드사/카카오톡 결제 알림을 자동으로 인식하려면 시스템 설정에서 [가계부] 앱의 '알림 접근'을 허용해 주셔야 합니다.")
                .setPositiveButton("설정하러 가기") { _, _ ->
                    startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                }
                .setNegativeButton("취소", null)
                .show()
        }
    }

    private fun showNoticeDialog() {
        AlertDialog.Builder(this)
            .setTitle("📢 공지사항")
            .setMessage(
                "• [v1.0.0 업데이트]\n" +
                        "  - 가계부/달력/통계/자산/예산/일기메모 완성\n" +
                        "  - 모든 금액 입력 시 실시간 쉼표(,) 자동 포맷터 탑재\n" +
                        "  - 글자 크기(작게/보통/크게) 설정 및 다크모드 지원\n" +
                        "  - 은행/카드/토스/카카오톡 중복방지 자동인식 탑재"
            )
            .setPositiveButton("확인", null)
            .show()
    }

    private fun checkSmsPermissionStatus() {
        val granted = ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            Toast.makeText(this, "SMS 자동인식 권한이 정상 허용되어 있습니다.", Toast.LENGTH_SHORT).show()
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS), 100)
        }
    }

    private fun showClearAllDataDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ 전체 데이터 초기화")
            .setMessage("작성된 모든 가계부 내역, 자산, 일기 메모가 영구히 삭제됩니다.\n정말 초기화하시겠습니까?")
            .setPositiveButton("초기화") { _, _ ->
                thread {
                    for (item in allTransactions) db.transactionDao().deleteTransaction(item)
                    for (asset in allAssets) db.assetDao().deleteAsset(asset)
                    for (memo in allDiaryMemos) db.diaryMemoDao().deleteMemo(memo)
                    runOnUiThread {
                        Toast.makeText(this, "모든 내역이 초기화되었습니다.", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                }
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun sendFeedbackEmail() {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:support@gagebu.com")
            putExtra(Intent.EXTRA_SUBJECT, "[가계부 앱 문의/피드백]")
            putExtra(Intent.EXTRA_TEXT, "기기 정보 및 문의 내용을 작성해 주세요.\n\n")
        }
        try {
            startActivity(Intent.createChooser(emailIntent, "이메일 앱 선택"))
        } catch (e: Exception) {
            Toast.makeText(this, "이메일을 보낼 수 있는 앱이 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateThemeLabel() {
        val sharedPref = getSharedPreferences("theme_prefs", Context.MODE_PRIVATE)
        val mode = sharedPref.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        tvCurrentThemeLabel.text = when (mode) {
            AppCompatDelegate.MODE_NIGHT_NO -> "라이트 모드 〉"
            AppCompatDelegate.MODE_NIGHT_YES -> "다크 모드 〉"
            else -> "시스템 설정 〉"
        }
    }

    private fun exportDataToCsv() {
        try {
            val csvFile = File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "gagebu_backup.csv")
            val writer = FileWriter(csvFile)
            writer.append("ID,날짜,구분,카테고리,금액,메모\n")

            for (t in allTransactions) {
                writer.append("${t.id},${t.date},${t.type},${t.category},${t.amount},\"${t.memo}\"\n")
            }
            writer.flush()
            writer.close()

            Toast.makeText(this, "CSV 백업 완료: ${csvFile.name}", Toast.LENGTH_LONG).show()

            val uri: Uri = FileProvider.getUriForFile(this, "$packageName.provider", csvFile)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(sendIntent, "가계부 백업 파일 공유"))

        } catch (e: Exception) {
            Toast.makeText(this, "CSV 저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadData() {
        thread {
            allTransactions = db.transactionDao().getAllTransactions()
            allAssets = db.assetDao().getAllAssets()
            allDiaryMemos = db.diaryMemoDao().getAllMemos()
            runOnUiThread {
                updateTabContent(tabLayout.selectedTabPosition)
                refreshAllSyncedData()
            }
        }
    }
}