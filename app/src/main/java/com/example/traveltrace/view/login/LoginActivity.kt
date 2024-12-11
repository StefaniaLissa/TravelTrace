package com.example.traveltrace.view.login

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.example.traveltrace.R
import com.example.traveltrace.view.MainActivity
import com.example.traveltrace.view.login.RecoverActivity

/**
 * LoginActivity - Actividad para gestionar el inicio de sesión.
 *
 * Características principales:
 * - Inicio de sesión por correo y contraseña.
 * - Integración con Google Sign-In para autenticación.
 * - Validación de credenciales de usuario.
 * - Registro de usuarios nuevos autenticados con Google en Firestore.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var et_email:          EditText        // Campo de entrada para el email
    private lateinit var et_password:       EditText        // Campo de entrada para la contraseña
    private lateinit var tv_alert:          TextView        // Texto de alerta para errores
    private lateinit var btn_login:         Button          // Botón de inicio de sesión
    private lateinit var tv_forgot:         TextView        // Texto para recuperar contraseña
    private lateinit var btn_login_google:  LinearLayout    // Botón para inicio de sesión con Google
    private lateinit var btn_signup:        Button          // Botón para registrarse
    private lateinit var pb_login:          ProgressBar     // Barra de progreso para inicio de sesión normal
    private lateinit var pb_login_google:   ProgressBar     // Barra de progreso para inicio con Google
    private lateinit var auth:              FirebaseAuth    // Objeto para gestionar la autenticación Firebase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        init() // Inicializar elementos del diseño

        // Inicio de sesión con correo y contraseña
        btn_login.setOnClickListener {
            login(et_email.text.toString(), et_password.text.toString())
        }

        // Recuperar contraseña
        tv_forgot.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RecoverActivity::class.java))
        }

        // Registrarse
        btn_signup.setOnClickListener {
            startActivity(Intent(this@LoginActivity, SignupActivity::class.java))
        }

        // Iniciar sesión con Google
        btn_login_google.setOnClickListener {
            google()
        }
    }

    /**
     * Método para manejar el inicio de sesión con correo y contraseña.
     * Valida las credenciales antes de proceder con la autenticación.
     */
    private fun login(email: String, passw: String) {
        if (!isEmailValid(email)) {
            alert(getString(R.string.wrong_email)) // Mostrar alerta para email inválido
        } else if (!isPasswValid(passw)) {
            alert(getString(R.string.wrong_passw)) // Mostrar alerta para contraseña inválida
        } else {
            // Mostrar barra de progreso y ocultar botón de login
            pb_login.visibility = View.VISIBLE
            btn_login.visibility = View.GONE

            auth.signInWithEmailAndPassword(email, passw).addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Redirigir a MainActivity en caso de éxito
                    pb_login.visibility = View.GONE
                    startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                    finish()
                }
            }.addOnFailureListener { e ->
                // Manejo de errores
                pb_login.visibility = View.GONE
                btn_login.visibility = View.VISIBLE
                if (e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true) {
                    alert(getString(R.string.wrong_login))
                } else {
                    Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Inicio de sesión con Google.
     */
    private fun google() {
        pb_login_google.visibility = View.VISIBLE
        btn_login_google.visibility = View.GONE
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Obtener el token ID del cliente web
            .requestEmail()
            .build()
        val googleClient = GoogleSignIn.getClient(this, gso)
        val googleSignIntent = googleClient.signInIntent
        googleSignInARL.launch(googleSignIntent) // Lanzar actividad para seleccionar cuenta
    }

    /**
     * Manejador de resultados del inicio de sesión con Google.
     */
    private val googleSignInARL = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { resultado ->
        if (resultado.resultCode == RESULT_OK) {
            val data = resultado.data
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                verificarFirebase(account.idToken)
            } catch (e: Exception) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
        } else {
            pb_login_google.visibility = View.GONE
            btn_login_google.visibility = View.VISIBLE
            Toast.makeText(applicationContext, "Cancelado", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Método para verificar el token de Google con Firebase y manejar el registro de nuevos usuarios.
     */
    private fun verificarFirebase(idToken: String?) {
        val credencial = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credencial)
            .addOnSuccessListener { authResult ->
                if (authResult.additionalUserInfo!!.isNewUser) {
                    insertFirebase() // Registrar usuario en Firestore
                } else {
                    // Usuario ya registrado
                    pb_login_google.visibility = View.GONE
                    btn_login_google.visibility = View.VISIBLE
                    startActivity(Intent(this, MainActivity::class.java))
                    finishAffinity()
                }
            }.addOnFailureListener { e ->
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Registro de usuarios nuevos autenticados con Google en Firestore.
     */
    private fun insertFirebase() {
        val db = FirebaseFirestore.getInstance()
        val email = auth.currentUser?.email
        val name = auth.currentUser?.displayName.toString()
        val user = hashMapOf(
            "email" to email,
            "name" to name,
            "public" to true,
            "googleProvieded" to true
        )
        pb_login_google.visibility = View.GONE
        btn_login_google.visibility = View.VISIBLE
        db.collection("users").document(auth.currentUser!!.uid).set(user)
            .addOnSuccessListener {
                startActivity(Intent(this@LoginActivity, MainActivity::class.java))
            }
            .addOnFailureListener { e ->
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Valida el formato de un correo electrónico.
     */
    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Valida la contraseña según el patrón definido.
     */
    private fun isPasswValid(passw: String): Boolean {
        val passwRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$"
        return passw.matches(passwRegex.toRegex())
    }

    /**
     * Muestra un mensaje de alerta en pantalla.
     */
    private fun alert(text: String) {
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 4000
        tv_alert.text = text
        tv_alert.startAnimation(animation)
        tv_alert.visibility = View.VISIBLE
        val animation2 = AlphaAnimation(1f, 0f)
        animation2.duration = 4000
        tv_alert.startAnimation(animation2)
        tv_alert.visibility = View.INVISIBLE
    }

    /**
     * Inicializa los elementos de la interfaz gráfica.
     */
    private fun init() {
        et_email            = findViewById(R.id.et_email)
        et_password         = findViewById(R.id.et_password)
        tv_alert            = findViewById(R.id.tv_alert)
        btn_login           = findViewById(R.id.btn_login)
        tv_forgot           = findViewById(R.id.tv_forgot)
        btn_login_google    = findViewById(R.id.btn_login_google)
        btn_signup          = findViewById(R.id.btn_signup)
        auth                = FirebaseAuth.getInstance()
        pb_login            = findViewById(R.id.pb_login)
        pb_login_google     = findViewById(R.id.pb_login_google)
    }
}
