package com.app.medbox_wifi

import androidx.room.*

@Dao
interface LoggedMedicineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(loggedMedicine: LoggedMedicine)

    @Update
    suspend fun update(loggedMedicine: LoggedMedicine)

    @Delete
    suspend fun delete(loggedMedicine: LoggedMedicine)

    @Query("SELECT * FROM logged_medicines ORDER BY timestamp DESC LIMIT 6")
    suspend fun getRecentLogs(): List<LoggedMedicine>

    @Query("SELECT * FROM logged_medicines WHERE id = :id")
    suspend fun getById(id: Int): LoggedMedicine?

    @Query("SELECT * FROM logged_medicines WHERE UPPER(brandName) = UPPER(:brandName) LIMIT 1")
    suspend fun getByBrandName(brandName: String): LoggedMedicine?
}
