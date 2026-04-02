package com.example.electronicbill

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bill_history ORDER BY id DESC LIMIT 1")
    suspend fun getLatestRecord(): BillRecord?

    @Query("SELECT * FROM bill_history ORDER BY date DESC")
    fun getAllRecordsFlow(): Flow<List<BillRecord>>

    @Delete
    suspend fun deleteRecord(record: BillRecord)

    @Insert
    suspend fun insert(record: BillRecord)
}

@Database(entities = [BillRecord::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun billDao(): BillDao
}
