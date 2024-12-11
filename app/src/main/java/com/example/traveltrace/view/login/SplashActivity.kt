package com.example.traveltrace.view.login

/**
 * SplashActivity - Pantalla inicial que muestra animaciones
 * mientras verifica el estado de autenticación del usuario.
 *
 * Características principales:
 * - Animaciones de entrada (fade-in y escalado).
 * - Redirección automática:
 *      - Usuarios autenticados     -> MainActivity.
 *      - Usuarios no autenticados  -> LoginActivity.
 * - Usa FirebaseAuth para verificar el estado de autenticación.
 *
 * Tiempo visible: 2 segundos.
 */

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.firebase.auth.FirebaseAuth
import com.example.traveltrace.view.MainActivity
import com.example.traveltrace.R

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Configuración de las animaciones iniciales
        val logo = findViewById<ConstraintLayout>(R.id.cly_main)
        val fadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f).apply {
            duration = ANIMATION_DURATION
        }
        val scaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.8f, 1f).apply {
            duration = ANIMATION_DURATION
        }
        val scaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.8f, 1f).apply {
            duration = ANIMATION_DURATION
        }

        // Ejecutar las animaciones juntas
        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            interpolator = DecelerateInterpolator()
            start()
        }

        // Redirige al usuario después del tiempo establecido
        Handler(Looper.getMainLooper()).postDelayed({
            navigateToNextScreen()
        }, SPLASH_DELAY)
    }

    /**
     * Navega a la siguiente actividad según el estado de autenticación del usuario.
     * Usa FirebaseAuth para determinar si el usuario ya está autenticado.
     */
    private fun navigateToNextScreen() {
        val auth = FirebaseAuth.getInstance()
        try {
            val nextActivity = if (auth.currentUser != null) {
                MainActivity::class.java    // Usuario autenticado
            } else {
                LoginActivity::class.java   // Usuario no autenticado
            }
            startActivity(Intent(this@SplashActivity, nextActivity))
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            finish()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val SPLASH_DELAY = 2000L          // Tiempo de espera en milisegundos
        private const val ANIMATION_DURATION = 1000L    // Duración de la animación
    }
}