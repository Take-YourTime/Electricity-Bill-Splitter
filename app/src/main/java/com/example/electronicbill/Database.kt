package com.example.electronicbill

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bill_history ORDER BY id DESC LIMIT 1")
    // One-shot fetch used for restoring the newest snapshot if needed.
    suspend fun getLatestRecord(): BillRecord? // 用於開啟 App 時還原狀態

    @Query("SELECT * FROM bill_history ORDER BY date DESC")
    // Reactive stream for UI lists that should auto-refresh on DB changes.
    fun getAllRecordsFlow(): kotlinx.coroutines.flow.Flow<List<BillRecord>> // 用於顯示歷史列表

    @Delete
    // Remove a single selected history entry.
    suspend fun deleteRecord(record: BillRecord) // 用於刪除特定紀錄

    @Query("SELECT * FROM bill_history ORDER BY date DESC")
    // Alternative full-history stream kept for possible future use.
    fun getAllRecords(): Flow<List<BillRecord>>

    @Insert
    // Insert one calculated bill snapshot.
    suspend fun insert(record: BillRecord)
}

@Database(entities = [BillRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    // Main access point for bill-history data operations.
    abstract fun billDao(): BillDao
}