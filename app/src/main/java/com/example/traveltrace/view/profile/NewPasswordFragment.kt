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
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.example.traveltrace.R

/**
 * Fragmento para actualizar la contraseña del usuario autenticado.
 *
 * Características principales:
 * - Valida la contraseña ingresada con un formato seguro.
 * - Permite al usuario actualizar su contraseña en Firebase Authentication.
 */
class NewPasswordFragment : Fragment() {

    private lateinit var et_password:   EditText    // Campo para ingresar la nueva contraseña
    private lateinit var btn_passw:     Button      // Botón para actualizar la contraseña
    private lateinit var tv_alert:      TextView    // Texto para mostrar mensajes de alerta

    var user: FirebaseUser? = null // Usuario autenticado actualmente

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_new_password, container, false)
        et_password = view.findViewById(R.id.et_password)
        btn_passw = view.findViewById(R.id.btn_passw)
        tv_alert = view.findViewById(R.id.tv_alert)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Obtener el usuario actualmente autenticado
        user = FirebaseAuth.getInstance().currentUser

        // Configurar el evento del botón para actualizar la contraseña
        btn_passw.setOnClickListener {
            val passw = et_password.text.toString() // Obtener la contraseña ingresada
            if (!isPasswValid(passw)) {
                // Mostrar alerta si la contraseña no cumple los requisitos
                alert(getString(R.string.wrong_passw))
            } else {
                // Actualizar la contraseña si es válida
                update(passw)
                // Cerrar el fragmento actual
                parentFragmentManager.beginTransaction().remove(this).commit()
            }
        }
    }

    /**
     * Actualiza la contraseña del usuario autenticado en Firebase Authentication.
     *
     * @param passw La nueva contraseña a establecer.
     */
    private fun update(passw: String) {
        user!!.updatePassword(passw)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // Mostrar un mensaje de éxito al usuario
                    Toast.makeText(
                        context,
                        "Contraseña Actualizada",
                        Toast.LENGTH_SHORT
                    ).show()
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
}
