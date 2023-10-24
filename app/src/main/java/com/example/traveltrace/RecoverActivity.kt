package com.example.traveltrace

import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RecoverActivity : AppCompatActivity() {

    private lateinit var et_email: EditText
    private lateinit var btn_recover: Button
    private lateinit var tv_alert: TextView
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recover)
        et_email = findViewById(R.id.et_email)
        btn_recover = findViewById(R.id.btn_recover)
        tv_alert = findViewById(R.id.tv_alert)
        auth = FirebaseAuth.getInstance()
        var db = FirebaseFirestore.getInstance()

        btn_recover.setOnClickListener {
            var email = et_email.text.toString()
            if (email.isEmpty()) {
                alert(getString(R.string.empty_email))
            } else {
                //Validar que exista una cuenta con ese correo
                db.collection("users")
                    .whereEqualTo("email", email)
                    .get().addOnSuccessListener { documents ->
                        if (documents.isEmpty) { // No existe ningún usuario registrado con ese email
                            alert(getString(R.string.wrong_email))
                        } else { // Existe un usuario con ese correo electrónico
                            // Enviar correo de recuperación
                            auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener {
                                    alert(getString(R.string.sended))
                                    startActivity(Intent(this, LoginActivity::class.java))
                                    finish()
                                }
                                .addOnFailureListener { e -> alert(e.message.toString())}
                        }
                    }.addOnFailureListener { exception ->
                        Log.w("E", exception)
                    }
            }
        }
    }

    private fun alert(text: String) {
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 4000
        tv_alert.setText(text)
        tv_alert.startAnimation(animation)
        tv_alert.setVisibility(View.VISIBLE)
        val animation2 = AlphaAnimation(1f, 0f)
        animation2.duration = 4000
        tv_alert.startAnimation(animation2)
        tv_alert.setVisibility(View.INVISIBLE)
    }
}