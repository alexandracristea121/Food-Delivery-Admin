package com.example.admin_food_app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_food_app.databinding.ActivityAddItemBinding
import com.example.admin_food_app.model.AllMenu
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage

class AddItemActivity : AppCompatActivity() {

    private lateinit var foodName: String
    private lateinit var foodPrice: String
    private lateinit var foodDescription: String
    private lateinit var foodIngredient: String
    private var foodImage: Uri?=null

    //Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    private val binding : ActivityAddItemBinding by lazy{
        ActivityAddItemBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        //initialize
        auth=FirebaseAuth.getInstance()
        database=FirebaseDatabase.getInstance()

        binding.AddItemButton.setOnClickListener {
            //get data from filed
            foodName=binding.foodName.text.toString().trim()
            foodPrice=binding.foodPrice.text.toString().trim()
            foodDescription=binding.description.text.toString().trim()
            foodIngredient=binding.ingredient.text.toString().trim()

            if(!(foodName.isBlank() || foodPrice.isBlank() || foodDescription.isBlank() || foodIngredient.isBlank())){
                uploadData()
                Toast.makeText(this, "Item Add Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }else{
                Toast.makeText(this, "Fill all the details", Toast.LENGTH_SHORT).show()
            }
        }
        binding.selectImage.setOnClickListener {
            pickImage.launch("image/*")
        }


        binding.backButton.setOnClickListener {
            finish()
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun uploadData() {
          //get a reference to the "menu" node is the database
        val menuRef= database.getReference("menu")
        //generate a unique key for the new menu item
        val newItemKey =menuRef.push().key

        if(foodImage != null){
            val storageRef = FirebaseStorage.getInstance().reference
            val imageRef = storageRef.child("menu_images/${newItemKey}.jpg")
            val uploadTask = imageRef.putFile(foodImage!!)

            uploadTask.addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener {
                    downloadUrl->
                    //create a new menu item
                    val newItem = AllMenu(
                        newItemKey,
                        foodName = foodName,
                        foodPrice = foodPrice,
                        foodDescription = foodDescription,
                        foodIngredient = foodIngredient,
                        foodImage = downloadUrl.toString()
                    )
                    newItemKey?.let {
                        key ->
                        menuRef.child(key).setValue(newItem).addOnSuccessListener {
                            Toast.makeText(this, "data uploaded successfully", Toast.LENGTH_SHORT).show()
                        }
                            .addOnFailureListener{
                                Toast.makeText(this, "data uploaded failed", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }.addOnFailureListener{
                Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show()
            }

        }else{
            Toast.makeText(this, "Image Upload Failed", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()){uri->
        if(uri!=null)
        {
            binding.selectedImage.setImageURI(uri)
            foodImage = uri
        }
    }
}