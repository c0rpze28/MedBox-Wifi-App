package com.app.medbox_wifi

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MedicineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medicines: List<Medicine>)

    /**
     * Case-insensitive matching that checks if a medicine name exists within the scanned text.
     */
    @Query("""
        SELECT * FROM ph_medicines 
        WHERE UPPER(:scannedText) LIKE '%' || UPPER(brandName) || '%' 
        OR UPPER(:scannedText) LIKE '%' || UPPER(genericName) || '%'
        LIMIT 1
    """)
    suspend fun findMatchingMedicine(scannedText: String): Medicine?

    @Query("SELECT COUNT(*) FROM ph_medicines")
    suspend fun getMedicineCount(): Int
}
