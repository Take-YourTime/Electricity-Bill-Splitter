package com.example.electronicbill

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainViewModel : ViewModel() {
    var totalAmount by mutableStateOf("") // total money to pay
    var totalUnits by mutableStateOf("")  // total kWh used

    var publicCostPerPersonResult by mutableDoubleStateOf(0.0) // public electricity cost per person (publicUnits * unitPrice / resident count)

    // 住戶清單
    val residents = mutableStateListOf<Resident>() // the list of residents

    // 歷史紀錄列表
    var historyList by mutableStateOf<List<BillRecord>>(emptyList()) // history record list

    // 是否已經計算過？（控制顯示詳細過程按鈕）
    var isCalculated by mutableStateOf(false) // Flag: has the calculation been performed at least once?

    // 語言設定
    var currentLanguage by mutableStateOf("zh") // "zh" or "en"


    // add a new resident to residents list
    fun addResident() {
        residents.add(Resident("住戶 ${'A' + residents.size}"))
    }

    // remove a resident form residents list
    // input: the index of the resident to be removed
    fun removeResident(index: Int) {
        if (residents.size > 1) residents.removeAt(index)
    }


    /* --- Calculate electricity bill --- */
    // 1. get the price per unit by "totalAmount / totalUnits"

    // 2. calculate each resident's usage by "currReading - prevReading",
    // and calculate each resident's cost by "usage * price per unit"

    // 3. calculate public electricity units by "totalUnits - sum of individual usage",
    // and calculate public electricity cost per person by "publicUnits * price per unit / resident count"

    var errorMessage by mutableStateOf("")

    // 4. get the final amount each resident should pay by "individual cost + public cost per person"
    fun calculate(db: AppDatabase) {
        errorMessage = "" // Clear previous error
        
        // change the type of totalAmount and totalUnits from String to Double,
        // if failed then set to 0.0
        val billPrice = totalAmount.toDoubleOrNull() ?: 0.0
        var billDegree = totalUnits.toDoubleOrNull() ?: 0.0

        // Guard against division by zero (cannot compute unit price).
        if (billDegree <= 0) {
            errorMessage = "請輸入有效的總用電度數 (大於 0)"
            return
        }


        val pricePerUnit = billPrice / billDegree

        // Calculate every resident's usage degree and cost, and calculate the public electricity degree
        residents.forEachIndexed { index, r ->
            // val now = r.currReading.toDoubleOrNull() ?: 0.0
            // val pre = r.prevReading.toDoubleOrNull() ?: 0.0
            val used = (r.currReading.toDoubleOrNull() ?: 0.0) - (r.prevReading.toDoubleOrNull() ?: 0.0)

            // Store per-resident usage back into state list for UI rendering.
            residents[index] = r.copy(usage = used)
            billDegree -= used
        }

        // Split public electricity cost equally among all residents.
        publicCostPerPersonResult = (billDegree * pricePerUnit) / residents.size

        // Update the final electricity bill amount of each resident
        residents.forEachIndexed { index, r ->
            val finalPrice = (r.usage * pricePerUnit) + publicCostPerPersonResult
            residents[index] = residents[index].copy(resultAmount = finalPrice.roundToInt().toDouble())
        }

        // Show the "View Details" button after calculation, so users can review the breakdown.
        isCalculated = true

        // save data to database
        saveToDatabase(db)
    }

    /* --- 資料庫操作 (Room) --- */
    private fun saveToDatabase(db: AppDatabase) {
        viewModelScope.launch {
            // Persist full snapshot so history can restore same residents and results.
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
        viewModelScope.launch {
            // Observe history as a Flow so drawer list updates in real time.
            db.billDao().getAllRecordsFlow().collect { list ->
                historyList = list
                if (residents.isEmpty() && list.isNotEmpty()) {
                    // First launch with data: auto-load the newest record.
                    applyRecord(list.first())
                } else if (residents.isEmpty()) {
                    // First launch without data: start with one resident row.
                    addResident()
                }
            }
        }
    }

    fun applyRecord(record: BillRecord) {
        // Restore top-level bill fields.
        totalAmount = record.totalAmount.toString()
        totalUnits = record.totalUnits.toString()

        val listType = object : com.google.gson.reflect.TypeToken<List<Resident>>() {}.type
        val saved: List<Resident> = Gson().fromJson(record.residentsJson, listType)

        residents.clear()
        // Restore resident list (name/readings/results) from stored JSON.
        residents.addAll(saved)

        // 載入紀錄時預設已計算過
        isCalculated = record.totalAmount > 0
    }

    fun deleteRecord(db: AppDatabase, record: BillRecord) {
        viewModelScope.launch { db.billDao().deleteRecord(record) }
    }

    // 分析數據聚合邏輯
    fun getAggregatedData(): Pair<Map<String, Double>, Map<String, Double>> {
        // unitMap: accumulated kWh by resident name, costMap: accumulated payment.
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