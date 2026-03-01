package com.app.medbox_wifi

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScannedText::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedTextDao(): ScannedTextDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medbox_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
