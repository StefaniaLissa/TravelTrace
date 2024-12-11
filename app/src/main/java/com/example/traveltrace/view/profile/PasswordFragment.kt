package com.example.traveltrace.view.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.example.traveltrace.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

/**
 * Fragmento para verificar la contraseña actual del usuario antes de permitir
 * el cambio de contraseña a través de un nuevo fragmento.
 *
 * Características principales:
 * - Validación de contraseña ingresada.
 * - Reautenticación del usuario mediante Firebase Authentication.
 * - Transición al fragmento de actualización de contraseña en caso de éxito.
 */
class PasswordFragment : Fragment() {

    // Campos y botones del diseño del fragmento
    private lateinit var et_password:   EditText    // Campo de entrada para la contraseña actual
    private lateinit var btn_passw:     Button      // Botón para confirmar la contraseña
    private lateinit var tv_alert:      TextView    // Texto para mostrar mensajes de alerta

    private lateinit var auth: FirebaseAuth // Instancia de FirebaseAuth
    var user: FirebaseUser? = null // Usuario autenticado actualmente

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_password, container, false)
        et_password = view.findViewById(R.id.et_password)
        btn_passw = view.findViewById(R.id.btn_passw)
        tv_alert = view.findViewById(R.id.tv_alert)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configurar el evento del botón para verificar la contraseña actual
        btn_passw.setOnClickListener {
            val passw = et_password.text.toString()

            // Validar si la contraseña cumple con los requisitos
            if (isPasswValid(passw)) {
                auth = FirebaseAuth.getInstance()
                user = FirebaseAuth.getInstance().currentUser

                // Intentar reautenticar al usuario con su contraseña actual
                auth.signInWithEmailAndPassword(user?.email.toString(), passw)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            // Transición al fragmento de actualización de contraseña
                            val fragmentTransaction = parentFragmentManager.beginTransaction()
                            val newPasswordFragment = NewPasswordFragment()
                            fragmentTransaction
                                .add(R.id.fragment_container, newPasswordFragment)
                                .addToBackStack(null)
                                .commit()

                            // Remover el fragmento actual
                            parentFragmentManager.beginTransaction().remove(this).commit()
                        }
                    }.addOnFailureListener { e ->
                        // Manejar fallos de autenticación
                        if (e.message?.contains("INVALID_LOGIN_CREDENTIALS") == true) {
                            alert(getString(R.string.wrong_passw)) // Contraseña incorrecta
                        } else {
                            alert(e.message.toString()) // Otro error
                        }
                    }
            } else {
                // Mostrar alerta si la contraseña ingresada no es válida
                alert("Contraseña inválida")
            }
        }
    }

    /**
     * Valida que la contraseña cumpla con los requisitos de seguridad:
     * - Mínimo 8 caracteres
     * - Al menos una letra minúscula
     * - Al menos una letra mayúscula
     * - Al menos un número
     *
     * @param passw Contraseña a validar.
     * @return `true` si cumple con los requisitos, `false` en caso contrario.
     */
    private fun isPasswValid(passw: String): Boolean {
        val passwRegex = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}\$"
        return passw.matches(passwRegex.toRegex())
    }

    /**
     * Muestra un mensaje de alerta animado en la pantalla.
     *
     * @param text El mensaje a mostrar.
     */
    private fun alert(text: String) {
        // Animación de aparición gradual
        val animation = AlphaAnimation(0f, 1f)
        animation.duration = 4000
        tv_alert.text = text
        tv_alert.startAnimation(animation)
        tv_alert.visibility = View.VISIBLE

        // Animación de desaparición gradual
        val animation2 = AlphaAnimation(1f, 0f)
        animation2.duration = 4000
        tv_alert.startAnimation(animation2)
        tv_alert.visibility = View.INVISIBLE
    }
}
