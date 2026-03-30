package com.example.electronicbill

import androidx.room.Entity
import androidx.room.PrimaryKey

// 住戶資料結構
data class Resident(
    var name: String = "新住戶",
    var prevReading: String = "", // 前期用電度數
    var currReading: String = "", // 當期用電度數
    var usage: Double = 0.0,      // 個人用電度數 = currReading - prevReading
    var resultAmount: Double = 0.0// 該用戶電費
)

// 資料庫實體
@Entity(tableName = "bill_history")
data class BillRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: Long,
    val totalAmount: Double,
    val totalUnits: Double,
    val residentsJson: String // 存儲住戶清單的 JSON
)