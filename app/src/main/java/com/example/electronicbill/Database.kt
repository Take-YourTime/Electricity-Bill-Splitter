package com.example.electronicbill

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bill_history ORDER BY date DESC")
    fun getAllRecords(): Flow<List<BillRecord>>

    @Insert
    suspend fun insert(record: BillRecord)
}

@Database(entities = [BillRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
}