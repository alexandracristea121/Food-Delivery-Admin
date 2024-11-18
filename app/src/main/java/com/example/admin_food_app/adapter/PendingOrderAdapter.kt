package com.example.admin_food_app.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.admin_food_app.PendingOrderActivity
import com.example.admin_food_app.databinding.PendingOrderItemBinding
import java.util.ArrayList

class PendingOrderAdapter(private val customerNames:ArrayList<String>, private val quantity:ArrayList<String>, private val foodImage:ArrayList<Int>, private val context: Context) : RecyclerView.Adapter<PendingOrderAdapter.PendingOrderViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PendingOrderViewHolder {
        val binding = PendingOrderItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PendingOrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PendingOrderViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = customerNames.size

    inner class PendingOrderViewHolder(private val binding: PendingOrderItemBinding):RecyclerView.ViewHolder(binding.root) {
        private var isAccepted = false
        fun bind(position: Int) {
            binding.apply {
                customerName.text=customerNames[position]
                pendingOrderQuantity.text=quantity[position]
                orderFoodImage.setImageResource(foodImage[position])

                orderedAcceptButton.apply{
                    if(!isAccepted){
                        text= "Accept"
                    }else{
                        text = "Dispatch"
                    }

                    setOnClickListener {
                        if(!isAccepted){
                            text="Dispatch"
                            isAccepted=true
                            showToast("Order is accepted")
                        } else {
                            customerNames.removeAt(adapterPosition)
                            notifyItemRemoved(adapterPosition)
                            showToast("Order is dispatched")
                        }
                    }
                }
            }
        }
        private fun showToast(message: String){
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }
}