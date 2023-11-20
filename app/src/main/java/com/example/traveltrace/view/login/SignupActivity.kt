package com.example.traveltrace.view.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.traveltrace.R
import com.example.traveltrace.view.MainActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class SignupActivity : AppCompatActivity() {

    private lateinit var et_email: EditText
    private lateinit var et_name: EditText
    private lateinit var et_password: EditText
    private lateinit var et_re_password: EditText
    private lateinit var tv_alert: TextView
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
                alert(getString(R.string.signup_user_name))
            } else if (!isEmailValid(email)) {
                alert(getString(R.string.wrong_email))
            } else if (!isPasswValid(passw)) {
                alert(getString(R.string.wrong_passw))
            } else if (re_passw.isEmpty()) {
                alert(getString(R.string.repete))
            } else if (passw != re_passw) {
                alert(getString(R.string.passw_dont_match))
            } else {
                signup(email, passw, name)
            }
        }
    }

    private fun signup(email: String, passw: String, name: String) {
        //Init Ruedita
        auth.createUserWithEmailAndPassword(email, passw)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Fin Ruedita
                    //Registrar en BD
                    val user = hashMapOf(
                        "email" to email,
                        "name" to name,
                        "public" to true,
                        "googleProvieded" to false
                    )

                    // Agregar a la colección con nuevo ID
                    db.collection("users")
                        .document(auth.currentUser!!.uid)
                        .set(user)
                        .addOnSuccessListener { documentReference ->
                            Toast.makeText(
                                applicationContext,
                                "Se ha registrado con éxito",
                                Toast.LENGTH_SHORT
                            ).show()
                            startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                            finish()
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

    fun isEmailValid(email: String): Boolean {
        //Sin puntos iniciales, finales o consecutivos
        //val emailRegex = "^[A-Z0-9_!#\$%&'*+/=?`{|}~^-]+(?:\\.[A-Z0-9_!#\$%&'*+/=?`{|}~^-]+↵\n)*@[A-Z0-9-]+(?:\\.[A-Z0-9-]+)*\$"
        //email.matches(emailRegex.toRegex())
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswValid(passw: String): Boolean {
        // Mínimo 8 char, una minúscula, una mayúscula, un número
        val passwRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$"
        return passw.matches(passwRegex.toRegex())
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

    private fun init() {
        et_email = findViewById(R.id.et_email)
        et_name = findViewById(R.id.et_name)
        et_password = findViewById(R.id.et_password)
        et_re_password = findViewById(R.id.et_re_password)
        tv_alert = findViewById(R.id.tv_alert)
        signup = findViewById(R.id.signup)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }
}