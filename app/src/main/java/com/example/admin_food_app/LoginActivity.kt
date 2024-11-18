package com.example.admin_food_app

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.admin_food_app.databinding.ActivityLoginBinding
import com.example.admin_food_app.model.UserModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.core.Tag
import com.google.firebase.database.database

class LoginActivity : AppCompatActivity() {

    private  var userName: String ?= null
    private  var nameOfRestaurant: String ?= null
    private lateinit var email: String
    private lateinit var password: String
    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleSignInClient: GoogleSignInClient

    private val binding:ActivityLoginBinding by lazy{
        ActivityLoginBinding.inflate(layoutInflater)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        val googleSignInOptions = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        auth = Firebase.auth
        database=Firebase.database.reference
        googleSignInClient= GoogleSignIn.getClient(this, googleSignInOptions)
        binding.loginButton.setOnClickListener {

            email = binding.Email.text.toString().trim()
            password = binding.Password.text.toString().trim()

            if(email.isBlank() || password.isBlank()){
                Toast.makeText(this, "Please fill all details", Toast.LENGTH_SHORT).show()
            }else{
                createUserAccount(email, password)
            }
        }
        binding.GoogleButton.setOnClickListener {
            val signIntent = googleSignInClient.signInIntent
            launcer.launch(signIntent)
        }
        binding.dontHaveAccountButton.setOnClickListener {
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun createUserAccount(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if(task.isSuccessful){
                val user=auth.currentUser
                Toast.makeText(this, "Login Successfully", Toast.LENGTH_SHORT).show()
                updateUi(user)
            }else{
                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        val user=auth.currentUser
                        Toast.makeText(this, "Create User & Login Successfully", Toast.LENGTH_SHORT).show()
                        saveUserData()
                        updateUi(user)
                    }else{
                        Toast.makeText(this, "Authentication failed", Toast.LENGTH_SHORT).show()
                        Log.d("Account", "createUserAccount: Authentication failed", task.exception)
                    }
                }
            }
        }
    }

    private fun saveUserData() {
        email = binding.Email.text.toString().trim()
        password = binding.Password.text.toString().trim()

        val user = UserModel(userName, nameOfRestaurant, email, password)
        val userId=FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            database.child("user").child(it).setValue(user)
        }
    }

    private fun saveGoogleUserData(account: GoogleSignInAccount) {
        val userName = account.displayName ?: "Unknown" // Poți folosi displayName sau altceva
        val email = account.email ?: "Unknown"
        val user = UserModel(userName, null, email, null) // Poți adăuga restul câmpurilor dacă ai nevoie

        // Salvăm datele utilizatorului în Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            database.child("user").child(it).setValue(user).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Firebase", "User data saved successfully")
                } else {
                    Log.d("Firebase", "Failed to save user data", task.exception)
                }
            }
        }
    }

    private val launcer = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        result ->
        if(result.resultCode == Activity.RESULT_OK){
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            if(task.isSuccessful){
                val account : GoogleSignInAccount = task.result
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                auth.signInWithCredential(credential).addOnCompleteListener { authTask ->
                    if(authTask.isSuccessful){
                        Toast.makeText(this, "Successfully sign in with Google", Toast.LENGTH_SHORT).show()
                        //saveGoogleUserData(account)
                        updateUi(authTask.result?.user)
                        finish()
                    }else{
                        Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            else{
                Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show()
            }
        }
    }
    //check if user is already logged in
    override fun onStart(){
        super.onStart()
        val currentUser = auth.currentUser
        if(currentUser!=null){
            startActivity(Intent(this, MainActivity::class.java))
            finish()

        }
    }
    private fun updateUi(user: FirebaseUser?) {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}