package com.app.medbox_wifi

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var database: AppDatabase
    private lateinit var adapter: MedicineAdapter
    private lateinit var rvRecentLogs: RecyclerView
    private lateinit var tvEmptyState: TextView

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val notifGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: false
        if (!notifGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(this, "Notifications are disabled. You won't receive medicine reminders.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        checkPermissions()

        database = AppDatabase.getDatabase(this, lifecycleScope)
        
        rvRecentLogs = findViewById(R.id.rvRecentLogs)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        val btnScan = findViewById<Button>(R.id.btnScan)

        adapter = MedicineAdapter { loggedMed ->
            val intent = Intent(this, EditMedicineActivity::class.java)
            intent.putExtra("MEDICINE_ID", loggedMed.id)
            startActivity(intent)
        }

        rvRecentLogs.layoutManager = LinearLayoutManager(this)
        rvRecentLogs.adapter = adapter

        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val medicineToDelete = adapter.currentList[position]
                
                lifecycleScope.launch(Dispatchers.IO) {
                    database.loggedMedicineDao().delete(medicineToDelete)
                    withContext(Dispatchers.Main) {
                        loadRecentLogs()
                        Toast.makeText(this@MainActivity, "${medicineToDelete.brandName} removed", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                val itemView = viewHolder.itemView
                val itemHeight = itemView.bottom - itemView.top
                
                val paint = Paint().apply { color = Color.parseColor("#FF3B30") }
                val background = RectF(itemView.left.toFloat(), itemView.top.toFloat(), itemView.right.toFloat(), itemView.bottom.toFloat())
                c.drawRoundRect(background, 16f * resources.displayMetrics.density, 16f * resources.displayMetrics.density, paint)

                val icon = ContextCompat.getDrawable(this@MainActivity, android.R.drawable.ic_menu_delete)
                icon?.let {
                    val iconMargin = (itemHeight - it.intrinsicHeight) / 2
                    val iconTop = itemView.top + (itemHeight - it.intrinsicHeight) / 2
                    val iconBottom = iconTop + it.intrinsicHeight
                    
                    if (dX > 0) {
                        val iconLeft = itemView.left + iconMargin
                        val iconRight = iconLeft + it.intrinsicWidth
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    } else if (dX < 0) {
                        val iconRight = itemView.right - iconMargin
                        val iconLeft = iconRight - it.intrinsicWidth
                        it.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    }
                    it.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }

        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(rvRecentLogs)

        btnScan.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        loadRecentLogs()
    }

    private fun loadRecentLogs() {
        lifecycleScope.launch {
            val logs = database.loggedMedicineDao().getRecentLogs()
            if (logs.isEmpty()) {
                tvEmptyState.visibility = View.VISIBLE
                rvRecentLogs.visibility = View.GONE
            } else {
                tvEmptyState.visibility = View.GONE
                rvRecentLogs.visibility = View.VISIBLE
                adapter.submitList(logs)
            }
        }
    }
}
