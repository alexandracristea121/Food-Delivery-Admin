package com.example.admin_food_app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.admin_food_app.adapter.DeliveryAdapter
import com.example.admin_food_app.adapter.PendingOrderAdapter
import com.example.admin_food_app.databinding.ActivityPendingOrderBinding
import com.example.admin_food_app.databinding.PendingOrderItemBinding

class PendingOrderActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPendingOrderBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPendingOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.backButton.setOnClickListener {
            finish()
        }
        val orderedCustomerName= arrayListOf("Alex Scrofan", "Denisa Dobra", "Alexandra Cristea")
        val orderedQuantity= arrayListOf("8", "6", "5")
        val orderedFoodImage = arrayListOf(R.drawable.menu2, R.drawable.menu3, R.drawable.menu2)
        val adapter= PendingOrderAdapter(orderedCustomerName, orderedQuantity, orderedFoodImage, this)
        binding.pendingOrderRecyclerView.adapter=adapter
        binding.pendingOrderRecyclerView.layoutManager= LinearLayoutManager(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}