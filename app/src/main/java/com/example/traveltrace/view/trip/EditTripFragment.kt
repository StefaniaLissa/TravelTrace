package com.example.traveltrace.view.trip

import com.example.traveltrace.helper.ImagePickerHelper
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity.RESULT_OK
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.traveltrace.R
import com.example.traveltrace.viewmodel.TripViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

/**
 * Fragmento para editar un viaje existente.
 * Permite actualizar el nombre del viaje, imagen de portada y configuración de visibilidad.
 */
class EditTripFragment : Fragment() {

    // Vistas del layout
    private lateinit var iv_cover: ImageView         // Imagen de portada del viaje
    private lateinit var btn_new_image: Button      // Botón para agregar una nueva imagen
    private lateinit var iv_delete: ImageView       // Botón para eliminar la imagen de portada
    private lateinit var et_name: EditText          // Campo para editar el nombre del viaje
    private lateinit var cb_online: CheckBox        // Checkbox para marcar el viaje como público
    private lateinit var btn_create: Button         // Botón para guardar los cambios

    // Variables de Firebase
    private lateinit var db: FirebaseFirestore      // Referencia a Firestore
    private var uri: Uri? = null                    // URI de la nueva imagen seleccionada
    private var uriString: String? = null           // Cadena de la URI seleccionada
    private lateinit var user: FirebaseUser         // Usuario actual
    private lateinit var viewModel: TripViewModel   // ViewModel para manejar datos del viaje
    private lateinit var tripID: String             // ID del viaje
    private var originalImageUrl: String? = null    // URL original de la imagen de portada

    // Ayudante para selección de imágenes
    private lateinit var imagePickerHelper: ImagePickerHelper

