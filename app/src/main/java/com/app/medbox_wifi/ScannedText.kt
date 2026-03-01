package com.app.medbox_wifi

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_texts")
data class ScannedText(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
