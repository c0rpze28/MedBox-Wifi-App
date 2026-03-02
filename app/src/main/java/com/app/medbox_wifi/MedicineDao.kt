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
     * Finds the best matching medicine using a scoring system:
     * 1. Brand matches get 100 points + their length.
     * 2. Generic matches get 50 points + their length.
     * This ensures 'Alaxan' beats 'Biogesic' even if 'Paracetamol' is detected.
     */
    @Query("""
        SELECT *, 
        (CASE 
            WHEN (brandName != '' AND UPPER(:scannedText) LIKE '%' || UPPER(brandName) || '%') THEN 100 + LENGTH(brandName)
            WHEN (genericName != '' AND UPPER(:scannedText) LIKE '%' || UPPER(genericName) || '%') THEN 50 + LENGTH(genericName)
            ELSE 0 
        END) as matchScore
        FROM ph_medicines 
        WHERE matchScore > 0
        ORDER BY matchScore DESC
        LIMIT 1
    """)
    suspend fun findMatchingMedicine(scannedText: String): Medicine?

    @Query("SELECT COUNT(*) FROM ph_medicines")
    suspend fun getMedicineCount(): Int
}
