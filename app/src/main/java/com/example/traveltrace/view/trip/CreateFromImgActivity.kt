package com.example.traveltrace.view.trip

import com.example.traveltrace.helper.ImagePickerHelper
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.view.adapters.ImageAdapter
import com.example.traveltrace.view.adapters.StopAdapter
import com.example.traveltrace.viewmodel.StopViewModel
import java.util.Calendar

/**
 * Actividad para crear un viaje a partir de imágenes seleccionadas.
 * Permite cargar imágenes, extraer información de ubicación y detalles de los puntos de interés (stops),
 * y almacenarlos en Firebase Firestore.
 */
class CreateFromImgActivity : AppCompatActivity() {

    // Variables de vistas
    private lateinit var rv_images:       RecyclerView     // RecyclerView para mostrar imágenes seleccionadas
    private lateinit var img_adapter:     ImageAdapter     // Adaptador para las imágenes
    private lateinit var imagesList:      ArrayList<String> // Lista de rutas de imágenes seleccionadas
    private lateinit var rv_stops:        RecyclerView     // RecyclerView para mostrar los stops detectados
    private lateinit var stopViewModel:   StopViewModel    // ViewModel para manejar la lógica de los stops
    private lateinit var stopAdapter:     StopAdapter      // Adaptador para los stops
    private lateinit var btn_save:        Button           // Botón para guardar los stops
    private lateinit var btn_upload:      Button           // Botón para subir imágenes
    private lateinit var btn_process:     Button           // Botón para procesar las imágenes seleccionadas
    private lateinit var sv_images:       ScrollView       // ScrollView para contener las imágenes seleccionadas
    private lateinit var tv_instructions: TextView         // Texto de instrucciones iniciales
    private lateinit var tv_instructions2: TextView        // Texto de instrucciones secundarias
    private lateinit var tripID:          String           // ID del viaje
    private lateinit var pb_save:         ProgressBar      // Barra de progreso para guardar los stops
    private lateinit var db:              FirebaseFirestore // Instancia de Firebase Firestore

