package com.app.medbox_wifi

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class EditMedicineActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    private var medicineId: Int = -1
    private var currentMedicine: LoggedMedicine? = null

    private lateinit var etQuantity: TextInputEditText
    private lateinit var etDosage: TextInputEditText
    private lateinit var etExpiryDate: TextInputEditText
    private lateinit var etIntakeTime: TextInputEditText
    private lateinit var etNotes: TextInputEditText
    private lateinit var switchReminder: MaterialSwitch

    private val calendar = Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_medicine)

        database = AppDatabase.getDatabase(this, lifecycleScope)
        medicineId = intent.getIntExtra("MEDICINE_ID", -1)

        setupViews()
        loadMedicineData()
    }

    private fun setupViews() {
        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        etQuantity = findViewById(R.id.etQuantity)
        etDosage = findViewById(R.id.etDosage)
        etExpiryDate = findViewById(R.id.etExpiryDate)
        etIntakeTime = findViewById(R.id.etIntakeTime)
        etNotes = findViewById(R.id.etNotes)
        switchReminder = findViewById(R.id.switchReminder)

        etExpiryDate.setOnClickListener { showDatePicker() }
        etIntakeTime.setOnClickListener { showTimePicker() }

        findViewById<android.widget.Button>(R.id.btnSave).setOnClickListener {
            saveChanges()
        }
    }

    private fun loadMedicineData() {
        if (medicineId == -1) return

        lifecycleScope.launch {
            currentMedicine = database.loggedMedicineDao().getById(medicineId)
            currentMedicine?.let { med ->
                findViewById<TextView>(R.id.tvBrandName).text = med.brandName
                findViewById<TextView>(R.id.tvGenericName).text = med.genericName
                etQuantity.setText(med.quantity.toString())
                etDosage.setText(med.dosage)
                etNotes.setText(med.notes)
                switchReminder.isChecked = med.remindersEnabled

                if (med.expiryDate > 0) {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    etExpiryDate.setText(sdf.format(Date(med.expiryDate)))
                }

                etIntakeTime.setText(med.intakeTime)
            }
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                etExpiryDate.setText(sdf.format(calendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showTimePicker() {
        TimePickerDialog(
            this,
            { _, hourOfDay, minute ->
                val time = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
                etIntakeTime.setText(time)
            },
            calendar.get(Calendar.HOUR_OF_DAY),
            calendar.get(Calendar.MINUTE),
            true
        ).show()
    }

    private fun saveChanges() {
        val med = currentMedicine ?: return
        
        val updatedMed = med.copy(
            quantity = etQuantity.text.toString().toIntOrNull() ?: 0,
            dosage = etDosage.text.toString(),
            expiryDate = if (etExpiryDate.text.isNullOrEmpty()) 0 else calendar.timeInMillis,
            intakeTime = etIntakeTime.text.toString(),
            remindersEnabled = switchReminder.isChecked,
            notes = etNotes.text.toString()
        )

        lifecycleScope.launch(Dispatchers.IO) {
            database.loggedMedicineDao().update(updatedMed)
            withContext(Dispatchers.Main) {
                Toast.makeText(this@EditMedicineActivity, "Details Updated", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }
}
