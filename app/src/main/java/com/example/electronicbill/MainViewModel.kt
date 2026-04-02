package com.example.electronicbill

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

sealed class CalculationResult(open val message: String) {
    data class Success(override val message: String) : CalculationResult(message)
    data class Error(override val message: String) : CalculationResult(message)
}

data class AggregatedData(
    val unitData: Map<String, Double> = emptyMap(),
    val costData: Map<String, Double> = emptyMap()
)

class MainViewModel : ViewModel() {
    var totalAmount by mutableStateOf("")
    var totalUnits by mutableStateOf("")

    var publicUnitsResult by mutableDoubleStateOf(0.0)
    var publicCostPerPersonResult by mutableDoubleStateOf(0.0)

    val residents = mutableStateListOf<Resident>()

    var historyList by mutableStateOf<List<BillRecord>>(emptyList())
    var aggregatedData by mutableStateOf(AggregatedData())

    var isCalculated by mutableStateOf(false)
    var isSaving by mutableStateOf(false)

    var currentLanguage by mutableStateOf("zh")

    private val gson = Gson()
    private val residentListType = object : TypeToken<List<Resident>>() {}.type

    fun addResident() {
        val nextNumber = residents.size + 1
        val defaultName = if (currentLanguage == "zh") {
            "住戶 $nextNumber"
        } else {
            "Resident $nextNumber"
        }
        residents.add(Resident(name = defaultName))
    }

    fun removeResident(index: Int) {
        if (residents.size > 1) residents.removeAt(index)
    }

    private fun invalidateCalculationState() {
        isCalculated = false
        publicUnitsResult = 0.0
        publicCostPerPersonResult = 0.0
        residents.forEachIndexed { index, resident ->
            residents[index] = resident.copy(usage = 0.0, resultAmount = 0.0)
        }
    }

    suspend fun calculateAndSave(db: AppDatabase): CalculationResult {
        invalidateCalculationState()
        if (isSaving) {
            return CalculationResult.Error(
                if (currentLanguage == "zh") "正在存檔中，請稍候" else "Saving in progress. Please wait."
            )
        }

        val billPrice = totalAmount.toDoubleOrNull()
            ?: return CalculationResult.Error(
                if (currentLanguage == "zh") "請輸入有效的總金額" else "Please enter a valid total amount."
            )

        val billDegree = totalUnits.toDoubleOrNull()
            ?: return CalculationResult.Error(
                if (currentLanguage == "zh") "請輸入有效的總度數" else "Please enter valid total units."
            )

        if (billPrice < 0) {
            return CalculationResult.Error(
                if (currentLanguage == "zh") "總金額不可為負數" else "Total amount cannot be negative."
            )
        }

        if (billDegree <= 0) {
            return CalculationResult.Error(
                if (currentLanguage == "zh") "總度數必須大於 0" else "Total units must be greater than 0."
            )
        }

        if (residents.isEmpty()) {
            return CalculationResult.Error(
                if (currentLanguage == "zh") "至少需要一位住戶" else "At least one resident is required."
            )
        }

        val validatedResidents = mutableListOf<Resident>()
        var sumIndividualUnits = 0.0

        residents.forEachIndexed { index, resident ->
            val displayName = resident.name.trim().ifBlank {
                if (currentLanguage == "zh") "住戶 ${index + 1}" else "Resident ${index + 1}"
            }

            val prev = resident.prevReading.trim().toDoubleOrNull()
                ?: return CalculationResult.Error(
                    if (currentLanguage == "zh") {
                        "$displayName 的前期度數格式不正確"
                    } else {
                        "Invalid previous reading for $displayName."
                    }
                )

            val curr = resident.currReading.trim().toDoubleOrNull()
                ?: return CalculationResult.Error(
                    if (currentLanguage == "zh") {
                        "$displayName 的當期度數格式不正確"
                    } else {
                        "Invalid current reading for $displayName."
                    }
                )

            if (prev < 0 || curr < 0) {
                return CalculationResult.Error(
                    if (currentLanguage == "zh") {
                        "$displayName 的電表讀數不可為負數"
                    } else {
                        "Meter readings for $displayName cannot be negative."
                    }
                )
            }

            if (curr < prev) {
                return CalculationResult.Error(
                    if (currentLanguage == "zh") {
                        "$displayName 的當期度數不可小於前期度數"
                    } else {
                        "Current reading cannot be smaller than previous reading for $displayName."
                    }
                )
            }

            val usage = curr - prev
            sumIndividualUnits += usage
            validatedResidents += resident.copy(
                name = displayName,
                prevReading = resident.prevReading.trim(),
                currReading = resident.currReading.trim(),
                usage = usage
            )
        }

        if (sumIndividualUnits > billDegree + 1e-9) {
            return CalculationResult.Error(
                if (currentLanguage == "zh") {
                    "所有住戶用電總和不可大於帳單總度數"
                } else {
                    "Sum of resident usage cannot exceed the bill total units."
                }
            )
        }

        val pricePerUnit = billPrice / billDegree
        publicUnitsResult = (billDegree - sumIndividualUnits).coerceAtLeast(0.0)
        publicCostPerPersonResult =
            (publicUnitsResult * pricePerUnit) / validatedResidents.size

        isSaving = true
        try {
            // 將計算結果更新回 residents 列表
            validatedResidents.forEachIndexed { index, resident ->
                val finalPrice = (resident.usage * pricePerUnit) + publicCostPerPersonResult
                residents[index] = resident.copy(
                    resultAmount = finalPrice.roundToInt().toDouble()
                )
            }

            isCalculated = true
            saveToDatabase(db)
            return CalculationResult.Success(
                if (currentLanguage == "zh") "成功存檔" else "Saved successfully."
            )
        } finally {
            isSaving = false
        }
    }

