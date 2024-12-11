package com.example.traveltrace.view.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.example.traveltrace.R
import com.example.traveltrace.view.login.LoginActivity

/**
 * RecoverActivity - Actividad para recuperar la contraseña de un usuario.
 *
 * Características principales:
 * - Permite al usuario ingresar su correo electrónico registrado.
 * - Envía un correo de recuperación para restablecer la contraseña.
 * - Cambia dinámicamente la interfaz una vez enviado el correo.
 */
class RecoverActivity : AppCompatActivity() {

    private lateinit var et_email:      EditText        // Campo de entrada para el email
    private lateinit var btn_recover:   Button          // Botón para enviar correo de recuperación
    private lateinit var tv_alert:      TextView        // Texto de alerta para errores o confirmaciones
    private lateinit var auth:          FirebaseAuth    // Objeto para la autenticación Firebase
    private lateinit var tv_forgot:     TextView        // Texto que se actualiza al enviar el correo
    private lateinit var til_email:     TextInputLayout // Contenedor para el campo de email con estilos de material design

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recover)

        // Inicializar elementos del diseño
        et_email = findViewById(R.id.et_email)
        btn_recover = findViewById(R.id.btn_recover)
        tv_alert = findViewById(R.id.tv_alert)
        tv_forgot = findViewById(R.id.tv_forgot)
        til_email = findViewById(R.id.til_email)

        auth = FirebaseAuth.getInstance() // Inicializar FirebaseAuth

        // Manejar clic del botón de recuperación
        btn_recover.setOnClickListener {
            val email = et_email.text.toString()
            if (email.isEmpty()) {
                // Mostrar alerta si el email está vacío
                alert(getString(R.string.empty_email), 1)
            } else {
                // Verificar si el correo está registrado en Firebase Authentication
                auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Enviar correo de recuperación
                        auth.setLanguageCode("es") // Idioma del correo de recuperación
                        auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener { sendTask ->
                                if (sendTask.isSuccessful) {
                                    // Mostrar mensaje de éxito y cambiar la interfaz
                                    alert(getString(R.string.sended), 1)

                                    // Ocultar el campo de email y cambiar el texto del botón
                                    et_email.visibility  = View.GONE
                                    til_email.visibility = View.GONE
                                    tv_forgot.text   = getString(R.string.emailEnviado)
                                    btn_recover.text = getString(R.string.iniciar_sesion)
                                    btn_recover.setOnClickListener {
                                        startActivity(Intent(this, LoginActivity::class.java))
                                        finish()
                                    }
                                } else {
                                    // Mostrar mensaje de error si falla el envío
                                    alert(task.exception!!.message.toString(), 1)
                                }
                            }.addOnFailureListener { e ->
                                // Mostrar error de red o servidor
                                alert(e.message.toString(), 1)
                            }
                    } else {
                        // Mostrar mensaje si el correo no está registrado
                        alert("Correo electrónico sin registrar", 2)
                    }
                }
            }
        }
    }

    /**
     * Muestra una alerta animada en la parte superior de la pantalla.
     * @param text Mensaje a mostrar.
     * @param duration Duración de la animación en segundos.
     */
    private fun alert(text: String, duration: Long) {
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
}
