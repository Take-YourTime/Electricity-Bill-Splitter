package com.example.electronicbill

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainViewModel : ViewModel() {
    /* --- 介面狀態 (State) --- */
    var totalAmount by mutableStateOf("") // 帳單總金額
    var totalUnits by mutableStateOf("")  // 帳單總度數

    // 公電計算結果
    var publicUnitsResult by mutableStateOf(0.0)
    var publicCostPerPersonResult by mutableStateOf(0.0)

    // 住戶清單
    val residents = mutableStateListOf<Resident>()

    // 歷史紀錄列表
    var historyList by mutableStateOf<List<BillRecord>>(emptyList())

    // 新增：是否已經計算過？（控制顯示詳細過程按鈕）
    var isCalculated by mutableStateOf(false)

    // 語言設定
    var currentLanguage by mutableStateOf("zh") // "zh" 或 "en"

    // 資料庫操作的 Job 參考，確保在 ViewModel 清除時取消
    private var dbJob: Job? = null

    // --- 🚀 全域導覽鎖 ---
    var isNavigating by mutableStateOf(false)
        private set

    // 封裝導覽執行器：確保 600ms 內只會執行一個導覽動作
    fun performNavigation(action: () -> Unit) {
        if (!isNavigating) {
            isNavigating = true
            action()
            viewModelScope.launch {
                // 這裡的延遲時間建議略長於 Compose 的預設轉場動畫 (約 300-500ms)
                delay(700)
                isNavigating = false
            }
        }
    }

    /* --- 住戶管理功能 --- */
    fun addResident() {
        residents.add(Resident("住戶 ${'A' + residents.size}"))
    }

    fun removeResident(index: Int) {
        if (residents.size > 1) residents.removeAt(index)
    }

    /* --- 核心計算邏輯 (C++ 邏輯) --- */
    fun calculate(db: AppDatabase) {
        val billPrice = totalAmount.toDoubleOrNull() ?: 0.0
        val billDegree = totalUnits.toDoubleOrNull() ?: 0.0
        if (billDegree <= 0) return

        val pricePerUnit = billPrice / billDegree
        var sumIndividualUnits = 0.0

        // 計算每位住戶的用電量
        residents.forEachIndexed { index, r ->
            val now = r.currReading.toDoubleOrNull() ?: 0.0
            val pre = r.prevReading.toDoubleOrNull() ?: 0.0
            val used = now - pre
            residents[index] = r.copy(usage = used)
            sumIndividualUnits += used
        }

        // 公電計算
        publicUnitsResult = billDegree - sumIndividualUnits
        publicCostPerPersonResult = (publicUnitsResult * pricePerUnit) / residents.size

        // 更新最終應付金額 (含公電)
        residents.forEachIndexed { index, r ->
            val finalPrice = (r.usage * pricePerUnit) + publicCostPerPersonResult
            residents[index] = residents[index].copy(resultAmount = finalPrice.roundToInt().toDouble())
        }

        // --- 重點：更新計算狀態 ---
        isCalculated = true

        // 計算完畢後自動儲存到資料庫
        saveToDatabase(db)
    }

    /* --- 資料庫操作 (Room) --- */
    private fun saveToDatabase(db: AppDatabase) {
        viewModelScope.launch {
            val record = BillRecord(
                date = System.currentTimeMillis(),
                totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
                totalUnits = totalUnits.toDoubleOrNull() ?: 0.0,
                residentsJson = Gson().toJson(residents.toList())
            )
            db.billDao().insert(record)
        }
    }

    fun initData(db: AppDatabase) {
        dbJob?.cancel()
        dbJob = viewModelScope.launch {
            db.billDao().getAllRecordsFlow().collect { list ->
                historyList = list
                if (residents.isEmpty() && list.isNotEmpty()) {
                    applyRecord(list.first())
                } else if (residents.isEmpty()) {
                    addResident()
                }
            }
        }
    }

    fun applyRecord(record: BillRecord) {
        totalAmount = record.totalAmount.toString()
        totalUnits = record.totalUnits.toString()

        val listType = object : com.google.gson.reflect.TypeToken<List<Resident>>() {}.type
        val saved: List<Resident> = Gson().fromJson(record.residentsJson, listType)

        residents.clear()
        residents.addAll(saved)

        // 載入紀錄時預設已計算過
        isCalculated = record.totalAmount > 0
    }

    fun deleteRecord(db: AppDatabase, record: BillRecord) {
        viewModelScope.launch { db.billDao().deleteRecord(record) }
    }

    // 分析數據聚合邏輯
    fun getAggregatedData(): Pair<Map<String, Double>, Map<String, Double>> {
        val unitMap = mutableMapOf<String, Double>()
        val costMap = mutableMapOf<String, Double>()
        val gson = Gson()
        val listType = object : com.google.gson.reflect.TypeToken<List<Resident>>() {}.type

        historyList.forEach { record ->
            val savedResidents: List<Resident> = gson.fromJson(record.residentsJson, listType)
            savedResidents.forEach { r ->
                // 根據名稱累加度數與金額
                unitMap[r.name] = (unitMap[r.name] ?: 0.0) + r.usage
                costMap[r.name] = (costMap[r.name] ?: 0.0) + r.resultAmount
            }
        }
        return Pair(unitMap, costMap)
    }
}