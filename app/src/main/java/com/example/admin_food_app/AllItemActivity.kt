package com.example.admin_food_app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.admin_food_app.adapter.MenuItemAdapter
import com.example.admin_food_app.databinding.ActivityAllItemBinding
import com.example.admin_food_app.model.AllMenu
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AllItemActivity : AppCompatActivity() {

    private lateinit var databaseReference: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private var menuItems: ArrayList<AllMenu> = ArrayList()
    private val binding : ActivityAllItemBinding by lazy {
        ActivityAllItemBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        databaseReference = FirebaseDatabase.getInstance().reference
        retrieveMenuItem()

        binding.backButton.setOnClickListener {
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun retrieveMenuItem() {
        database=FirebaseDatabase.getInstance()
        val foodRef: DatabaseReference = database.reference.child("menu")

        //fetch data from data base
        foodRef.addListenerForSingleValueEvent(object :ValueEventListener{

            override fun onDataChange(snapshot: DataSnapshot){
                //clear existing data before populaing
                menuItems.clear()
                //loop for through each food item
                for(foodSnapshot in snapshot.children){
                    val menuItem=foodSnapshot.getValue(AllMenu::class.java)
                    menuItem?.let {
                        menuItems.add(it)
                    }
                }
                setAdapter()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.d("DatabaseError", "Error: ${error.message}")
            }
        })
    }
    private fun setAdapter() {

        val adapter = MenuItemAdapter(this@AllItemActivity, menuItems, databaseReference){ position ->
            deleteMenuItems(position)
        }
        binding.MenuRecyclerView.layoutManager=LinearLayoutManager(this)
        binding.MenuRecyclerView.adapter=adapter
    }

    private fun deleteMenuItems(position: Int) {
        if (position < 0 || position >= menuItems.size) {
            // Position is out of bounds, return early
            Log.e("DeleteMenuItem", "Invalid position: $position")
            return
        }

        // Proceed with deletion if position is valid
        val menuItemToDelete = menuItems[position]
        val menuItemKey = menuItemToDelete.key
        val foodMenuReference = database.reference.child("menu").child(menuItemKey!!)

        foodMenuReference.removeValue().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Remove the item from the list
                menuItems.removeAt(position)

                // Notify the adapter that the item was removed
                binding.MenuRecyclerView.adapter?.notifyItemRemoved(position)

                // Optionally, you may want to notify that the range of items may have changed
                binding.MenuRecyclerView.adapter?.notifyItemRangeChanged(position, menuItems.size)

            } else {
                // Handle failure
                Toast.makeText(this, "Item not deleted", Toast.LENGTH_SHORT).show()
            }
        }
    }

}
