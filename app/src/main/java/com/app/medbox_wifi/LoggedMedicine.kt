package com.app.medbox_wifi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logged_medicines")
data class LoggedMedicine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brandName: String,
    val genericName: String,
    val dosage: String = "",
    val quantity: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
