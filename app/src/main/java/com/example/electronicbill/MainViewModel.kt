package com.example.electronicbill

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class MainViewModel : ViewModel() {
    var totalAmount by mutableStateOf("")
    var totalUnits by mutableStateOf("")

    var publicUnitsResult by mutableDoubleStateOf(0.0)
    var publicCostPerPersonResult by mutableDoubleStateOf(0.0)

    val residents = mutableStateListOf<Resident>()

    var historyList by mutableStateOf<List<BillRecord>>(emptyList())

    var isCalculated by mutableStateOf(false)

    var currentLanguage by mutableStateOf("zh")

    fun addResident() {
        residents.add(Resident("住戶 ${'A' + residents.size}"))
    }

    fun removeResident(index: Int) {
        if (residents.size > 1) residents.removeAt(index)
    }

    fun calculate(db: AppDatabase) {
        val billPrice = totalAmount.toDoubleOrNull() ?: 0.0
        val billDegree = totalUnits.toDoubleOrNull() ?: 0.0

        if (billDegree <= 0 || residents.isEmpty()) return

        val pricePerUnit = billPrice / billDegree
        var sumIndividualUnits = 0.0

        residents.forEachIndexed { index, r ->
            val used = (r.currReading.toDoubleOrNull() ?: 0.0) -
                    (r.prevReading.toDoubleOrNull() ?: 0.0)

            sumIndividualUnits += used
            residents[index] = r.copy(usage = used)
        }

        publicUnitsResult = billDegree - sumIndividualUnits
        publicCostPerPersonResult =
            (publicUnitsResult * pricePerUnit) / residents.size

        residents.forEachIndexed { index, r ->
            val finalPrice = (r.usage * pricePerUnit) + publicCostPerPersonResult
            residents[index] = residents[index].copy(
                resultAmount = finalPrice.roundToInt().toDouble()
            )
        }

        isCalculated = true
        saveToDatabase(db)
    }

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
        viewModelScope.launch {
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

        recomputeSummary()
        isCalculated = record.totalAmount > 0
    }

    private fun recomputeSummary() {
        val totalBill = totalAmount.toDoubleOrNull() ?: 0.0
        val totalDegree = totalUnits.toDoubleOrNull() ?: 0.0
        val unitPrice = if (totalDegree > 0) totalBill / totalDegree else 0.0

        val sumIndividualUnits = residents.sumOf { it.usage }
        publicUnitsResult = totalDegree - sumIndividualUnits

        publicCostPerPersonResult =
            if (residents.isNotEmpty()) {
                (publicUnitsResult * unitPrice) / residents.size
            } else {
                0.0
            }
    }

    fun deleteRecord(db: AppDatabase, record: BillRecord) {
        viewModelScope.launch {
            db.billDao().deleteRecord(record)
        }
    }

    fun getAggregatedData(): Pair<Map<String, Double>, Map<String, Double>> {
        val unitMap = mutableMapOf<String, Double>()
        val costMap = mutableMapOf<String, Double>()
        val gson = Gson()
        val listType = object : com.google.gson.reflect.TypeToken<List<Resident>>() {}.type

        historyList.forEach { record ->
            val savedResidents: List<Resident> = gson.fromJson(record.residentsJson, listType)
            savedResidents.forEach { r ->
                unitMap[r.name] = (unitMap[r.name] ?: 0.0) + r.usage
                costMap[r.name] = (costMap[r.name] ?: 0.0) + r.resultAmount
            }
        }
        return Pair(unitMap, costMap)
    }
}