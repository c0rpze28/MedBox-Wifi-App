package com.app.medbox_wifi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ph_medicines")
data class Medicine(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val brandName: String,
    val genericName: String,
    val description: String? = null
)
