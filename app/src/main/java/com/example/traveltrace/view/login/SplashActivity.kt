package com.example.traveltrace.view.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.traveltrace.R
import com.example.traveltrace.view.MainActivity
import com.google.firebase.auth.FirebaseAuth

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        var auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            startActivity(Intent(this@SplashActivity, MainActivity::class.java))
        } else {
            startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
        }
        finish()
    }
}