package com.palace.driverapp.adapter

import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.palace.driverapp.R
import com.palace.driverapp.network.models.Bus

class BusAdapter(
    private var buses: List<Bus>,
    private val onBusSelected: (Bus) -> Unit
) : RecyclerView.Adapter<BusAdapter.BusViewHolder>() {

    private var selectedPosition: Int = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bus, parent, false)
        return BusViewHolder(view)
    }

    override fun onBindViewHolder(holder: BusViewHolder, position: Int) {
        holder.bind(buses[position], position == selectedPosition) {
            val previousPosition = selectedPosition
            selectedPosition = position

            notifyItemChanged(previousPosition)
            notifyItemChanged(position)

            onBusSelected(buses[position])
        }
    }

    override fun getItemCount() = buses.size

    fun updateBuses(newBuses: List<Bus>) {
        buses = newBuses
        selectedPosition = -1
        notifyDataSetChanged()
    }

    inner class BusViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cvBus: CardView = itemView.findViewById(R.id.cvBus)
        private val tvBusNumber: TextView = itemView.findViewById(R.id.tvBusNumber)
        private val tvBusPlate: TextView = itemView.findViewById(R.id.tvBusPlate)
        private val tvBusModel: TextView = itemView.findViewById(R.id.tvBusModel)
        private val tvBusCapacity: TextView = itemView.findViewById(R.id.tvBusCapacity)
        private val ivCheckmark: ImageView = itemView.findViewById(R.id.ivCheckmark)
        private val llDetails: LinearLayout = itemView.findViewById(R.id.llDetails)

        fun bind(bus: Bus, isSelected: Boolean, onClick: () -> Unit) {
            tvBusNumber.text = "Unidad #${bus.number}"
            tvBusPlate.text = "Placa: ${bus.plate}"
            tvBusModel.text = "Modelo: ${bus.model}"
            tvBusCapacity.text = "Capacidad: ${bus.capacity} pasajeros"

            cvBus.setOnClickListener {
                animateClick()
                onClick()
            }

            updateSelectionState(isSelected)
        }

        private fun updateSelectionState(isSelected: Boolean) {
            if (isSelected) {
                cvBus.setCardBackgroundColor(itemView.context.getColor(R.color.primary_light))
                tvBusNumber.setTextColor(itemView.context.getColor(R.color.white))
                tvBusPlate.setTextColor(itemView.context.getColor(R.color.white))
                tvBusModel.setTextColor(itemView.context.getColor(R.color.white))
                tvBusCapacity.setTextColor(itemView.context.getColor(R.color.white))

                ivCheckmark.visibility = View.VISIBLE
                animateCheckmark()
            } else {
                cvBus.setCardBackgroundColor(itemView.context.getColor(R.color.white))
                tvBusNumber.setTextColor(itemView.context.getColor(R.color.text_primary))
                tvBusPlate.setTextColor(itemView.context.getColor(R.color.text_secondary))
                tvBusModel.setTextColor(itemView.context.getColor(R.color.text_secondary))
                tvBusCapacity.setTextColor(itemView.context.getColor(R.color.text_secondary))

                ivCheckmark.visibility = View.GONE
            }
        }

        private fun animateCheckmark() {
            ivCheckmark.scaleX = 0f
            ivCheckmark.scaleY = 0f

            val scaleX = ObjectAnimator.ofFloat(ivCheckmark, "scaleX", 0f, 1.2f, 1f)
            val scaleY = ObjectAnimator.ofFloat(ivCheckmark, "scaleY", 0f, 1.2f, 1f)

            scaleX.duration = 400
            scaleY.duration = 400
            scaleX.interpolator = OvershootInterpolator()
            scaleY.interpolator = OvershootInterpolator()

            scaleX.start()
            scaleY.start()
        }

        private fun animateClick() {
            val scaleDown = ObjectAnimator.ofFloat(cvBus, "scaleX", 1f, 0.95f, 1f)
            val scaleDownY = ObjectAnimator.ofFloat(cvBus, "scaleY", 1f, 0.95f, 1f)

            scaleDown.duration = 150
            scaleDownY.duration = 150

            scaleDown.start()
            scaleDownY.start()
        }
    }
}