package com.example.admin_food_app

import LocationUtil
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.admin_food_app.adapter.PendingOrderAdapter
import com.example.admin_food_app.databinding.ActivityPendingOrderBinding
import com.example.admin_food_app.model.OrderDetails
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class OrderManagementActivity : AppCompatActivity(), PendingOrderAdapter.OnItemClicked {

    private lateinit var binding: ActivityPendingOrderBinding
    private var listOfName: MutableList<String> = mutableListOf()
    private var listOfTotalPrice: MutableList<String> = mutableListOf()
    private var listOfImageFirstFoodOrder: MutableList<String> = mutableListOf()
    private var listOfOrderItem: ArrayList<OrderDetails> = arrayListOf()
    private lateinit var database: FirebaseDatabase
    private lateinit var databaseOrderDetails: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var currentUserId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityPendingOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
        databaseOrderDetails = database.reference.child("orders")

        // Initialize Firebase Authentication
        auth = FirebaseAuth.getInstance()
        currentUserId = auth.currentUser?.uid ?: ""

        // Fetch order details
        getOrderDetails()

        // Back button
        binding.backButton.setOnClickListener {
            finish()
        }

        // Ensure UI adjusts for system bars
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun getOrderDetails() {
        databaseOrderDetails.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                listOfOrderItem.clear() // Clear the list before adding new data

                for (orderSnapshot in snapshot.children) {
                    val orderDetails = orderSnapshot.getValue(OrderDetails::class.java)
                    orderDetails?.let {
                        if (it.adminUserId == currentUserId && it.orderDelivered == false) {
                            // Only add orders that are not delivered (not dispatched)
                            listOfOrderItem.add(it)
                        }
                    }
                }
                addDataToListForRecyclerView()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@OrderManagementActivity, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addDataToListForRecyclerView() {
        // Populate lists with the order data
        for (orderItem in listOfOrderItem) {
            orderItem.userName?.let { listOfName.add(it) }
            orderItem.totalPrice?.let { listOfTotalPrice.add(it) }
            orderItem.foodImages?.filterNot { it.isEmpty() }?.forEach {
                listOfImageFirstFoodOrder.add(it)
            }
        }
        setAdapter()
    }

    private fun setAdapter() {
        binding.pendingOrderRecyclerView.layoutManager = LinearLayoutManager(this)
        val adapter = PendingOrderAdapter(this, listOfName, listOfTotalPrice, listOfImageFirstFoodOrder, this)
        binding.pendingOrderRecyclerView.adapter = adapter
    }

    override fun onItemClickListener(position: Int) {
        val intent = Intent(this, OrderDetailsActivity::class.java)
        val userOrderDetails = listOfOrderItem[position]
        intent.putExtra("UserOrderDetails", userOrderDetails)
        startActivity(intent)
    }

    // Your existing onItemAcceptClickListener function
    override fun onItemAcceptClickListener(position: Int) {
        val childItemPushKey = listOfOrderItem[position].itemPushkey ?: return

        // Fetch and update order status
        updateOrderAcceptedStatus(childItemPushKey, position) { success ->
            if (success) {
                fetchOrderDetails(childItemPushKey) { order ->
                    if (order != null) {
                        processOrderForNearestCourier(order, childItemPushKey)
                    } else {
                        Log.d("CourierLog", "Invalid order data or missing locations.")
                        showToast("Invalid order data or missing locations", 10000)
                    }
                }
            } else {
                Toast.makeText(this, "Failed to accept order", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Updates the order's acceptance status in Firebase */
    private fun updateOrderAcceptedStatus(orderId: String, position: Int, callback: (Boolean) -> Unit) {
        val orderRef = database.reference.child("orders").child(orderId)

        orderRef.child("orderAccepted").setValue(true)
            .addOnSuccessListener {
                updateOrderAcceptStatus(position)
                callback(true)
            }
            .addOnFailureListener { error ->
                Log.e("CourierLog", "Error accepting order: ${error.message}")
                callback(false)
            }
    }

    /** Fetches order details from Firebase */
    private fun fetchOrderDetails(orderId: String, callback: (OrderDetails?) -> Unit) {
        val orderRef = database.reference.child("orders").child(orderId)

        orderRef.get().addOnSuccessListener { snapshot ->
            val order = snapshot.getValue(OrderDetails::class.java)
            callback(order)
        }.addOnFailureListener {
            Log.e("CourierLog", "Error fetching order details: ${it.message}")
            callback(null)
        }
    }

    private fun processOrderForNearestCourier(order: OrderDetails, orderId: String) {
        val locationUtil = LocationUtil()

        // Get LatLng from the restaurant and user locations
        val restaurantLatLng = locationUtil.getLatLngFromAddress(this, order.restaurantLocation.toString())
        val userLatLng = locationUtil.getLatLngFromAddress(this, order.userLocation.toString())

        if (restaurantLatLng != null && userLatLng != null) {
            // Fetch all couriers from the database
            val couriersRef = database.reference.child("couriers")
            couriersRef.get().addOnSuccessListener { snapshot ->
                val couriers = mutableListOf<String>()

                // Collect all courier IDs from the database
                for (courierSnapshot in snapshot.children) {
                    val courierId = courierSnapshot.key
                    val status = courierSnapshot.child("status").getValue(String::class.java)
                    if (courierId != null && status == "AVAILABLE") {
                        couriers.add(courierId)
                    }
                }

                // If no available couriers, show an error
                if (couriers.isEmpty()) {
                    Log.d("CourierLog", "No available couriers.")
                    showToast("No available couriers", 5000)
                    return@addOnSuccessListener
                }

                // Sort couriers based on their distance from the restaurant
                couriers.sortBy { courierId ->
                    val courierLatLng = locationUtil.getLatLngFromAddress(applicationContext, order.restaurantLocation.toString())
                    locationUtil.calculateDistance(restaurantLatLng, courierLatLng)
                }

                // Try to find the nearest available courier
                for (courierId in couriers) {
                    val courierRef = database.reference.child("couriers").child(courierId)
                    courierRef.get().addOnSuccessListener { courierSnapshot ->
                        val status = courierSnapshot.child("status").getValue(String::class.java)

                        if (status == "AVAILABLE") {
                            Log.d("CourierLog", "Nearest AVAILABLE Courier Found: $courierId")

                            // Update the courier's status to "DELIVERING"
                            locationUtil.updateCourierStatusAndAddLocations(courierId, restaurantLatLng, userLatLng, "DELIVERING")

                            // Add the userUid and orderId to the courier
                            database.reference.child("couriers").child(courierId).child("orderId").setValue(orderId)
                            database.reference.child("couriers").child(courierId).child("userUid").setValue(order.userUid)

                            // Show a success message to the user
                            showToast("Nearest AVAILABLE Courier found: $courierId", 5000)
                            return@addOnSuccessListener
                        }
                    }
                }

                // If no available courier is found (shouldn't reach here if available couriers exist)
                Log.d("CourierLog", "No available couriers found after checking all.")
                showToast("No available couriers found after checking all.", 5000)
            }.addOnFailureListener {
                Log.d("CourierLog", "Error fetching couriers.")
                showToast("Error fetching couriers.", 5000)
            }
        } else {
            Log.d("CourierLog", "Error geocoding locations.")
            showToast("Error geocoding locations", 5000)
        }
    }

    // Custom Toast function to display for 10 seconds
    fun showToast(message: String, duration: Long) {
        val toast = Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT)
        toast.show()

        // Use Handler to show the Toast for 10 seconds
        android.os.Handler().postDelayed({
            toast.cancel()
        }, duration)
    }

    override fun onItemDispatchClickListener(position: Int) {
        val dispatchItemPushKey = listOfOrderItem[position].itemPushkey

        if (dispatchItemPushKey != null) {
            val orderReference = database.reference.child("orders").child(dispatchItemPushKey)
            val couriersReference = database.reference.child("couriers")

            // Find the courier with this orderId
            couriersReference.orderByChild("orderId").equalTo(dispatchItemPushKey)
                .get()
                .addOnSuccessListener { snapshot ->
                    if (snapshot.exists()) {
                        for (courierSnapshot in snapshot.children) {
                            val courierId = courierSnapshot.key
                            val userLatitude = courierSnapshot.child("userLatitude").getValue(Double::class.java)
                            val userLongitude = courierSnapshot.child("userLongitude").getValue(Double::class.java)

                            if (courierId != null && userLatitude != null && userLongitude != null) {
                                val courierReference = couriersReference.child(courierId)

                                // ✅ First, mark the order as delivered
                                orderReference.updateChildren(mapOf("orderDelivered" to true))
                                    .addOnSuccessListener {
                                        Toast.makeText(this, "Order marked as delivered", Toast.LENGTH_SHORT).show()

                                        // ✅ Then, reset the courier data
                                        val courierUpdates = mapOf(
                                            "latitude" to userLatitude,
                                            "longitude" to userLongitude,
                                            "restaurantLatitude" to 0.0,
                                            "restaurantLongitude" to 0.0,
                                            "orderId" to "",
                                            "userLatitude" to 0.0,
                                            "userLongitude" to 0.0,
                                            "userUid" to "",
                                            "status" to "AVAILABLE",
                                            "lastUpdate" to System.currentTimeMillis()
                                        )

                                        courierReference.updateChildren(courierUpdates)
                                            .addOnSuccessListener {
                                                Toast.makeText(this, "Courier reset to AVAILABLE", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener { e ->
                                                Toast.makeText(this, "Failed to reset courier: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }

                                    }
                                    .addOnFailureListener {
                                        Toast.makeText(this, "Failed to update order status", Toast.LENGTH_SHORT).show()
                                    }
                            } else {
                                Toast.makeText(this, "Courier data is incomplete", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(this, "No courier found for this order", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.d("CourierLog", "Error finding courier: ${e.message}")
                }
        } else {
            Toast.makeText(this, "Invalid order key", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateOrderAcceptStatus(position: Int) {
        // Update order acceptance in user's buyhistory and orderdetails
        val userIdOfClickedItem = listOfOrderItem[position].userUid
        val pushKeyOfClickedItem = listOfOrderItem[position].itemPushkey
        val buyHistoryReference = database.reference.child("adminUsers").child(userIdOfClickedItem!!).child("BuyHistory").child(pushKeyOfClickedItem!!)
        buyHistoryReference.child("orderAccepted").setValue(true)
        databaseOrderDetails.child(pushKeyOfClickedItem).child("orderAccepted").setValue(true)
    }
}