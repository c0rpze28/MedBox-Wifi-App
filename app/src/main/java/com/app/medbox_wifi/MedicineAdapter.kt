package com.app.medbox_wifi

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

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
            
            // Set the avatar letter based on the first character of the brand name
            tvAvatarLetter.text = medicine.brandName.take(1).uppercase()
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
