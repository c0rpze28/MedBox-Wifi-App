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
     * Finds medicines where the brand name or generic name matches or is part of the scanned text.
     * We order by brandName length descending to prioritize more specific matches (e.g., Alaxan over Biogesic if both match).
     */
    @Query("""
        SELECT * FROM ph_medicines 
        WHERE (brandName != '' AND UPPER(:scannedText) LIKE '%' || UPPER(brandName) || '%')
        OR (genericName != '' AND UPPER(:scannedText) LIKE '%' || UPPER(genericName) || '%')
        ORDER BY LENGTH(brandName) DESC
        LIMIT 1
    """)
    suspend fun findMatchingMedicine(scannedText: String): Medicine?

    @Query("SELECT COUNT(*) FROM ph_medicines")
    suspend fun getMedicineCount(): Int
}