    private suspend fun saveToDatabase(db: AppDatabase) {
        val record = BillRecord(
            date = System.currentTimeMillis(),
            totalAmount = totalAmount.toDoubleOrNull() ?: 0.0,
            totalUnits = totalUnits.toDoubleOrNull() ?: 0.0,
            residentsJson = gson.toJson(residents.toList())
        )
        db.billDao().insert(record)
    }

    fun initData(db: AppDatabase) {
        viewModelScope.launch {
            db.billDao().getAllRecordsFlow().collect { list ->
                historyList = list
                rebuildAggregatedData(list)

                if (residents.isEmpty() && list.isNotEmpty()) {
                    applyRecord(list.first())
                } else if (residents.isEmpty()) {
                    addResident()
                }
            }
        }
    }

    fun applyRecord(record: BillRecord) {
        totalAmount = trimTrailingZero(record.totalAmount)
        totalUnits = trimTrailingZero(record.totalUnits)

        val saved: List<Resident> = runCatching {
            gson.fromJson<List<Resident>>(record.residentsJson, residentListType)
        }.getOrDefault(emptyList())

        residents.clear()
        if (saved.isEmpty()) {
            addResident()
        } else {
            residents.addAll(saved.mapIndexed { index, resident ->
                resident.copy(
                    name = resident.name.ifBlank {
                        if (currentLanguage == "zh") "住戶 ${index + 1}" else "Resident ${index + 1}"
                    }
                )
            })
        }

        recomputeSummary()
        isCalculated = record.totalAmount > 0 && record.totalUnits > 0
    }

    fun deleteRecord(db: AppDatabase, record: BillRecord) {
        viewModelScope.launch {
            db.billDao().deleteRecord(record)
        }
    }

    private fun recomputeSummary() {
        val totalBill = totalAmount.toDoubleOrNull() ?: 0.0
        val totalDegree = totalUnits.toDoubleOrNull() ?: 0.0
        val unitPrice = if (totalDegree > 0) totalBill / totalDegree else 0.0

        val sumIndividualUnits = residents.sumOf { it.usage }
        publicUnitsResult = (totalDegree - sumIndividualUnits).coerceAtLeast(0.0)

        publicCostPerPersonResult =
            if (residents.isNotEmpty()) {
                (publicUnitsResult * unitPrice) / residents.size
            } else {
                0.0
            }
    }

    private fun rebuildAggregatedData(records: List<BillRecord>) {
        val unitMap = linkedMapOf<String, Double>()
        val costMap = linkedMapOf<String, Double>()

        records.forEach { record ->
            val savedResidents: List<Resident> = runCatching {
                gson.fromJson<List<Resident>>(record.residentsJson, residentListType)
            }.getOrDefault(emptyList())

            savedResidents.forEachIndexed { index, resident ->
                val name = resident.name.ifBlank {
                    if (currentLanguage == "zh") "住戶 ${index + 1}" else "Resident ${index + 1}"
                }
                unitMap[name] = (unitMap[name] ?: 0.0) + resident.usage
                costMap[name] = (costMap[name] ?: 0.0) + resident.resultAmount
            }
        }

        aggregatedData = AggregatedData(
            unitData = unitMap.toMap(),
            costData = costMap.toMap()
        )
    }

    private fun trimTrailingZero(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }
}
