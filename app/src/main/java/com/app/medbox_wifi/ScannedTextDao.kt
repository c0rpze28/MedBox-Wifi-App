package com.app.medbox_wifi

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ScannedTextDao {
    @Insert
    suspend fun insert(scannedText: ScannedText)

    @Query("SELECT * FROM scanned_texts ORDER BY timestamp DESC")
    suspend fun getAll(): List<ScannedText>
}
