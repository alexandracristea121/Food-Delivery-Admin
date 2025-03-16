package com.example.admin_food_app

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import android.widget.AdapterView
import androidx.appcompat.app.AppCompatActivity
import com.example.admin_food_app.databinding.ManageExistingRestaurantsBinding
import com.google.android.gms.maps.model.LatLng
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest

import com.google.firebase.auth.FirebaseAuth

class ManageExistingRestaurantsActivity : AppCompatActivity() {

    private lateinit var binding: ManageExistingRestaurantsBinding
    private lateinit var placesClient: PlacesClient
    private lateinit var database: FirebaseDatabase
    private var isAddressPrefilled = false // Flag to track if address is pre-filled from DB
    private lateinit var auth: FirebaseAuth // Firebase Authentication instance

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ManageExistingRestaurantsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase Authentication and Database reference
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "AIzaSyCHP4wYR_Qe0d1sasOGQ-vlmCncYK2F4KQ") // Use the provided API key
        }
        placesClient = Places.createClient(this)

        // Fetch the current admin's UID and then fetch associated restaurant data
        fetchCurrentAdminUserIdAndFetchRestaurantData()

        // Add TextWatcher for address autocomplete
        binding.restaurantAddressEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val input = s.toString()

                // Only show suggestions if the address is not pre-filled
                if (!isAddressPrefilled && input.isNotEmpty()) {
                    fetchAddressSuggestions(input)
                } else {
                    binding.suggestionsContainer.visibility = View.GONE
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        // Back button functionality
        binding.backButton.setOnClickListener {
            finish() // Close the activity and return to the previous screen
        }

        // Handle item selection in Spinner
        binding.restaurantSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                val selectedRestaurant = binding.restaurantSpinner.selectedItem.toString()
                // Fetch data based on selection and populate the name and address fields
                fetchRestaurantDetails(selectedRestaurant)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Handle case where nothing is selected
            }
        }

        // Save restaurant button functionality
        binding.saveRestaurantButton.setOnClickListener {
            val selectedRestaurantName = binding.restaurantSpinner.selectedItem.toString()
            val updatedRestaurantName = binding.restaurantNameEditText.text.toString() // Get updated name
            val updatedAddress = binding.restaurantAddressEditText.text.toString()

            if (updatedAddress.isNotEmpty()) {
                geocodeAddress(updatedAddress) { latLng ->
                    if (latLng != null) {
                        // Update both the name and address in the database
                        updateRestaurantDetailsInDatabase(selectedRestaurantName, updatedRestaurantName, updatedAddress, latLng)
                    } else {
                        Toast.makeText(this, "Error fetching location for the address", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please fill in the address", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Fetch the current admin's UID and then fetch associated restaurant data
    private fun fetchCurrentAdminUserIdAndFetchRestaurantData() {
        val currentUser = auth.currentUser // Get the current logged-in user
        if (currentUser != null) {
            val adminUserId = currentUser.uid // Get the current admin's UID

            // Use this adminUserId to fetch restaurants associated with this admin
            fetchRestaurantData(adminUserId)
        } else {
            Toast.makeText(this, "No logged-in user found", Toast.LENGTH_SHORT).show()
        }
    }

    // Fetch restaurant list from Firebase for the current admin
    private fun fetchRestaurantData(adminUserId: String) {
        database.reference.child("Restaurants")
            .orderByChild("adminUserId").equalTo(adminUserId) // Filter by adminUserId
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val restaurantList = mutableListOf<String>()

                    // Check if the snapshot contains data and filter accordingly
                    if (snapshot.exists()) {
                        for (restaurantSnapshot in snapshot.children) {
                            val restaurantName = restaurantSnapshot.child("name").getValue(String::class.java)
                            // Only add restaurants that are associated with the adminUserId
                            if (restaurantName != null) {
                                restaurantList.add(restaurantName)
                            }
                        }

                        if (restaurantList.isEmpty()) {
                            Toast.makeText(this@ManageExistingRestaurantsActivity, "No restaurants found for this admin", Toast.LENGTH_SHORT).show()
                        } else {
                            // Set up the adapter for the Spinner
                            val adapter = ArrayAdapter(this@ManageExistingRestaurantsActivity, android.R.layout.simple_spinner_item, restaurantList)
                            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                            binding.restaurantSpinner.adapter = adapter
                        }
                    } else {
                        // If no data exists for the adminUserId, show a message
                        Toast.makeText(this@ManageExistingRestaurantsActivity, "No restaurants found for this admin", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ManageExistingRestaurantsActivity, "Failed to load restaurants", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Fetch details of a specific restaurant when selected from the Spinner
    private fun fetchRestaurantDetails(restaurantName: String) {
        database.reference.child("Restaurants")
            .orderByChild("name").equalTo(restaurantName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (restaurantSnapshot in snapshot.children) {
                        val restaurantAddress = restaurantSnapshot.child("address").getValue(String::class.java)
                        val restaurantName = restaurantSnapshot.child("name").getValue(String::class.java)
                        if (restaurantAddress != null && restaurantName != null) {
                            binding.restaurantAddressEditText.setText(restaurantAddress)
                            binding.restaurantNameEditText.setText(restaurantName) // Populate the name field
                            isAddressPrefilled = true // Set flag to true since address is fetched from DB
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ManageExistingRestaurantsActivity, "Failed to fetch restaurant details", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // Fetch address suggestions using Google Places API
    private fun fetchAddressSuggestions(query: String) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(query)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                val suggestions = response.autocompletePredictions.map { it.getFullText(null).toString() }
                updateAddressSuggestions(suggestions)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error fetching suggestions: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Update address suggestions in the dropdown
    private fun updateAddressSuggestions(suggestions: List<String>) {
        if (suggestions.isNotEmpty()) {
            binding.suggestionsContainer.visibility = View.VISIBLE
            val adapter = ArrayAdapter(
                this,
                android.R.layout.simple_dropdown_item_1line,
                suggestions
            )
            binding.addressSuggestions.adapter = adapter
            adapter.notifyDataSetChanged()

            binding.addressSuggestions.setOnItemClickListener { parent, _, position, _ ->
                val selectedAddress = parent.getItemAtPosition(position).toString()
                binding.restaurantAddressEditText.setText(selectedAddress)
                clearSuggestions()
                isAddressPrefilled = false // Address has been modified, so reset flag
            }
        } else {
            clearSuggestions()
        }
    }

    private fun clearSuggestions() {
        binding.suggestionsContainer.visibility = View.GONE
        binding.addressSuggestions.adapter = null
    }

    // Geocode the address to get latitude and longitude
    private fun geocodeAddress(address: String, callback: (LatLng?) -> Unit) {
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(address)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                if (response.autocompletePredictions.isNotEmpty()) {
                    val prediction = response.autocompletePredictions[0]
                    val placeId = prediction.placeId

                    val placeFields = listOf(Place.Field.LAT_LNG)
                    val placeRequest = FetchPlaceRequest.builder(placeId, placeFields).build()

                    placesClient.fetchPlace(placeRequest)
                        .addOnSuccessListener { fetchPlaceResponse ->
                            val place = fetchPlaceResponse.place
                            val latLng = place.latLng
                            callback(latLng)
                        }
                        .addOnFailureListener {
                            callback(null)
                        }
                } else {
                    callback(null)
                }
            }
            .addOnFailureListener {
                callback(null)
            }
    }

    // Update restaurant details (name and address) in the database
    private fun updateRestaurantDetailsInDatabase(
        selectedRestaurantName: String,
        updatedRestaurantName: String,
        updatedAddress: String,
        latLng: LatLng
    ) {
        database.reference.child("Restaurants").orderByChild("name").equalTo(selectedRestaurantName)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (restaurantSnapshot in snapshot.children) {
                        val restaurantId = restaurantSnapshot.child("id").getValue(String::class.java)
                        if (restaurantId != null) {
                            // Create a map with both the name and address fields to be updated
                            val updates = mapOf<String, Any>(
                                "Restaurants/$restaurantId/name" to updatedRestaurantName, // Update the restaurant name
                                "Restaurants/$restaurantId/address" to updatedAddress,
                                "Restaurants/$restaurantId/latitude" to latLng.latitude,
                                "Restaurants/$restaurantId/longitude" to latLng.longitude
                            )

                            database.reference.updateChildren(updates).addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(this@ManageExistingRestaurantsActivity, "Restaurant updated successfully", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(this@ManageExistingRestaurantsActivity, "Failed to update restaurant", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@ManageExistingRestaurantsActivity, "Failed to update restaurant details", Toast.LENGTH_SHORT).show()
                }
            })
    }
}