package com.example.traveltrace.view.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.traveltrace.R
import com.example.traveltrace.view.MainActivity

/**
 * SignupActivity - Actividad para el registro de nuevos usuarios.
 *
 * Características principales:
 * - Permite a los usuarios registrarse mediante correo y contraseña.
 * - Validación de campos (email, contraseña, confirmación de contraseña, nombre).
 * - Registro de los datos del usuario en Firebase Authentication y Firestore.
 */
class SignupActivity : AppCompatActivity() {

    private lateinit var et_email:          EditText            // Campo para el correo electrónico
    private lateinit var et_name:           EditText            // Campo para el nombre del usuario
    private lateinit var et_password:       EditText            // Campo para la contraseña
    private lateinit var et_re_password:    EditText            // Campo para repetir la contraseña
    private lateinit var tv_alert:          TextView            // Texto para mostrar alertas
    private lateinit var signup:            Button              // Botón para realizar el registro
    private lateinit var auth:              FirebaseAuth        // Instancia de Firebase Authentication
    private lateinit var db:                FirebaseFirestore   // Instancia de Firestore
    private lateinit var pb_signup:         ProgressBar         // Barra de progreso mientras se realiza el registro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)
        init() // Inicializar componentes

        // Registro
        signup.setOnClickListener {
            pb_signup.visibility = View.VISIBLE // Mostrar barra de progreso
            signup.visibility = View.GONE // Ocultar el botón durante el proceso

            // Obtener valores de los campos
            val email = et_email.text.toString()
            val name = et_name.text.toString()
            val passw = et_password.text.toString()
            val re_passw = et_re_password.text.toString()

            // Validación de los campos
            if (name.isEmpty()) {
                alert(getString(R.string.signup_user_name), 2)
            } else if (!isEmailValid(email)) {
                alert(getString(R.string.wrong_email), 2)
            } else if (!isPasswValid(passw)) {
                alert(getString(R.string.wrong_passw), 3)
            } else if (re_passw.isEmpty()) {
                alert(getString(R.string.repete), 2)
            } else if (passw != re_passw) {
                alert(getString(R.string.passw_dont_match), 2)
            } else {
                signup(email, passw, name) // Registrar al usuario
            }
        }
    }

    /**
     * Registra un nuevo usuario en Firebase Authentication y almacena sus datos en Firestore.
     * @param email Correo electrónico del usuario.
     * @param passw Contraseña del usuario.
     * @param name Nombre del usuario.
     */
    private fun signup(email: String, passw: String, name: String) {
        auth.createUserWithEmailAndPassword(email, passw)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Crear un mapa con la información del usuario para almacenar en Firestore
                    val user = hashMapOf(
                        "email" to email,
                        "name" to name,
                        "public" to true, // El los viajes del usuario serán públicos por defecto
                        "googleProvieded" to false // Indica que no es un usuario registrado con Google
                    )

                    if (task.isComplete) {
                        // Guardar los datos del usuario en Firestore
                        db.collection("users")
                            .document(auth.currentUser!!.uid) // Usar el UID generado por Firebase Authentication
                            .set(user)
                            .addOnSuccessListener {
                                Toast.makeText(applicationContext, "Se ha registrado con éxito", Toast.LENGTH_SHORT).show()
                                // Ir a la actividad principal
                                startActivity(Intent(this@SignupActivity, MainActivity::class.java))
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(applicationContext, "Error", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
            }
        // Ocultar barra de progreso y mostrar el botón nuevamente
        pb_signup.visibility = View.GONE
        signup.visibility = View.VISIBLE
    }

    /**
     * Verifica si un correo electrónico tiene un formato válido.
     * @param email Correo electrónico a verificar.
     * @return `true` si el correo es válido, `false` de lo contrario.
     */
    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Verifica si una contraseña cumple con los requisitos mínimos.
     * Requisitos:
     * - Mínimo 8 caracteres.
     * - Al menos una letra minúscula.
     * - Al menos una letra mayúscula.
     * - Al menos un número.
     * @param passw Contraseña a verificar.
     * @return `true` si la contraseña es válida, `false` de lo contrario.
     */
    private fun isPasswValid(passw: String): Boolean {
        val passwRegex =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$" // Expresión regular para validación
        return passw.matches(passwRegex.toRegex())
    }

    /**
     * Muestra un mensaje de alerta animado en la pantalla.
     * @param text Texto del mensaje.
     * @param duration Duración de la animación (en segundos).
     */
    private fun alert(text: String, duration: Long) {
        pb_signup.visibility = View.GONE // Ocultar barra de progreso
        signup.visibility = View.VISIBLE // Mostrar botón nuevamente
        val animation = AlphaAnimation(0f, 1f) // Animación de entrada
        animation.duration = 4000 * duration
        tv_alert.text = text
        tv_alert.startAnimation(animation)
        tv_alert.visibility = View.VISIBLE
        val animation2 = AlphaAnimation(1f, 0f) // Animación de salida
        animation2.duration = 4000 * duration
        tv_alert.startAnimation(animation2)
        tv_alert.visibility = View.INVISIBLE
    }

    /**
     * Inicializa los elementos visuales de la actividad.
     */
    private fun init() {
        et_email = findViewById(R.id.et_email)
        et_name = findViewById(R.id.et_name)
        et_password = findViewById(R.id.et_password)
        et_re_password = findViewById(R.id.et_re_password)
        tv_alert = findViewById(R.id.tv_alert)
        signup = findViewById(R.id.signup)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        pb_signup = findViewById(R.id.progress_bar)
    }
}
