package com.example.traveltrace

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class SignupActivity : AppCompatActivity() {

    private lateinit var et_email: EditText
    private lateinit var et_name: EditText
    private lateinit var et_password: EditText
    private lateinit var et_re_password: EditText
    private lateinit var signup: Button
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        init()

        signup.setOnClickListener {
            val email = et_email.text.toString()
            val name = et_name.text.toString()
            val passw = et_password.text.toString()
            val re_passw = et_re_password.text.toString()

            if (name.isEmpty()) {
                Toast.makeText(applicationContext, "Ingrese nombre de usuario", Toast.LENGTH_SHORT)
                    .show()
            } else if (email.isEmpty()) {
                Toast.makeText(applicationContext, "Ingrese su correo", Toast.LENGTH_SHORT).show()
            } else if (passw.isEmpty()) {
                Toast.makeText(applicationContext, "Ingrese su contraseña", Toast.LENGTH_SHORT)
                    .show()
            } else if (re_passw.isEmpty()) {
                Toast.makeText(
                    applicationContext,
                    "Por favor repita su contraseña",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (passw != re_passw) {
                Toast.makeText(
                    applicationContext,
                    "Las contraseñas no coinciden",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                signup(email, passw)
            }

        }

    }

    private fun signup(email: String, passw: String) {
        //Init Ruedita
        auth.createUserWithEmailAndPassword(email, passw)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Fin Ruedita

                    //Registrar en BD
                    val user = hashMapOf(
                        "email" to et_email.text.toString(),
                        "name" to et_name.text.toString(),
                        "password" to et_password.text.toString(),
                        "verify" to false
                    )

                    // Agregar a la colección con nuevo ID
                    db.collection("users")
                        .add(user)
                        .addOnSuccessListener { documentReference ->
                            Toast.makeText(
                                applicationContext,
                                "Se ha registrado con éxito ${documentReference.id}",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT)
                                .show()
                        }

                } else {
                    // Fin Ruedita
                    Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                // Fin Ruedita
                Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun init() {
        et_email = findViewById(R.id.et_email)
        et_name = findViewById(R.id.et_name)
        et_password = findViewById(R.id.et_password)
        et_re_password = findViewById(R.id.et_re_password)
        signup = findViewById(R.id.signup)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }
}