    /**
     * Infla la vista y enlaza las vistas del layout con sus variables.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_trip, container, false)
        iv_cover = view.findViewById(R.id.iv_cover)
        btn_new_image = view.findViewById(R.id.btn_new_image)
        iv_delete = view.findViewById(R.id.iv_delete)
        et_name = view.findViewById(R.id.et_name)
        cb_online = view.findViewById(R.id.cb_online)
        btn_create = view.findViewById(R.id.btn_create)
        db = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance().currentUser!!
        uriString = String()

        // Obtener ID del viaje desde los argumentos
        tripID = arguments?.getString("trip")!!

        // Configurar el ViewModel
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(tripID)

        return view
    }

    /**
     * Configura los elementos del fragmento al ser creado.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa el ayudante para la selección de imágenes
        imagePickerHelper = ImagePickerHelper(
            context = requireContext(),
            singleImageMode = true, // Solo una imagen
            onImagePicked = { uris ->
                if (uris.isNotEmpty()) {
                    uri = uris[0] // Establecer URI de la nueva imagen
                    updateCoverImage(uri) // Actualizar la imagen de portada
                    iv_delete.isEnabled = true
                    iv_delete.alpha = 1.0f
                }
            })

        // Configura el observador del ViewModel para cargar datos del viaje
        viewModel.trip.observe(viewLifecycleOwner) {
            if (it != null) {
                et_name.setText(it.name) // Cargar el nombre del viaje
                if (it.image != null) {
                    originalImageUrl = it.image // Guardar URL original
                    iv_cover.contentDescription = it.image
                    Glide.with(this).load(it.image).placeholder(R.drawable.ic_downloading)
                        .error(R.drawable.ic_error).centerCrop().into(iv_cover)
                    iv_delete.isEnabled = true
                    iv_delete.alpha = 1.0f
                } else {
                    iv_delete.isEnabled = false
                    iv_delete.alpha = 0.5f
                }
                cb_online.isChecked = it.public!!
                viewModel.trip.removeObservers(viewLifecycleOwner) // Elimina observadores para evitar duplicados
            }
        }

        // Configurar clic en imagen para cambiarla
        iv_cover.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        // Configurar clic en botón de nueva imagen
        btn_new_image.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        // Configurar clic en botón para eliminar imagen
        iv_delete.setOnClickListener {
            clearCoverImage()
        }

        // Configurar clic en botón para guardar cambios
        btn_create.setOnClickListener {
            updateTrip()
        }
    }

    /**
     * Actualiza los datos del viaje en Firestore.
     * Incluye nombre, visibilidad y manejo de la imagen de portada.
     */
    private fun updateTrip() {
        val trip = hashMapOf(
            "name" to et_name.text.toString(),
            "public" to cb_online.isChecked
        )

        // Actualizar datos del viaje
        val docRef = db.collection("trips").document(tripID)
        docRef.update(trip as Map<String, Any>).addOnSuccessListener {
            uri?.let { imageUri -> uploadCoverImage(imageUri.toString()) } // Subir nueva imagen

            // Eliminar imagen anterior si fue borrada
            if (originalImageUrl != null && uri == null) {
                deleteImageFromStorage(originalImageUrl!!)
            }

            Toast.makeText(context, "Viaje actualizado correctamente", Toast.LENGTH_SHORT).show()
            parentFragmentManager.beginTransaction().remove(this).commit()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Elimina la imagen de portada del almacenamiento de Firebase.
     */
    private fun deleteImageFromStorage(imageUrl: String) {
        val referenceStorage = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)
        referenceStorage.delete().addOnSuccessListener {
            db.collection("trips").document(tripID).update("image", null) // Actualiza Firestore
            Toast.makeText(context, "Imagen eliminada correctamente", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener { e ->
            Toast.makeText(context, "Error al eliminar la imagen: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Sube una nueva imagen de portada al almacenamiento de Firebase.
     */
    private fun uploadCoverImage(imageUri: String) {
        val rutaImagen = "TripCover/$tripID/${System.currentTimeMillis()}"
        val referenceStorage = FirebaseStorage.getInstance().getReference(rutaImagen)
        referenceStorage.putFile(imageUri.toUri()).addOnSuccessListener { task ->
            task.storage.downloadUrl.addOnSuccessListener { uri ->
                db.collection("trips").document(tripID).update("image", uri.toString())
            }
        }.addOnFailureListener { e ->
            Toast.makeText(context, "No se pudo subir la imagen: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        }
    }

    /**
     * Muestra un diálogo de confirmación para eliminar la imagen de portada.
     * Si el usuario confirma, se elimina la imagen actual.
     */
    private fun clearCoverImage() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(
            requireContext(), R.style.CustomDialogTheme
        )
        builder.setTitle("Confirmación")
        builder.setMessage("¿Está seguro de que desea eliminar la imagen? Esta acción no se puede deshacer.")

        builder.setPositiveButton("Eliminar") { dialog, _ ->
            dialog.dismiss()

            // Restablece el estado de la imagen a su valor por defecto
            uri = null
            iv_cover.setImageResource(R.drawable.ic_cover_background) // Imagen por defecto
            iv_cover.contentDescription = null

            // Si hay una imagen guardada en Firestore, la elimina
            if (!originalImageUrl.isNullOrEmpty()) {
                deleteImageFromStorage(originalImageUrl!!) // Elimina la imagen de Firebase Storage
                originalImageUrl = null // Restablece el estado del URL original
            }

            // Desactiva el botón de eliminación de imagen
            iv_delete.isEnabled = false
            iv_delete.alpha = 0.5f
        }

        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss() // Cierra el diálogo sin realizar ninguna acción
        }

        builder.create().show() // Muestra el diálogo al usuario
    }

    /**
     * Actualiza la imagen de portada con el URI proporcionado.
     * Cambia la imagen en la vista `ImageView`.
     *
     * @param uri URI de la nueva imagen que se mostrará como portada.
     */
    private fun updateCoverImage(uri: Uri?) {
        iv_cover.setImageURI(uri) // Establece la imagen proporcionada en el ImageView
    }

    /**
     * Solicita permisos para acceder a la cámara o galería.
     * Si el permiso es denegado, muestra un mensaje de error.
     */
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

    /**
     * Inicia un intent para seleccionar una imagen de la galería.
     * Si la operación se completa con éxito, maneja el resultado a través del `ImagePickerHelper`.
     */
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imagePickerHelper.handleGalleryResult(result.data)
            }
        }

    /**
     * Inicia un intent para capturar una imagen con la cámara.
     * Si la operación se completa con éxito, maneja el resultado a través del `ImagePickerHelper`.
     */
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imagePickerHelper.handleCameraResult()
            }
        }
}