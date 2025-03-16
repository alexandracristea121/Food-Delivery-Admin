package com.example.admin_food_app

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_food_app.databinding.ActivityAddItemBinding
import com.example.admin_food_app.model.AllMenu
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AddItemActivity : AppCompatActivity() {

    private lateinit var foodName: String
    private lateinit var foodPrice: String
    private lateinit var foodDescription: String
    private lateinit var foodIngredient: String
    private var foodImage: Uri? = null
    private lateinit var selectedRestaurantId: String
    private lateinit var selectedCategory: String // For storing the selected category

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val binding: ActivityAddItemBinding by lazy {
        ActivityAddItemBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
        window.statusBarColor = ContextCompat.getColor(this, android.R.color.white)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Fetch restaurant list from Firebase and populate the spinner
        fetchRestaurantData()

        // Set up the category dropdown
        setupCategorySpinner()

        // Add item button click
        binding.AddItemButton.setOnClickListener {
            foodName = binding.foodName.text.toString().trim()
            foodPrice = binding.foodPrice.text.toString().trim()
            foodDescription = binding.description.text.toString().trim()
            foodIngredient = binding.ingredient.text.toString().trim()

            if (foodName.isNotBlank() && foodPrice.isNotBlank() && foodDescription.isNotBlank() && foodIngredient.isNotBlank()) {
                uploadData()
                Toast.makeText(this, "Item Added Successfully", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Please fill all the details", Toast.LENGTH_SHORT).show()
            }
        }

        // Image picker for selecting food image
        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Back button functionality
        binding.backButton.setOnClickListener {
            finish()
        }

        // Apply padding to avoid system bar overlap
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    // Function to upload data to Firebase
    private fun uploadData() {
        val restaurantRef = database.getReference("Restaurants").child(selectedRestaurantId).child("menu")
        val newItemKey = restaurantRef.push().key // This generates a unique ID for the new menu item

        if (foodImage != null) {
            // Upload image to Firebase Storage
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("menu_images/${newItemKey}.jpg")
            val uploadTask = imageRef.putFile(foodImage!!)

            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    // Create a new menu item object
                    val newItem = AllMenu(
                        newItemKey,
                        foodName = foodName,
                        foodPrice = foodPrice,
                        foodDescription = foodDescription,
                        foodIngredient = foodIngredient,
                        foodImage = downloadUrl.toString(),
                        restaurantName = selectedRestaurantId, // Store selected restaurant ID
                        category = selectedCategory // Store selected category
                    )

                    // Add new item to the restaurant's menu
                    newItemKey?.let { key ->
                        restaurantRef.child(key).setValue(newItem).addOnSuccessListener {
                            Toast.makeText(this, "Data uploaded successfully", Toast.LENGTH_SHORT).show()
                        }
                            .addOnFailureListener {
                                Toast.makeText(this, "Data upload failed", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show()
            }
        } else {
            // If image not selected
            Toast.makeText(this, "Please select an image", Toast.LENGTH_SHORT).show()
        }
    }

    // Image selection function
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            binding.selectedImage.setImageURI(uri)
            foodImage = uri
        }
    }

    // Function to fetch restaurant data and populate the spinner
    private fun fetchRestaurantData() {
        val restaurantRef = database.getReference("Restaurants")

        // Get the current logged-in user's UID
        val currentUserId = auth.currentUser?.uid ?: ""

        restaurantRef.get().addOnSuccessListener { snapshot ->
            val restaurantList = mutableListOf<String>()
            val restaurantIds = mutableListOf<String>() // Store restaurant IDs

            for (restaurantSnapshot in snapshot.children) {
                val restaurantName = restaurantSnapshot.child("name").getValue(String::class.java)
                val restaurantId = restaurantSnapshot.key // Get the restaurant ID
                val adminUserId = restaurantSnapshot.child("adminUserId").getValue(String::class.java)

                // Check if the current logged-in user's UID matches the adminUserId of the restaurant
                if (restaurantName != null && restaurantId != null && adminUserId != null && adminUserId == currentUserId) {
                    restaurantList.add(restaurantName)
                    restaurantIds.add(restaurantId)
                }
            }

            // If no restaurants are found for the current user, show a message
            if (restaurantList.isEmpty()) {
                Toast.makeText(this, "No restaurants found for this user.", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            // Set up the spinner with the restaurant list
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, restaurantList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.restaurantSpinner.adapter = adapter

            // Handle restaurant selection
            binding.restaurantSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                    selectedRestaurantId = restaurantIds[position] // Set selected restaurant ID
                }

                override fun onNothingSelected(parentView: AdapterView<*>) {
                    // Handle case where no restaurant is selected
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load restaurants", Toast.LENGTH_SHORT).show()
        }
    }

    // Function to set up the category spinner
    private fun setupCategorySpinner() {
        // Predefined categories for food items
        val categories = listOf(
            "Pizza", "Pasta", "Burgers", "Sandwiches", "Salads",
            "Sushi", "Mexican", "Asian", "Desserts", "Drinks",
            "Soups", "Snacks", "Fast Food", "Breakfast",
            "Grilled Food", "Vegan", "Vegetarian", "Seafood", "BBQ"
        )

        // Set up an adapter for the category spinner
        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.categorySpinner.adapter = categoryAdapter

        // Handle category selection
        binding.categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                selectedCategory = categories[position] // Store selected category
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
            }
        }
    }
}