package com.app.medbox_wifi

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MedicineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medicines: List<Medicine>)

    @Query("""
        SELECT *, 
        (CASE 
            WHEN (brandName != '' AND (UPPER(:scannedText) LIKE '%' || UPPER(brandName) || '%' OR UPPER(brandName) LIKE '%' || UPPER(:scannedText) || '%')) THEN 1000 + LENGTH(brandName)
            WHEN (genericName != '' AND (UPPER(:scannedText) LIKE '%' || UPPER(genericName) || '%' OR UPPER(genericName) LIKE '%' || UPPER(:scannedText) || '%')) THEN 100 + LENGTH(genericName)
            ELSE 0 
        END) as matchScore
        FROM ph_medicines 
        WHERE matchScore > 0
        ORDER BY matchScore DESC
        LIMIT 1
    """)
    suspend fun findMatchingMedicine(scannedText: String): Medicine?

    @Query("SELECT * FROM ph_medicines")
    suspend fun getAllMedicines(): List<Medicine>

    @Query("SELECT COUNT(*) FROM ph_medicines")
    suspend fun getMedicineCount(): Int
}
