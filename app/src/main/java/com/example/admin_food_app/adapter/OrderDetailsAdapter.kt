package com.example.admin_food_app.adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.admin_food_app.databinding.OrderDetailItemBinding

class OrderDetailsAdapter(
    private var context: Context,
    private var foodNames: ArrayList<String>,
    private var foodImages: ArrayList<String>,
    private var foodQuantitys: ArrayList<Int>,
    private var foodPrices: ArrayList<String>
): RecyclerView.Adapter<OrderDetailsAdapter.OrderDetailsViwHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderDetailsViwHolder {
        val binding=OrderDetailItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderDetailsViwHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderDetailsViwHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = foodNames.size

    inner class OrderDetailsViwHolder(private val binding: OrderDetailItemBinding):RecyclerView.ViewHolder(binding.root) {
        fun bind(position: Int) {
            binding.apply{
                foodName.text=foodNames[position]
                foodQuantity.text=foodQuantitys[position].toString()
                val uriString=foodImages[position]
                val uri= Uri.parse(uriString)
                Glide.with(context).load(uri).into(foodImage)
                foodPrice.text=foodPrices[position]
            }
        }

    }
}