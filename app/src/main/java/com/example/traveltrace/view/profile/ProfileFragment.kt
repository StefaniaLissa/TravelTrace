package com.example.traveltrace.view.profile

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.traveltrace.R
import com.example.traveltrace.view.login.LoginActivity
import com.example.traveltrace.viewmodel.UserViewModel
import com.google.firebase.auth.FirebaseAuth

/**
 * Fragmento que muestra la información del perfil del usuario.
 *
 * Características principales:
 * - Permite al usuario cerrar sesión.
 * - Ofrece la opción de editar el perfil.
 * - Muestra datos como nombre, correo electrónico, visibilidad pública y foto de perfil.
 * - Sincroniza los datos del usuario con Firebase Firestore utilizando UserViewModel.
 */
class ProfileFragment : Fragment() {

    // Declaración de vistas y variables necesarias
    private lateinit var tv_name:       TextView        // Muestra el nombre del usuario
    private lateinit var tv_email:      TextView        // Muestra el correo electrónico del usuario
    private lateinit var btn_logout:    Button          // Botón para cerrar sesión
    private lateinit var viewModel:     UserViewModel   // ViewModel para observar los datos del usuario
    private lateinit var tv_online:     TextView        // Indicador de visibilidad pública del usuario
    private lateinit var iv_edit:       ImageView       // Botón para editar el perfil
    private lateinit var iv_imagen:     ImageView       // Imagen de perfil del usuario

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflar el diseño del fragmento y enlazar las vistas
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        tv_name = view.findViewById(R.id.tv_name)
        tv_email = view.findViewById(R.id.tv_email)
        btn_logout = view.findViewById(R.id.btn_logout)
        tv_online = view.findViewById(R.id.tv_online)
        iv_edit = view.findViewById(R.id.iv_edit)
        iv_imagen = view.findViewById(R.id.iv_imagen)

        // Cerrar sesión
        btn_logout.setOnClickListener {
            FirebaseAuth.getInstance().signOut() // Cierra la sesión del usuario
            startActivity(Intent(activity, LoginActivity::class.java)) // Redirige a la pantalla de inicio de sesión
            activity?.finish()
        }

        // Editar el perfil
        iv_edit.setOnClickListener {
            val bundle = Bundle().apply {
                // Pasa los datos del perfil actual al fragmento de edición
                putCharSequence("name", tv_name.text)
                putCharSequence("email", tv_email.text)
                putBoolean("public", tv_online.isVisible)
                putCharSequence("image", iv_imagen.contentDescription)
            }
            val fragmentTransaction = parentFragmentManager.beginTransaction()
            val editProfileFragment = EditProfileFragment().apply {
                arguments = bundle
            }
            // Reemplaza el fragmento actual por el de edición
            fragmentTransaction.add(R.id.fragment_container, editProfileFragment)
                .addToBackStack(null).commit()
        }

        // Inicialización del ViewModel para observar datos del usuario
        viewModel = ViewModelProvider(this)[UserViewModel::class.java]
        viewModel.user.observe(viewLifecycleOwner) { user ->
            // Verificar si los datos del usuario son nulos
            if (user == null) {
                tv_name.text = getString(R.string.user_not_found) // Mensaje de error
                tv_email.text = getString(R.string.empty)
                tv_online.visibility = View.GONE
                iv_imagen.setImageResource(R.drawable.ic_error) // Imagen de error
                return@observe
            }

            // Poblar las vistas con los datos del usuario
            if (!user.image.isNullOrEmpty()) {
                iv_imagen.contentDescription = user.image
                Glide.with(this)
                    .load(user.image)                       // Cargar imagen desde la URL
                    .placeholder(R.drawable.ic_downloading) // Imagen mientras carga
                    .error(R.drawable.ic_error)             // Imagen en caso de error
                    .centerCrop()
                    .into(iv_imagen)
            } else {
                iv_imagen.contentDescription = null
                iv_imagen.setImageResource(R.drawable.ic_user_placeholder) // Imagen predeterminada
            }

            tv_name.text = user.name
            tv_email.text = user.email
            tv_online.visibility = if (user.public == true) View.VISIBLE else View.GONE
        }

        return view
    }
}
