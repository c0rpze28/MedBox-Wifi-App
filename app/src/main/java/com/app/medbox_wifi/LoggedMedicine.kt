package com.app.medbox_wifi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "logged_medicines")
data class LoggedMedicine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brandName: String,
    val genericName: String,
    val dosage: String = "",
    val quantity: Int = 0,
    val expiryDate: Long = 0,
    val intakeTime: String = "",
    val remindersEnabled: Boolean = false,
    val notes: String = "",
    val pillboxNumber: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
