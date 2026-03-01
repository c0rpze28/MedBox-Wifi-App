package com.app.medbox_wifi

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ScannedText::class, Medicine::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedTextDao(): ScannedTextDao
    abstract fun medicineDao(): MedicineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "medbox_database"
                )
                .addCallback(AppDatabaseCallback(scope))
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.medicineDao())
                    }
                }
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                // Always check if we need to repopulate on open if version changed
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        if (database.medicineDao().getMedicineCount() == 0) {
                            populateDatabase(database.medicineDao())
                        }
                    }
                }
            }

            suspend fun populateDatabase(medicineDao: MedicineDao) {
                val medicines = listOf(
                    Medicine(brandName = "Biogesic", genericName = "Paracetamol", description = "For fever and pain relief"),
                    Medicine(brandName = "Neozep", genericName = "Phenylephrine HCl / Chlorphenamine Maleate / Paracetamol", description = "For relief of clogged nose, runny nose, postnasal drip, itchy and watery eyes"),
                    Medicine(brandName = "Bioflu", genericName = "Phenylephrine HCl / Chlorphenamine Maleate / Paracetamol", description = "For relief of flu symptoms like fever, body aches, and clogged nose"),
                    Medicine(brandName = "Alaxan", genericName = "Ibuprofen + Paracetamol", description = "For relief of body aches and pains"),
                    Medicine(brandName = "Ascof", genericName = "Lagundi (Vitex negundo L.)", description = "Herbal medicine for cough relief"),
                    Medicine(brandName = "Solmux", genericName = "Carbocisteine", description = "For cough with phlegm"),
                    Medicine(brandName = "Tempra", genericName = "Paracetamol", description = "Fever and pain relief for children"),
                    Medicine(brandName = "Enervon", genericName = "Vitamin B-Complex + Vitamin C", description = "Nutritional supplement for energy and immunity"),
                    Medicine(brandName = "Potencee", genericName = "Vitamin C", description = "Immunity booster"),
                    Medicine(brandName = "Medicol", genericName = "Ibuprofen", description = "For pain relief and inflammation"),
                    Medicine(brandName = "Diatabs", genericName = "Loperamide", description = "Used for the control and symptomatic relief of acute non-specific diarrhea and chronic diarrhea")
                )
                medicineDao.insertAll(medicines)
            }
        }
    }
}
