package com.example.traveltrace.view.profile

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.traveltrace.R
import com.example.traveltrace.helper.ImagePickerHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.traveltrace.view.login.LoginActivity

/**
 * Fragmento para editar el perfil del usuario.
 * Permite cambiar nombre, correo electrónico, visibilidad pública, imagen de perfil,
 * así como eliminar el perfil del usuario.
 */
class EditProfileFragment : Fragment() {

    // Instancia del helper para seleccionar imágenes
    private lateinit var imagePickerHelper: ImagePickerHelper

    // Declaración de vistas
    private lateinit var iv_imagen:     ImageView       // Imagen del perfil
    private lateinit var iv_delete:     ImageView       // Botón para eliminar la imagen de perfil
    private lateinit var btn_new_image: Button          // Botón para seleccionar nueva imagen
    private lateinit var et_name:       EditText        // Campo de texto para el nombre
    private lateinit var et_email:      EditText        // Campo de texto para el correo electrónico
    private lateinit var tv_alert:      TextView        // Mensaje de alerta
    private lateinit var tv_delete:     TextView        // Mensaje para eliminar perfil
    private lateinit var btn_save:      Button          // Botón para guardar cambios
    private lateinit var cb_online:     CheckBox        // CheckBox para visibilidad pública
    private lateinit var btn_passw:     Button          // Botón para cambiar contraseña
    private lateinit var pb_img:        ProgressBar     // Barra de progreso para la imagen
    private var lv_public_old:          Boolean = true  // Estado anterior de visibilidad pública
    private var oldName:                String = ""     // Nombre anterior
    private lateinit var pb_save:       ProgressBar     // Barra de progreso para guardar

    private var uri: Uri? = null // URI de la nueva imagen seleccionada
    private lateinit var auth: FirebaseAuth// Instancias de Firebase
    var user: FirebaseUser? = null // Usuario autenticado

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        // Inflar la vista del fragmento
        val view = inflater.inflate(R.layout.fragment_edit_profile, container, false)

        // Inicialización de vistas
        iv_imagen = view.findViewById(R.id.iv_imagen)
        iv_delete = view.findViewById(R.id.iv_delete)
        btn_new_image = view.findViewById(R.id.btn_new_image)
        et_name = view.findViewById(R.id.et_name)
        et_email = view.findViewById(R.id.et_email)
        tv_alert = view.findViewById(R.id.tv_alert)
        tv_delete = view.findViewById(R.id.tv_delete)
        btn_save = view.findViewById(R.id.btn_save)
        cb_online = view.findViewById(R.id.cb_online)
        btn_passw = view.findViewById(R.id.btn_passw)
        pb_img = view.findViewById(R.id.pb_img)
        pb_save = view.findViewById(R.id.pb_save)

        // Configurar autenticación y usuario actual
        auth = FirebaseAuth.getInstance()
        user = auth.currentUser

        // Obtener información pasada al fragmento
        oldName = arguments?.getString("name").toString()
        et_name.setText(oldName) // Mostrar el nombre actual
        et_email.setText(arguments?.getString("email").toString()) // Mostrar el correo actual

        // Cargar imagen de perfil si existe
        if (arguments?.getString("image") != null) {
            pb_img.visibility = View.VISIBLE
            Glide.with(this).load(arguments?.getString("image").toString())
                .listener(object : RequestListener<Drawable> {
                    override fun onLoadFailed(
                        e: GlideException?,
                        model: Any?,
                        target: Target<Drawable>,
                        isFirstResource: Boolean
                    ): Boolean {
                        pb_img.visibility = View.GONE
                        return false
                    }

                    override fun onResourceReady(
                        resource: Drawable,
                        model: Any,
                        target: Target<Drawable>?,
                        dataSource: DataSource,
                        isFirstResource: Boolean
                    ): Boolean {
                        pb_img.visibility = View.GONE
                        return false
                    }
                }).placeholder(R.drawable.ic_downloading)   // Placeholder mientras carga
                .error(R.drawable.ic_error)                 // Imagen en caso de error
                .centerCrop()                               // Escalar la imagen al centro
                .into(iv_imagen)                            // Mostrar la imagen en el ImageView
        }

