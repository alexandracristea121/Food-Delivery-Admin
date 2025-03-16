package com.example.admin_food_app

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_food_app.databinding.ActivityMainBinding
import com.example.admin_food_app.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var database: FirebaseDatabase
    private lateinit var auth: FirebaseAuth
    private lateinit var completedOrderReference: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Navigation Buttons
        binding.profile.setOnClickListener {
            val intent = Intent(this, AdminProfileActivity::class.java)
            startActivity(intent)
        }
        binding.myRestaurants.setOnClickListener {
            val intent = Intent(this, MyRestaurantsActivity::class.java)
            startActivity(intent)
        }
        binding.createUser.setOnClickListener {
            val intent = Intent(this, CreateUserActivity::class.java)
            startActivity(intent)
        }
        binding.outForDeliveryButton.setOnClickListener {
            val intent = Intent(this, OutForDeliveryActivity::class.java)
            startActivity(intent)
        }
        binding.orderManagement.setOnClickListener {
            val intent = Intent(this, OrderManagementActivity::class.java)
            startActivity(intent)
        }
        binding.logout.setOnClickListener {
            auth.signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Adjust for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Real-time updates for user-specific data
        observePendingOrders()
        observeCompletedOrders()
        observeWholeTimeEarning()
    }

    private fun observePendingOrders() {
        val currentUserId = auth.currentUser?.uid ?: return
        val ordersReference: DatabaseReference = database.reference.child("orders")

        ordersReference.orderByChild("adminUserId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var pendingOrderCount = 0

                    for (orderSnapshot in snapshot.children) {
                        val orderDelivered = orderSnapshot.child("orderDelivered").getValue(Boolean::class.java) ?: false

                        if (!orderDelivered) {  // Count only orders that are NOT delivered
                            pendingOrderCount++
                        }
                    }

                    // Update UI with the pending order count
                    binding.pendingOrders.text = pendingOrderCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun observeCompletedOrders() {
        val currentUserId = auth.currentUser?.uid ?: return
        val ordersReference: DatabaseReference = database.reference.child("orders")

        ordersReference.orderByChild("adminUserId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var completedOrderCount = 0
                    for (orderSnapshot in snapshot.children) {
                        val orderDelivered = orderSnapshot.child("orderDelivered").getValue(Boolean::class.java) ?: false
                        if (orderDelivered) {
                            completedOrderCount++
                        }
                    }
                    binding.completeOrders.text = completedOrderCount.toString()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun observeWholeTimeEarning() {
        val currentUserId = auth.currentUser?.uid ?: return
        val ordersReference: DatabaseReference = database.reference.child("orders")

        ordersReference.orderByChild("adminUserId").equalTo(currentUserId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val listOfTotalPay = mutableListOf<Int>()
                    for (orderSnapshot in snapshot.children) {
                        val orderDelivered = orderSnapshot.child("orderDelivered").getValue(Boolean::class.java) ?: false

                        if (orderDelivered) {  // Only process delivered orders
                            val completeOrder = orderSnapshot.getValue(OrderDetails::class.java)

                            // Extract food prices and quantities
                            val foodPrices = completeOrder?.foodPrices
                            val foodQuantities = completeOrder?.foodQuantities

                            if (foodPrices != null && foodQuantities != null && foodPrices.size == foodQuantities.size) {
                                var totalPriceForOrder = 0
                                for (i in foodPrices.indices) {
                                    val price = foodPrices[i].toIntOrNull() ?: 0
                                    val quantity = foodQuantities[i]
                                    totalPriceForOrder += price * quantity
                                }
                                listOfTotalPay.add(totalPriceForOrder)
                            }
                        }
                    }

                    // Sum up all earnings and update the UI
                    val totalEarnings = listOfTotalPay.sum()
                    binding.wholeTimeEarning.text = "$$totalEarnings"
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@MainActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }
}