    // Helper
    private lateinit var imagePickerHelper: ImagePickerHelper // Ayudante para manejar la selección de imágenes

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_from_img)
        init() // Inicializar vistas y variables

        // Inicializar ImagePickerHelper para manejar imágenes
        imagePickerHelper = ImagePickerHelper(
            context = this,
            singleImageMode = false, // Permitir múltiples imágenes
            onImagePicked = { uris ->
                // Agregar imágenes seleccionadas a la lista
                if (uris.isNotEmpty()) {
                    imagesList.addAll(uris.map { it.toString() })
                    img_adapter.notifyDataSetChanged()
                    btn_process.visibility = View.VISIBLE // Mostrar botón de procesar
                }
            }
        )

        // Configuración del botón para subir imágenes
        btn_upload.setOnClickListener {
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = null,
                permissionLauncher = permissionLauncher
            )
        }

        // Procesar imágenes
        btn_process.setOnClickListener {
            // Obtener la API Key de Google Places
            val apiKey = applicationContext.packageManager.getApplicationInfo(
                applicationContext.packageName, PackageManager.GET_META_DATA
            ).metaData.getString("com.google.android.geo.API_KEY").toString()

            // Inicializar Google Places si no está inicializado
            if (!Places.isInitialized()) {
                Places.initialize(applicationContext, apiKey)
            }

            // Procesar cada imagen seleccionada
            for (uri in imagesList) {
                stopViewModel.addStopFromUri(
                    uri.toUri(), contentResolver, apiKey, Places.createClient(this)
                )
            }

            stopViewModel.stops.observe(this) { stops ->
                if (!stops.isNullOrEmpty()) {
                    btn_process.visibility = View.GONE
                    // Actualizar el adaptador de stops
                    stopAdapter.updateStopList(stops)
                    // Actualizar la UI
                    tv_instructions.visibility = View.GONE
                    sv_images.visibility = View.GONE
                    btn_upload.visibility = View.GONE
                    tv_instructions2.visibility = View.VISIBLE
                    btn_save.visibility = View.VISIBLE
                }
            }

            // Manejo de errores al procesar imágenes
            stopViewModel.error.observe(this) { errorMessage ->
                errorMessage?.let {
                    Toast.makeText(
                        this,
                        "No se encontraron datos en las imágenes.",
                        Toast.LENGTH_LONG
                    ).show()
                    // Restablecer la UI para permitir subir más imágenes
                    imagesList.clear()
                    img_adapter.notifyDataSetChanged()
                    sv_images.visibility = View.VISIBLE
                    btn_upload.visibility = View.VISIBLE
                    btn_process.visibility = View.GONE
                }
            }
        }

        // Guardar
        btn_save.setOnClickListener {
            pb_save.visibility = View.VISIBLE
            btn_save.visibility = View.GONE
            for (stop in stopViewModel.stops.value!!) {
                val hm_stop = hashMapOf(
                    "name" to stop.name,
                    "text" to stop.text,
                    "timestamp" to stop.timestamp,
                    "idPlace" to stop.idPlace,
                    "namePlace" to stop.namePlace,
                    "addressPlace" to stop.addressPlace,
                    "geoPoint" to stop.geoPoint
                )

                // Guardar stop en Firebase Firestore
                db.collection("trips").document(tripID).collection("stops").add(hm_stop)
                    .addOnSuccessListener { documentReference ->
                        Toast.makeText(
                            applicationContext, "Se ha registrado con éxito", Toast.LENGTH_SHORT
                        ).show()

                        // Subir imagen al almacenamiento
                        val imagePath = "Stop_Image/$tripID/${documentReference.id}/${System.currentTimeMillis()}"
                        val referenceStorage = FirebaseStorage.getInstance().getReference(imagePath)
                        referenceStorage.putFile(stop.photos!![0].toUri())
                            .addOnSuccessListener { task ->
                                task.storage.downloadUrl.addOnSuccessListener { uri ->
                                    UpdateFirestore(uri.toString(), documentReference.id)
                                }
                            }.addOnFailureListener { e ->
                                pb_save.visibility = View.GONE
                                btn_save.visibility = View.VISIBLE
                                Toast.makeText(applicationContext, "No se ha podido subir la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
                            }

                        //Fecha de Inicio del Viaje Portada
                        val calendar = Calendar.getInstance()
                        calendar.set(9999, Calendar.DECEMBER, 31, 23, 59, 59) // Año 9999, último día
                        calendar.set(Calendar.MILLISECOND, 999) // Último milisegundo
                        val endDate = Timestamp(calendar.time)

                        db.collection("trips").document(tripID).get().addOnCompleteListener {
                            val initDate =
                                if (it.result.get("initDate") == null) endDate else it.result.toObject(Trip::class.java)!!.initDate

                            if (initDate!! > stop.timestamp!!) {
                                db.collection("trips").document(tripID)
                                    .update("initDate", stop.timestamp).addOnFailureListener { ex ->
                                        Toast.makeText(
                                            applicationContext,
                                            "No se actualizó la fecha de inicio: ${ex.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }
                    }.addOnFailureListener { e ->
                        pb_save.visibility = View.GONE
                        btn_save.visibility = View.VISIBLE
                        Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
            }
        }
    }

    /**
     * Inicializa vistas y adaptadores, y configura el RecyclerView para imágenes y stops.
     */
    private fun init() {
        // Inicializar vistas
        tv_instructions = findViewById(R.id.tv_instructions)
        tv_instructions2 = findViewById(R.id.tv_instructions2)
        btn_upload = findViewById(R.id.btn_upload)
        btn_save = findViewById(R.id.btn_save)
        pb_save = findViewById(R.id.pb_save)
        btn_process = findViewById(R.id.btn_process)
        sv_images = findViewById(R.id.sv_images)
        rv_images = findViewById(R.id.rv_images)
        imagesList = ArrayList()
        img_adapter = ImageAdapter(imagesList) { imageUrl ->
            imagesList.remove(imageUrl)
            img_adapter.notifyDataSetChanged()
        }

        // Configuración del RecyclerView para imágenes
        val layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rv_images.layoutManager = layoutManager
        rv_images.adapter = img_adapter

        // Configuración del RecyclerView para stops
        rv_stops = findViewById(R.id.rv_stops)
        rv_stops.layoutManager = LinearLayoutManager(this)
        //rv_stops.setHasFixedSize(true)
        stopAdapter = StopAdapter(isClickable = false)
        rv_stops.adapter = stopAdapter

        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)

        // Obtener el ID del viaje desde el intent
        tripID = intent.getStringExtra("tripID").toString()
        db = FirebaseFirestore.getInstance()
    }

    /**
     * Actualiza Firestore con la URL de una imagen subida.
     */
    private fun UpdateFirestore(url: String, stopID: String) {
        val photo = hashMapOf(
            "url" to url
        )
        FirebaseFirestore.getInstance().collection("trips")
            .document(tripID).collection("stops").document(stopID)
            .collection("photos").add(photo)
            .addOnSuccessListener {
                Toast.makeText(
                    applicationContext,
                    "Se ha actualizado su imagen con éxito",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    applicationContext,
                    "No se ha actualizado su imagen debido a: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
    }

    // Manejo de permisos, galería y cámara
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
}
