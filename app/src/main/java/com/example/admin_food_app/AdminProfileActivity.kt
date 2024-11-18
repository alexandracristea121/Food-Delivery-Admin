package com.example.admin_food_app

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_food_app.databinding.ActivityAdminProfileBinding

class AdminProfileActivity : AppCompatActivity() {
    private val binding  : ActivityAdminProfileBinding by lazy {
        ActivityAdminProfileBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)
        binding.backButton.setOnClickListener {
            finish()
        }
        binding.name.isEnabled=false
        binding.address.isEnabled=false
        binding.email.isEnabled=false
        binding.phone.isEnabled=false
        binding.password.isEnabled=false

        var isEnable = false
        binding.editButton.setOnClickListener {
            isEnable = ! isEnable
            binding.name.isEnabled=isEnable
            binding.address.isEnabled=isEnable
            binding.email.isEnabled=isEnable
            binding.phone.isEnabled=isEnable
            binding.password.isEnabled=isEnable
            if(isEnable){
                binding.name.requestFocus()
            }
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }
}