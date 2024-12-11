package com.example.traveltrace.view.trip

import com.example.traveltrace.helper.ImagePickerHelper
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import com.example.traveltrace.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.traveltrace.view.tripExtra.MediumActivity

/**
 * Actividad para crear un viaje nuevo.
 * Permite seleccionar una imagen de portada, definir detalles del viaje (nombre, visibilidad),
 * y guardar los datos en Firebase Firestore.
 */
class CreateTripActivity : AppCompatActivity() {

    // Variables de vistas
    private lateinit var iv_cover:         ImageView       // Imagen de portada del viaje
    private lateinit var btn_new_image:    Button          // Botón para cambiar la imagen de portada
    private lateinit var iv_delete:        ImageView       // Botón para eliminar la imagen de portada
    private lateinit var et_name:          EditText        // Campo de texto para el nombre del viaje
    private lateinit var cb_online:        CheckBox        // CheckBox para definir si el viaje es público
    private lateinit var cb_share:         CheckBox        // CheckBox para definir si se compartirá el viaje
    private lateinit var btn_create:       Button          // Botón para crear el viaje
    private lateinit var db:               FirebaseFirestore // Instancia de Firebase Firestore
    private var uri:                       Uri? = null     // URI de la imagen seleccionada
    private lateinit var user:             FirebaseUser    // Usuario actual
    private lateinit var tripId:           String          // ID generado para el viaje

    // Helper
    private lateinit var imagePickerHelper: ImagePickerHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_trip)
        init() // Inicializar vistas y variables

        // Inicializar ImagePickerHelper
        imagePickerHelper = ImagePickerHelper(
            context = this,
            singleImageMode = true, // Solo se permite seleccionar una imagen para la portada
            onImagePicked = { uris ->
                // Manejo de imagen seleccionada
                if (uris.isNotEmpty() && uris[0].toString() != "") {
                    uri = uris[0]
                    iv_cover.setImageURI(uri)
                }
                iv_delete.isEnabled = true
                iv_delete.alpha = 1.0f // Habilitar botón de eliminar imagen
            }
        )

        // Cambiar la imagen
        btn_new_image.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        // Permitir cambiar la imagen tocando directamente en la portada
        iv_cover.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        // Crear el viaje
        btn_create.setOnClickListener {
            createTrip()
        }

        // Eliminar la imagen
        iv_delete.setOnClickListener {
            uri = null
            iv_cover.setImageURI(null)
            iv_delete.isEnabled = false
            iv_delete.alpha = 0.5f // Deshabilitar botón de eliminar
        }
    }

    /**
     * Método para crear un viaje y guardar los datos en Firestore.
     */
    private fun createTrip() {
        val trip = hashMapOf(
            "name" to et_name.text.toString(),
            "public" to cb_online.isChecked
        )

        // Guardar en la colección "trips" de Firestore
        db.collection("trips").add(trip).addOnSuccessListener { documentReference ->
            Toast.makeText(
                applicationContext, "Se ha registrado con éxito", Toast.LENGTH_SHORT
            ).show()

            tripId = documentReference.id // Guardar el ID generado para el viaje

            // Agregar al usuario actual como administrador del viaje
            val member = hashMapOf(
                "admin" to true, "tripID" to documentReference.id, "userID" to user.uid
            )
            db.collection("members").add(member)

            // Subir la imagen de portada, si existe
            if (uri != null) {
                uploadCoverImage(uri.toString())
            }

            // Caso de compartir el viaje
            if (cb_share.isChecked) {
                openShareTripFragment()
            } else {
                // Ir directamente a la actividad de detalle del viaje
                val intent = Intent(this@CreateTripActivity, DetailedTripActivity::class.java)
                intent.putExtra("id", tripId)
                startActivity(intent)
            }

            // Asegurar que los cambios en Firestore están guardados antes de finalizar
            db.waitForPendingWrites().addOnCompleteListener {
                finish()
            }

        }.addOnFailureListener { e ->
            Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Método para abrir la actividad de compartir viaje.
     */
    private fun openShareTripFragment() {
        val intent = Intent(this, MediumActivity::class.java)
        intent.putExtra("view", "share")
        intent.putExtra("tripId", tripId)
        startActivity(intent)
    }

    /**
     * Método para subir la imagen de portada a Firebase Storage.
     */
    private fun uploadCoverImage(imageUri: String) {
        val rutaImagen = "TripCover/" + tripId + "/" + System.currentTimeMillis()
        val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)

        // Subir la imagen
        referenceStorage.putFile(imageUri.toUri())
            .addOnSuccessListener { task ->
                task.storage.downloadUrl
                    .addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        db.collection("trips").document(tripId).update("image", imageUrl)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    applicationContext,
                                    "Imagen de portada subida con éxito.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    applicationContext,
                                    "Error al guardar la URL de la imagen: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            applicationContext,
                            "Error al obtener la URL de descarga: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    applicationContext,
                    "No se ha podido subir la imagen: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    /**
     * Inicializar vistas y configurar valores iniciales.
     */
    private fun init() {
        iv_cover = findViewById(R.id.iv_cover)
        btn_new_image = findViewById(R.id.btn_new_image)
        et_name = findViewById(R.id.et_name)
        cb_online = findViewById(R.id.cb_online)
        cb_share = findViewById(R.id.cb_share)
        btn_create = findViewById(R.id.btn_create)
        iv_delete = findViewById(R.id.iv_delete)
        db = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance().currentUser!!

        // Configurar estado inicial del CheckBox "cb_online"
        db.collection("users").document(user.uid).get().addOnSuccessListener { document ->
            if (document.exists()) {
                val isPublic = document.getBoolean("public") ?: false
                cb_online.isChecked = isPublic
            }
        }

        // Deshabilitar botón de eliminar imagen inicialmente
        iv_delete.isEnabled = false
        iv_delete.alpha = 0.5f
    }

    // Configuración de permisos y lanzadores para galería/cámara
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(this, "Permiso denegado. Vuelva a intentarlo.", Toast.LENGTH_SHORT).show()
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
