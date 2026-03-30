package com.example.electronicbill

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bill_history ORDER BY id DESC LIMIT 1")
    suspend fun getLatestRecord(): BillRecord? // 用於開啟 App 時還原狀態

    @Query("SELECT * FROM bill_history ORDER BY date DESC")
    fun getAllRecordsFlow(): kotlinx.coroutines.flow.Flow<List<BillRecord>> // 用於顯示歷史列表

    @Delete
    suspend fun deleteRecord(record: BillRecord) // 用於刪除特定紀錄

    @Query("SELECT * FROM bill_history ORDER BY date DESC")
    fun getAllRecords(): Flow<List<BillRecord>>

    @Insert
    suspend fun insert(record: BillRecord)
}

@Database(entities = [BillRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
}