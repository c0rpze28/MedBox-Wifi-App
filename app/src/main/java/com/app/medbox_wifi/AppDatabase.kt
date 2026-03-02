package com.app.medbox_wifi

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [ScannedText::class, Medicine::class, LoggedMedicine::class], version = 14)
abstract class AppDatabase : RoomDatabase() {
    abstract fun scannedTextDao(): ScannedTextDao
    abstract fun medicineDao(): MedicineDao
    abstract fun loggedMedicineDao(): LoggedMedicineDao

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
                    Medicine(brandName = "Neozep Forte", genericName = "Phenylephrine HCl / Chlorphenamine Maleate / Paracetamol", description = "For relief of flu symptoms and clogged nose"),
                    Medicine(brandName = "Neozep", genericName = "Phenylephrine HCl / Chlorphenamine Maleate / Paracetamol", description = "For relief of flu symptoms and clogged nose"),
                    Medicine(brandName = "Biogesic", genericName = "Paracetamol", description = "For fever and pain relief"),
                    Medicine(brandName = "Bioflu", genericName = "Phenylephrine HCl / Chlorphenamine Maleate / Paracetamol", description = "For relief of flu symptoms"),
                    Medicine(brandName = "Alaxan FR", genericName = "Ibuprofen + Paracetamol", description = "For relief of body aches and pains"),
                    Medicine(brandName = "Alaxan", genericName = "Ibuprofen + Paracetamol", description = "For relief of body aches and pains"),
                    Medicine(brandName = "Ascof", genericName = "Lagundi", description = "Herbal medicine for cough relief"),
                    Medicine(brandName = "Solmux", genericName = "Carbocisteine", description = "For cough with phlegm"),
                    Medicine(brandName = "Tempra", genericName = "Paracetamol", description = "Fever and pain relief for children"),
                    Medicine(brandName = "Enervon", genericName = "Vitamin B-Complex + Vitamin C", description = "Nutritional supplement"),
                    Medicine(brandName = "Potencee", genericName = "Vitamin C", description = "Immunity booster"),
                    Medicine(brandName = "Medicol", genericName = "Ibuprofen", description = "For pain relief"),
                    Medicine(brandName = "Diatabs", genericName = "Loperamide", description = "For diarrhea relief")
                )
                medicineDao.insertAll(medicines)
            }
        }
    }
}