        // Configurar visibilidad pública
        cb_online.isChecked = arguments?.getBoolean("public")!!
        lv_public_old = cb_online.isChecked // Guardar estado anterior

        // Configurar helper para seleccionar imágenes
        imagePickerHelper = ImagePickerHelper(
            context = requireContext(),
            singleImageMode = true, // Permitir una sola imagen
            onImagePicked = { uris ->
                if (uris.isNotEmpty()) {
                    uri = uris[0] // Obtener URI de la imagen seleccionada
                    iv_imagen.setImageURI(uri) // Mostrar imagen seleccionada
                }
            }
        )

        return view
    }

    // Configuración del comportamiento de botones y eventos
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Botón para seleccionar nueva imagen
        btn_new_image.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        //Cambiar Contraseña
        btn_passw.setOnClickListener {
            val fragmentTransaction = parentFragmentManager.beginTransaction()
            val passwordFragment = PasswordFragment()
            fragmentTransaction.add(R.id.fragment_container, passwordFragment).addToBackStack(null)
                .commit()
        }

        // Guardar Cambios
        btn_save.setOnClickListener {
            pb_save.visibility = View.VISIBLE
            btn_save.visibility = View.GONE

            //Validacion de la Imágen de Perfil
            val isNewImage =
                uri != null && uri.toString() != arguments?.getString("image").toString()
            if (isNewImage) {
                //Subir a Firebase Store
                val path = "UserProfile/" + auth.uid
                val referenceStorage = FirebaseStorage.getInstance().getReference(path)
                referenceStorage.putFile(uri!!).addOnSuccessListener { taskSnapshot ->
                    // Obtiene la URL de descarga de la imagen subida
                    taskSnapshot.storage.downloadUrl.addOnCompleteListener { uriTask ->
                        if (uriTask.isSuccessful) {
                            val url = uriTask.result.toString()
                            UpdateFirestore(url)
                        } else {
                            Toast.makeText(
                                context,
                                "Error al obtener la URL de descarga: ${uriTask.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "No se ha podido subir la imagen debido a: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            val newEmail = et_email.text.toString()
            val isEmailValid = isEmailValid(newEmail)
            val isEmailNew = (newEmail != user!!.email)
            if (isEmailNew) {
                if (isEmailValid) {

                    val currentEmail = user!!.email
                    val builder = androidx.appcompat.app.AlertDialog.Builder(
                        requireContext(), R.style.CustomDialogTheme
                    )
                    builder.setTitle("Reautenticación requerida")
                    builder.setMessage("Por favor, ingrese su contraseña actual para confirmar los cambios.")

                    val input = EditText(requireContext()).apply {
                        setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow))
                        setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                        setHintTextColor(ContextCompat.getColor(requireContext(), R.color.gray))
                        hint = "Enter text here"
                        textAlignment = View.TEXT_ALIGNMENT_CENTER
                    }

                    input.inputType =
                        android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
                    input.hint = "Contraseña actual"
                    builder.setView(input)

                    builder.setPositiveButton("Confirmar") { _, _ ->
                        val currentPassword = input.text.toString()
                        val credential = FirebaseAuth.getInstance()
                            .signInWithEmailAndPassword(currentEmail!!, currentPassword)

                        credential.addOnSuccessListener {
                            user!!.updateEmail(newEmail).addOnSuccessListener {
                                FirebaseFirestore.getInstance().collection("users")
                                    .document(user!!.uid).update("email", newEmail)
                                    .addOnFailureListener {
                                        Toast.makeText(
                                            context,
                                            "Error al actualizar el correo electrónico: ${it.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                Toast.makeText(
                                    context, "Correo actualizado con éxito", Toast.LENGTH_SHORT
                                ).show()
                            }.addOnFailureListener { e ->
                                Toast.makeText(
                                    context,
                                    "Error al actualizar el correo electrónico: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "La reautenticación falló: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnCompleteListener {
                            pb_save.visibility = View.GONE
                            btn_save.visibility = View.VISIBLE

                        }
                    }

                    builder.setNegativeButton("Cancelar") { dialog, _ ->
                        dialog.dismiss()
                        pb_save.visibility = View.GONE
                        btn_save.visibility = View.VISIBLE
                    }

                    builder.create().show()

                } else {
                    alert(getString(R.string.wrong_email))
                    pb_save.visibility = View.GONE
                    btn_save.visibility = View.VISIBLE
                }
            }

            if (cb_online.isChecked != lv_public_old) {
                FirebaseFirestore.getInstance().collection("users").document(user!!.uid)
                    .update("public", cb_online.isChecked).addOnCompleteListener {
                        pb_save.visibility = View.GONE
                        btn_save.visibility = View.VISIBLE
                    }
            }


            if (et_name.text.toString().trim().isEmpty()) {
                alert("El nombre no puede estar vacío")
            } else if (et_name.text.toString() != oldName) {
                FirebaseFirestore.getInstance().collection("users").document(user!!.uid)
                    .update("name", et_name.text.toString().trim()).addOnCompleteListener {
                        pb_save.visibility = View.GONE
                        btn_save.visibility = View.VISIBLE
                    }
            }

            parentFragmentManager.beginTransaction().remove(this).commit()
        }

        tv_delete.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(
                requireContext(), R.style.CustomDialogTheme
            )
            builder.setTitle("Confirmación")
            builder.setMessage("¿Está seguro de que desea eliminar su cuenta? Esta acción no se puede deshacer.")

            builder.setPositiveButton("Eliminar") { dialog, _ ->
                dialog.dismiss()

                val userId = auth.uid!!
                val db = FirebaseFirestore.getInstance()
                val storage = FirebaseStorage.getInstance()

                // Paso 0: Buscar los viajes donde el usuario es administrador
                db.collection("members")
                    .whereEqualTo("userID", userId) // Filtrar por ID del usuario
                    .whereEqualTo("admin", true)   // Filtrar por rol de administrador
                    .get()
                    .addOnSuccessListener { querySnapshot ->
                        if (!querySnapshot.isEmpty) {
                            val tripIds = mutableListOf<String>()

                            // Paso 2: Extraer los tripID
                            for (document in querySnapshot.documents) {
                                val tripID = document.getString("tripID")
                                if (tripID != null) {
                                    tripIds.add(tripID)
                                }
                            }

                            // Paso 3: Eliminar los viajes y sus stops
                            tripIds.forEach { tripID ->

                                // Eliminar "members"
                                db.collection("members").whereEqualTo("tripID", tripID).get()
                                    .addOnSuccessListener { snapshot ->
                                        for (doc in snapshot) {
                                            db.collection("members").document(doc.id).delete()
                                        }
                                    }

                                // Eliminar imágenes de "TripCover"
                                val storage = FirebaseStorage.getInstance()
                                val tripCoverPath = "TripCover/$tripID/"
                                val tripCoverRef = storage.reference.child(tripCoverPath)
                                tripCoverRef.listAll().addOnSuccessListener { listResult ->
                                    for (file in listResult.items) {
                                        file.delete()
                                    }
                                }

                                // Eliminar imágenes de "Stop_Image"
                                db.collection("trips").document(tripID).collection("stops").get()
                                    .addOnSuccessListener { stopsSnapshot ->
                                        for (stopDoc in stopsSnapshot) {
                                            val stopId = stopDoc.id
                                            val stopImagesPath = "Stop_Image/$tripID/$stopId/"
                                            val stopImagesRef =
                                                storage.reference.child(stopImagesPath)
                                            stopImagesRef.listAll()
                                                .addOnSuccessListener { listResult ->
                                                    for (file in listResult.items) {
                                                        file.delete()
                                                    }
                                                }
                                        }
                                    }.addOnFailureListener { e ->
                                        Log.e(
                                            "DeleteTrip",
                                            "Error al cargar las paradas: ${e.message}"
                                        )
                                    }

                                // Eliminar documento del viaje
                                db.collection("trips").document(tripID).delete()
                                    .addOnSuccessListener {
                                    }.addOnFailureListener { e ->
                                        Log.e(
                                            "DeleteTrip",
                                            "Error al eliminar el viaje: ${e.message}"
                                        )
                                    }
                            }

                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(
                            "Error",
                            "Error al buscar sus viajes: ${e.message}"
                        )
                    }


                // Paso 2: Eliminar al usuario de la colección "members"
                db.collection("members").whereEqualTo("userID", userId).get()
                    .addOnSuccessListener { membersSnapshot ->
                        if (!membersSnapshot.isEmpty) {
                            for (memberDoc in membersSnapshot) {
                                db.collection("members").document(memberDoc.id).delete()
                                    .addOnFailureListener { e ->
                                        Log.e("Error", "Error al eliminar miembro: ${e.message}")
                                    }
                            }
                        }
                    }.addOnFailureListener { e ->
                        Log.e("Error", "Error al consultar miembros: ${e.message}")
                    }

                // Paso 3: Eliminar la imagen de perfil
                val profileImageRef = storage.reference.child("UserProfile/$userId")
                profileImageRef.delete().addOnFailureListener { e ->
                    Log.e("Error", "Error al eliminar imagen de perfil: ${e.message}")
                }

                // Paso 4: Eliminar documento del usuario y su cuenta de autenticación
                db.collection("users").document(userId).delete().addOnSuccessListener {
                    FirebaseAuth.getInstance().currentUser?.delete()
                        ?.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(
                                    context,
                                    "Cuenta eliminada correctamente.",
                                    Toast.LENGTH_SHORT
                                ).show()
                                startActivity(Intent(activity, LoginActivity::class.java))
                                activity?.finish()
                            }
                        }!!.addOnFailureListener { e ->
                            Log.e(
                                "Error",
                                "Error al eliminar usuario en Firebase Auth: ${e.message}"
                            )
                        }
                }.addOnFailureListener { e ->
                    Log.e("Error", "Error al eliminar documento del usuario: ${e.message}")
                }
            }

            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.create().show()
        }

        // Eliminar Imágen de Perfil
        iv_delete.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(
                requireContext(), R.style.CustomDialogTheme
            )
            builder.setTitle("Confirmación")
            builder.setMessage("¿Está seguro que quiere eliminar su imagen? Esta acción no se puede deshacer.")

            builder.setPositiveButton("Eliminar") { dialog, _ ->
                dialog.dismiss()
                val path = "UserProfile/" + auth.uid
                val referenceStorage = FirebaseStorage.getInstance().getReference(path)
                referenceStorage.delete().addOnSuccessListener {
                    FirebaseFirestore.getInstance().collection("users").document(auth.uid!!)
                        .update("image", null).addOnFailureListener { e ->
                            Toast.makeText(
                                context,
                                "No se ha eliminado su imagen debido a: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnSuccessListener {
                            uri = null
                            Glide.with(this).clear(iv_imagen) // Borra cualquier caché
                            iv_imagen.setImageResource(R.drawable.ic_user_placeholder) // Placeholder predeterminado
                        }
                }.addOnFailureListener { e ->
                    Toast.makeText(
                        context,
                        "No se ha podido eliminar la imagen debido a: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            builder.setNegativeButton("Cancelar") { dialog, _ ->
                dialog.dismiss()
            }

            builder.create().show()
        }

    }

    /**
     * Validar formato de correo electrónico.
     */
    fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    /**
     * Mostrar alertas en pantalla.
     */
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

    /**
     * Actualizar la imagen en Firestore.
     */
    private fun UpdateFirestore(url: String) {
        FirebaseFirestore.getInstance().collection("users").document(auth.uid!!)
            .update("image", url).addOnFailureListener { e ->
                Toast.makeText(
                    context,
                    "No se ha actualizado su imagen debido a: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        FirebaseFirestore.getInstance().collection("users").document(auth.uid!!)
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    context,
                    "Permiso denegado. Vuelva a intentarlo.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imagePickerHelper.handleGalleryResult(result.data)
            }
        }

    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imagePickerHelper.handleCameraResult()
            }
        }

}