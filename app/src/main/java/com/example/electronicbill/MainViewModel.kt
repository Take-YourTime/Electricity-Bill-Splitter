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
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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
        val defaultName =
                if (currentLanguage == "zh") {
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

        val billPrice =
                totalAmount.toDoubleOrNull()
                        ?: return CalculationResult.Error(
                                if (currentLanguage == "zh") "請輸入有效的總金額"
                                else "Please enter a valid total amount."
                        )

        val billDegree =
                totalUnits.toDoubleOrNull()
                        ?: return CalculationResult.Error(
                                if (currentLanguage == "zh") "請輸入有效的總度數"
                                else "Please enter valid total units."
                        )

        if (billPrice < 0) {
            return CalculationResult.Error(
                    if (currentLanguage == "zh") "總金額不可為負數" else "Total amount cannot be negative."
            )
        }

        if (billDegree <= 0) {
            return CalculationResult.Error(
                    if (currentLanguage == "zh") "總度數必須大於 0"
                    else "Total units must be greater than 0."
            )
        }

        if (residents.isEmpty()) {
            return CalculationResult.Error(
                    if (currentLanguage == "zh") "至少需要一位住戶"
                    else "At least one resident is required."
            )
        }

        val validatedResidents = mutableListOf<Resident>()
        var sumIndividualUnits = 0.0

        residents.forEachIndexed { index, resident ->
            val displayName =
                    resident.name.trim().ifBlank {
                        if (currentLanguage == "zh") "住戶 ${index + 1}" else "Resident ${index + 1}"
                    }

            val prev =
                    resident.prevReading.trim().toDoubleOrNull()
                            ?: return CalculationResult.Error(
                                    if (currentLanguage == "zh") {
                                        "$displayName 的前期度數格式不正確"
                                    } else {
                                        "Invalid previous reading for $displayName."
                                    }
                            )

            val curr =
                    resident.currReading.trim().toDoubleOrNull()
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
            validatedResidents +=
                    resident.copy(
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
        publicCostPerPersonResult = (publicUnitsResult * pricePerUnit) / validatedResidents.size

        isSaving = true
        try {
            // 將計算結果更新回 residents 列表
            validatedResidents.forEachIndexed { index, resident ->
                val finalPrice = (resident.usage * pricePerUnit) + publicCostPerPersonResult
                residents[index] = resident.copy(resultAmount = finalPrice.roundToInt().toDouble())
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
        val record =
                BillRecord(
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

        val saved: List<Resident> =
                runCatching {
                            gson.fromJson<List<Resident>>(record.residentsJson, residentListType)
                        }
                        .getOrDefault(emptyList())

        residents.clear()
        if (saved.isEmpty()) {
            addResident()
        } else {
            residents.addAll(
                    saved.mapIndexed { index, resident ->
                        resident.copy(
                                name =
                                        resident.name.ifBlank {
                                            if (currentLanguage == "zh") "住戶 ${index + 1}"
                                            else "Resident ${index + 1}"
                                        }
                        )
                    }
            )
        }

        recomputeSummary()
        isCalculated = record.totalAmount > 0 && record.totalUnits > 0
    }

    fun deleteRecord(db: AppDatabase, record: BillRecord) {
        viewModelScope.launch { db.billDao().deleteRecord(record) }
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
            val savedResidents: List<Resident> =
                    runCatching {
                                gson.fromJson<List<Resident>>(
                                        record.residentsJson,
                                        residentListType
                                )
                            }
                            .getOrDefault(emptyList())

            savedResidents.forEachIndexed { index, resident ->
                val name =
                        resident.name.ifBlank {
                            if (currentLanguage == "zh") "住戶 ${index + 1}"
                            else "Resident ${index + 1}"
                        }
                unitMap[name] = (unitMap[name] ?: 0.0) + resident.usage
                costMap[name] = (costMap[name] ?: 0.0) + resident.resultAmount
            }
        }

        aggregatedData = AggregatedData(unitData = unitMap.toMap(), costData = costMap.toMap())
    }

    private fun trimTrailingZero(value: Double): String {
        return if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }

    fun saveTempData(context: android.content.Context) {
        val sharedPrefs =
                context.getSharedPreferences(
                        "ElectricBillPrefs",
                        android.content.Context.MODE_PRIVATE
                )
        val dataMap =
                mapOf(
                        "totalAmount" to totalAmount,
                        "totalUnits" to totalUnits,
                        "residents" to gson.toJson(residents.toList())
                )
        sharedPrefs.edit().putString("temp_data", gson.toJson(dataMap)).apply()
    }

    fun restoreTempData(context: android.content.Context) {
        val sharedPrefs =
                context.getSharedPreferences(
                        "ElectricBillPrefs",
                        android.content.Context.MODE_PRIVATE
                )
        val json = sharedPrefs.getString("temp_data", null)
        if (json != null) {
            try {
                val type = object : TypeToken<Map<String, String>>() {}.type
                val dataMap: Map<String, String> = gson.fromJson(json, type)

                totalAmount = dataMap["totalAmount"] ?: ""
                totalUnits = dataMap["totalUnits"] ?: ""

                val residentsJson = dataMap["residents"]
                if (residentsJson != null) {
                    val savedResidents: List<Resident> =
                            gson.fromJson(residentsJson, residentListType)
                    if (savedResidents.isNotEmpty()) {
                        residents.clear()
                        residents.addAll(savedResidents)
                    }
                }

                sharedPrefs.edit().remove("temp_data").apply()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun generateQRCodeBitmap(url: String, size: Int): android.graphics.Bitmap? {
        return try {
            val bitMatrix =
                    com.google.zxing.MultiFormatWriter()
                            .encode(
                                    url,
                                    com.google.zxing.BarcodeFormat.QR_CODE,
                                    size,
                                    size,
                                    mapOf(com.google.zxing.EncodeHintType.MARGIN to 1)
                            )
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap =
                    android.graphics.Bitmap.createBitmap(
                            width,
                            height,
                            android.graphics.Bitmap.Config.ARGB_8888
                    )
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                            x,
                            y,
                            if (bitMatrix[x, y]) android.graphics.Color.BLACK
                            else android.graphics.Color.TRANSPARENT
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun shareResultAsImage(context: android.content.Context) {
        if (!isCalculated) return

        val width = 1080
        val padding = 80f

        // Typography variables
        val titleTextSize = 72f
        val subtitleTextSize = 42f
        val bodyTextSize = 36f
        val largeNumberSize = 56f
        val smallTextSize = 28f

        // Colors (Minimalist modern)
        val bgColor = android.graphics.Color.parseColor("#F9FAFB")
        val cardColor = android.graphics.Color.WHITE
        val primaryTextColor = android.graphics.Color.parseColor("#111827")
        val secondaryTextColor = android.graphics.Color.parseColor("#6B7280")
        val accentColor = android.graphics.Color.parseColor("#4F46E5")
        val highlightColor = android.graphics.Color.parseColor("#EF4444")

        val headerHeight = 730f
        val itemHeight = 160f
        val qrCodeSize = 250
        val footerHeight = 400f

        val height = (headerHeight + (residents.size * itemHeight) + footerHeight).toInt()

        val bitmap =
                android.graphics.Bitmap.createBitmap(
                        width,
                        height,
                        android.graphics.Bitmap.Config.ARGB_8888
                )
        val canvas = android.graphics.Canvas(bitmap)
        canvas.drawColor(bgColor)

        val paint =
                android.graphics.Paint().apply {
                    isAntiAlias = true
                    typeface =
                            android.graphics.Typeface.create(
                                    android.graphics.Typeface.SANS_SERIF,
                                    android.graphics.Typeface.NORMAL
                            )
                }

        var y = padding + 60f

        // 1. Title
        paint.color = primaryTextColor
        paint.textSize = titleTextSize
        paint.typeface =
                android.graphics.Typeface.create(
                        android.graphics.Typeface.SANS_SERIF,
                        android.graphics.Typeface.BOLD
                )
        val title = if (currentLanguage == "zh") "本期電費" else "Electricity Bill"
        canvas.drawText(title, padding, y, paint)

        // Accent line
        paint.color = accentColor
        canvas.drawRoundRect(padding, y + 30f, padding + 120f, y + 40f, 5f, 5f, paint)

        y += 120f

        // 2. Summary Card
        val cardRect = android.graphics.RectF(padding, y, width - padding, y + 180f)
        paint.color = cardColor
        paint.setShadowLayer(15f, 0f, 5f, android.graphics.Color.parseColor("#1A000000"))
        canvas.drawRoundRect(cardRect, 24f, 24f, paint)
        paint.clearShadowLayer()

        paint.typeface =
                android.graphics.Typeface.create(
                        android.graphics.Typeface.SANS_SERIF,
                        android.graphics.Typeface.NORMAL
                )
        y += 70f

        val col1X = padding + 50f
        val col2X = width / 2f + 20f

        paint.color = secondaryTextColor
        paint.textSize = smallTextSize
        canvas.drawText(if (currentLanguage == "zh") "總金額" else "Total Amount", col1X, y, paint)
        canvas.drawText(if (currentLanguage == "zh") "總度數" else "Total Units", col2X, y, paint)

        y += 50f
        paint.color = primaryTextColor
        paint.textSize = largeNumberSize
        paint.typeface =
                android.graphics.Typeface.create(
                        android.graphics.Typeface.SANS_SERIF,
                        android.graphics.Typeface.BOLD
                )
        canvas.drawText("$totalAmount", col1X, y, paint)
        canvas.drawText("$totalUnits", col2X, y, paint)

        y += 70f

        // 3. Public Units Info
        val pubCardRect = android.graphics.RectF(padding, y, width - padding, y + 160f)
        paint.color = android.graphics.Color.parseColor("#EEF2FF")
        canvas.drawRoundRect(pubCardRect, 24f, 24f, paint)

        y += 70f
        paint.typeface =
                android.graphics.Typeface.create(
                        android.graphics.Typeface.SANS_SERIF,
                        android.graphics.Typeface.NORMAL
                )
        paint.color = accentColor
        paint.textSize = smallTextSize
        canvas.drawText(if (currentLanguage == "zh") "公電總度數" else "Public Units", col1X, y, paint)
        canvas.drawText(
                if (currentLanguage == "zh") "每人公電費" else "Public Cost/Person",
                col2X,
                y,
                paint
        )

        y += 50f
        paint.typeface =
                android.graphics.Typeface.create(
                        android.graphics.Typeface.SANS_SERIF,
                        android.graphics.Typeface.BOLD
                )
        paint.textSize = bodyTextSize
        canvas.drawText(
                "${String.format(java.util.Locale.getDefault(), "%.1f", publicUnitsResult)} ${if (currentLanguage == "zh") "度" else "kWh"}",
                col1X,
                y,
                paint
        )
        canvas.drawText(
                "$${String.format(java.util.Locale.getDefault(), "%.0f", publicCostPerPersonResult)}",
                col2X,
                y,
                paint
        )

        y += 120f

        // 4. Residents
        paint.color = secondaryTextColor
        paint.textSize = smallTextSize
        paint.typeface =
                android.graphics.Typeface.create(
                        android.graphics.Typeface.SANS_SERIF,
                        android.graphics.Typeface.BOLD
                )
        canvas.drawText(
                if (currentLanguage == "zh") "住戶明細" else "Residents Detail",
                padding,
                y,
                paint
        )
        y += 40f

        val rightAlignPaint =
                android.graphics.Paint(paint).apply {
                    textAlign = android.graphics.Paint.Align.RIGHT
                }

        residents.forEach { resident ->
            paint.color = cardColor
            val itemRect = android.graphics.RectF(padding, y, width - padding, y + itemHeight - 20f)
            canvas.drawRoundRect(itemRect, 20f, 20f, paint)

            val itemY = y + 60f

            paint.color = primaryTextColor
            paint.textSize = subtitleTextSize
            paint.typeface =
                    android.graphics.Typeface.create(
                            android.graphics.Typeface.SANS_SERIF,
                            android.graphics.Typeface.BOLD
                    )
            canvas.drawText(resident.name, padding + 40f, itemY, paint)

            paint.color = secondaryTextColor
            paint.textSize = bodyTextSize
            paint.typeface =
                    android.graphics.Typeface.create(
                            android.graphics.Typeface.SANS_SERIF,
                            android.graphics.Typeface.NORMAL
                    )
            canvas.drawText(
                    if (currentLanguage == "zh")
                            "用電: ${String.format(java.util.Locale.getDefault(), "%.1f", resident.usage)} 度"
                    else
                            "Usage: ${String.format(java.util.Locale.getDefault(), "%.1f", resident.usage)} kWh",
                    padding + 40f,
                    itemY + 50f,
                    paint
            )

            rightAlignPaint.color = highlightColor
            rightAlignPaint.textSize = subtitleTextSize
            rightAlignPaint.typeface =
                    android.graphics.Typeface.create(
                            android.graphics.Typeface.SANS_SERIF,
                            android.graphics.Typeface.BOLD
                    )
            val costStr =
                    "$${String.format(java.util.Locale.getDefault(), "%.0f", resident.resultAmount)}"
            canvas.drawText(costStr, width - padding - 40f, itemY + 25f, rightAlignPaint)

            y += itemHeight
        }

        // 5. Footer (App Info & QR Code centered)
        y += 80f
        
        val leftCenterX = width / 2f - 200f
        val rightCenterX = width / 2f + 160f
        
        // Draw App Icon
        val iconSize = 160
        val iconX = leftCenterX - iconSize / 2f
        val iconY = y + (qrCodeSize - iconSize) / 2f - 20f
        
        try {
            val drawable = context.packageManager.getApplicationIcon(context.applicationInfo)
            val iconBitmap = android.graphics.Bitmap.createBitmap(iconSize, iconSize, android.graphics.Bitmap.Config.ARGB_8888)
            val iconCanvas = android.graphics.Canvas(iconBitmap)
            drawable.setBounds(0, 0, iconSize, iconSize)
            drawable.draw(iconCanvas)
            canvas.drawBitmap(iconBitmap, iconX, iconY, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Draw App Name
        paint.color = primaryTextColor
        paint.textSize = bodyTextSize
        paint.typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
        paint.textAlign = android.graphics.Paint.Align.CENTER
        val appName = if (currentLanguage == "zh") "電費分攤助手" else "Elec. Bill Splitter"
        canvas.drawText(appName, leftCenterX, y + qrCodeSize + 40f, paint)

        // Draw QR Code on the right
        val qrCodeBitmap =
                generateQRCodeBitmap(
                        "https://github.com/Take-YourTime/Electricity-Bill-Splitter",
                        qrCodeSize
                )
        if (qrCodeBitmap != null) {
            val qrX = rightCenterX - qrCodeSize / 2f
            canvas.drawBitmap(qrCodeBitmap, qrX, y, null)

            paint.color = secondaryTextColor
            paint.textSize = smallTextSize
            paint.typeface =
                    android.graphics.Typeface.create(
                            android.graphics.Typeface.SANS_SERIF,
                            android.graphics.Typeface.NORMAL
                    )
            paint.textAlign = android.graphics.Paint.Align.CENTER
            val scanText = if (currentLanguage == "zh") "Github頁面" else "Github Page"
            canvas.drawText(scanText, rightCenterX, y + qrCodeSize + 40f, paint)
        }

        try {
            val cachePath = java.io.File(context.cacheDir, "images")
            cachePath.mkdirs()
            val file = java.io.File(cachePath, "bill_result.png")
            val stream = java.io.FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()

            val uri =
                    androidx.core.content.FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.fileprovider",
                            file
                    )
            val shareIntent =
                    android.content.Intent().apply {
                        action = android.content.Intent.ACTION_SEND
                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                        type = "image/png"
                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
            context.startActivity(
                    android.content.Intent.createChooser(
                            shareIntent,
                            if (currentLanguage == "zh") "分享計算結果" else "Share Result"
                    )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
