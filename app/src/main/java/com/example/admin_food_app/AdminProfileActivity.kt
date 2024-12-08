package com.example.admin_food_app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_food_app.databinding.ActivityAdminProfileBinding
import com.example.admin_food_app.model.UserModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class AdminProfileActivity : AppCompatActivity() {
    private val binding  : ActivityAdminProfileBinding by lazy {
        ActivityAdminProfileBinding.inflate(layoutInflater)
    }
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var adminReference:DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        auth=FirebaseAuth.getInstance()
        database=FirebaseDatabase.getInstance()
        adminReference=database.reference.child("user")


        binding.backButton.setOnClickListener {
            finish()
        }

        binding.saveInfoButton.setOnClickListener {
            updateUserData()
        }

        binding.name.isEnabled=false
        binding.address.isEnabled=false
        binding.email.isEnabled=false
        binding.phone.isEnabled=false
        binding.password.isEnabled=false
        binding.saveInfoButton.isEnabled=false

        var isEnable = false
        binding.editButton.setOnClickListener {
            isEnable = !isEnable
            binding.name.isEnabled=isEnable
            binding.address.isEnabled=isEnable
            binding.email.isEnabled=isEnable
            binding.phone.isEnabled=isEnable
            binding.password.isEnabled=isEnable
            if(isEnable){
                binding.name.requestFocus()
            }
            binding.saveInfoButton.isEnabled=isEnable
        }

        retrieveUserData()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun retrieveUserData() {
        val currentUserUid = auth.currentUser?.uid
        if(currentUserUid != null){
            val userReference=adminReference.child(currentUserUid)

            userReference.addListenerForSingleValueEvent(object :ValueEventListener{
                override fun onDataChange(snapshot: DataSnapshot) {
                    if(snapshot.exists()){
//                        var ownerName=snapshot.child("name").getValue()
//                        var email=snapshot.child("email").getValue()
//                        var password=snapshot.child("password").getValue()
//                        var address=snapshot.child("address").getValue()
//                        var phone=snapshot.child("phone").getValue()
                        var ownerName=snapshot.child("name").getValue()
                        var address=snapshot.child("address").getValue()
                        var email=snapshot.child("email").getValue()
                        var phone=snapshot.child("phone").getValue()
                        var password=snapshot.child("password").getValue()

                        setDataToTextView(ownerName, email, password, address, phone)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }

            })
        }

    }

    private fun setDataToTextView(
        ownerName: Any?,
        email: Any?,
        password: Any?,
        address: Any?,
        phone: Any?
    ) {
//        binding.name.setText(ownerName.toString())
//        binding.email.setText(email.toString())
//        binding.password.setText(password.toString())
//        binding.phone.setText(phone.toString())
//        binding.address.setText(address.toString())
        binding.name.setText(ownerName.toString())
        binding.address.setText(address.toString())
        binding.email.setText(email.toString())
        binding.phone.setText(phone.toString())
        binding.password.setText(password.toString())
    }

    private fun updateUserData() {
//        var updateName = binding.name.text.toString()
//        var updateEmail = binding.email.text.toString()
//        var updatePassword = binding.password.text.toString()
//        var updatePhone = binding.phone.text.toString()
//        var updateAddress = binding.address.text.toString()
        var updateName = binding.name.text.toString()
        var updateAddress = binding.address.text.toString()
        var  updateEmail = binding.email.text.toString()
        var updatePhone = binding.phone.text.toString()
        var updatePassword = binding.password.text.toString()

        var userdata= UserModel(updateName, updateEmail, updatePassword, updatePhone, updateAddress)
        adminReference.setValue(userdata).addOnSuccessListener {
            Toast.makeText(this, "Profile Updated Successful", Toast.LENGTH_SHORT).show()
            auth.currentUser?.updateEmail(updateEmail)
            auth.currentUser?.updatePassword(updatePassword)
        }.addOnFailureListener {
            Toast.makeText(this, "Proile Updated Fail", Toast.LENGTH_SHORT).show()
        }
    }
}