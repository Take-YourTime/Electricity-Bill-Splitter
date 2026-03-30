package com.example.electronicbill

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import kotlin.math.roundToInt

class MainViewModel : ViewModel() {
    var totalAmount by mutableStateOf("")
    var totalUnits by mutableStateOf("")

    // 公電資訊狀態
    var publicUnitsResult by mutableStateOf(0.0)
    var publicCostPerPersonResult by mutableStateOf(0.0)

    val residents = mutableStateListOf(Resident("住戶 A"))

    fun addResident() {
        residents.add(Resident("住戶 ${'A' + residents.size}"))
    }

    fun removeResident(index: Int) {
        if (residents.size > 1) residents.removeAt(index)
    }

    fun calculate() {
        val totalBillPrice = totalAmount.toDoubleOrNull() ?: 0.0
        val billTotalDegree = totalUnits.toDoubleOrNull() ?: 0.0
        if (billTotalDegree <= 0) return

        val pricePerUnit = totalBillPrice / billTotalDegree
        var sumIndividualUnits = 0.0

        // 計算各戶用電
        residents.forEachIndexed { index, r ->
            val now = r.currReading.toDoubleOrNull() ?: 0.0
            val pre = r.prevReading.toDoubleOrNull() ?: 0.0
            val used = now - pre
            residents[index] = r.copy(usage = used) // 儲存個人度數
            sumIndividualUnits += used
        }

        // 公電邏輯 (與你的 C++ 檔一致)
        publicUnitsResult = billTotalDegree - sumIndividualUnits
        publicCostPerPersonResult = (publicUnitsResult * pricePerUnit) / residents.size

        // 更新最終金額
        residents.forEachIndexed { index, r ->
            val finalPrice = (r.usage * pricePerUnit) + publicCostPerPersonResult
            residents[index] = residents[index].copy(resultAmount = finalPrice.roundToInt().toDouble())
        }
    }
}