package com.app.medbox_wifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class MedicineAdapter(private val onClick: (LoggedMedicine) -> Unit) :
    ListAdapter<LoggedMedicine, MedicineAdapter.MedicineViewHolder>(MedicineDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MedicineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_medicine_log, parent, false)
        return MedicineViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: MedicineViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class MedicineViewHolder(itemView: View, val onClick: (LoggedMedicine) -> Unit) :
        RecyclerView.ViewHolder(itemView) {
        private val tvBrandName: TextView = itemView.findViewById(R.id.tvBrandName)
        private val tvGenericName: TextView = itemView.findViewById(R.id.tvGenericName)
        private val tvAvatarLetter: TextView = itemView.findViewById(R.id.tvAvatarLetter)
        private val tvIntakeTime: TextView = itemView.findViewById(R.id.tvIntakeTime)
        private val tvQuantity: TextView = itemView.findViewById(R.id.tvQuantity)
        private val tvExpiryDate: TextView = itemView.findViewById(R.id.tvExpiryDate)
        private val tvPillbox: TextView = itemView.findViewById(R.id.tvPillbox)
        private var currentMedicine: LoggedMedicine? = null

        init {
            itemView.setOnClickListener {
                currentMedicine?.let { onClick(it) }
            }
        }

        fun bind(medicine: LoggedMedicine) {
            currentMedicine = medicine
            tvBrandName.text = medicine.brandName
            tvGenericName.text = medicine.genericName
            tvAvatarLetter.text = medicine.brandName.take(1).uppercase()
            
            if (medicine.intakeTime.isNotEmpty()) {
                tvIntakeTime.text = medicine.intakeTime
                tvIntakeTime.visibility = View.VISIBLE
            } else {
                tvIntakeTime.visibility = View.GONE
            }

            tvQuantity.text = "Qty: ${medicine.quantity}"
            
            if (medicine.expiryDate > 0) {
                val sdf = SimpleDateFormat("MMM yyyy", Locale.getDefault())
                tvExpiryDate.text = "Exp: ${sdf.format(Date(medicine.expiryDate))}"
                tvExpiryDate.visibility = View.VISIBLE
            } else {
                tvExpiryDate.visibility = View.GONE
            }

            if (medicine.pillboxNumber > 0) {
                tvPillbox.text = "Box ${medicine.pillboxNumber}"
                tvPillbox.visibility = View.VISIBLE
            } else {
                tvPillbox.visibility = View.GONE
            }
        }
    }

    class MedicineDiffCallback : DiffUtil.ItemCallback<LoggedMedicine>() {
        override fun areItemsTheSame(oldItem: LoggedMedicine, newItem: LoggedMedicine): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LoggedMedicine, newItem: LoggedMedicine): Boolean {
            return oldItem == newItem
        }
    }
}
