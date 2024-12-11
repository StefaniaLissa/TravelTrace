package com.example.traveltrace.view.stop

import com.example.traveltrace.helper.ImagePickerHelper
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Stop
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.view.adapters.ImageAdapter
import com.example.traveltrace.viewmodel.StopViewModel
import java.text.SimpleDateFormat
import java.util.*

/**
 * Actividad para gestionar la creación y edición de puntos de interés (Stops) en un viaje.
 *
 * Esta actividad permite al usuario:
 * - Crear un nuevo punto de interés con información como nombre, descripción, fecha, hora, lugar y fotos.
 * - Editar un punto de interés existente, con control de edición para evitar conflictos entre usuarios.
 * - Subir nuevas imágenes asociadas al punto de interés y eliminar las existentes.
 * - Actualizar las fechas del viaje si se modifican los timestamps de los puntos.
 *
 * Funcionalidades principales:
 * - Integración con Google Places API para seleccionar ubicaciones.
 * - Uso de Firebase Firestore para gestionar los datos del viaje y los puntos.
 * - Subida y eliminación de imágenes en Firebase Storage.
 * - Manejo de estado de edición para evitar conflictos.
 */
class PostStopActivity : AppCompatActivity() {

    companion object {
        const val MODE_CREATE = "CREATE" // Constante para el modo de creación
        const val MODE_EDIT = "EDIT"    // Constante para el modo de edición
    }

    // Vistas y variables necesarias
    private lateinit var etName:          EditText           // Campo de texto para el nombre del punto
    private lateinit var etDescription:   EditText           // Campo de texto para la descripción del punto
    private lateinit var btnUpload:       Button             // Botón para cargar imágenes
    private lateinit var rvImages:        RecyclerView       // RecyclerView para mostrar imágenes
    private lateinit var tvDate:          TextView           // Muestra la fecha del punto
    private lateinit var tvTime:          TextView           // Muestra la hora del punto
    private lateinit var fabDone:         FloatingActionButton // Botón de acción para guardar el punto
    private lateinit var sv_images:       ScrollView         // ScrollView para las imágenes
    private lateinit var pb_load:         ProgressBar        // Barra de progreso para carga de datos

    // Firebase
    private lateinit var db:              FirebaseFirestore  // Referencia a Firestore
    private var imagesList:               ArrayList<String> = ArrayList() // Lista de imágenes del punto
    private var newImages:                MutableList<String> = mutableListOf() // Imágenes nuevas para subir
    private var imagesToDelete:           MutableList<String> = mutableListOf() // Imágenes para eliminar

    // Otras variables necesarias
    private lateinit var adapter:         ImageAdapter       // Adaptador para el RecyclerView de imágenes
    private lateinit var layoutManager:   GridLayoutManager  // LayoutManager para el RecyclerView
    private lateinit var mode:            String             // Modo de la actividad: creación o edición
    private lateinit var tripID:          String             // ID del viaje asociado al punto
    private var stopID:                   String? = null     // ID del punto de interés (en edición)
    private var stop:                     Stop? = null       // Objeto del punto de interés actual
    private var timestampFb:              Timestamp = Timestamp(Date()) // Timestamp del punto
    private var geoPoint:                 GeoPoint = GeoPoint(0.0, 0.0) // Coordenadas del punto
    private var isEditMode:               Boolean = false    // Flag para determinar si estamos en modo edición
    private var calendar = Calendar.getInstance()            // Instancia de Calendar para manejar fechas y horas

    // Helper para manejo de imágenes
    private lateinit var imagePickerHelper: ImagePickerHelper // Helper para seleccionar imágenes

    // Variables para Google Places
    private var placeFragment:            AutocompleteSupportFragment? = null // Fragmento para autocompletar lugares
    private var idPlace:                  String = ""       // ID del lugar seleccionado
    private var namePlace:                String = ""       // Nombre del lugar seleccionado
    private var addressPlace:             String = ""       // Dirección del lugar seleccionado


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_post_stop)
        init()

        isEditMode = intent.getBooleanExtra("isEditMode", false) // Verifica si estamos en modo edición
        tripID = intent.getStringExtra("tripID") ?: ""                      // Obtiene el ID del viaje desde el intent
        stopID = intent.getStringExtra("stopID") ?: ""                      // Obtiene el ID del punto desde el intent
        val userName = FirebaseAuth.getInstance().currentUser?.email ?: ""        // Obtiene el email del usuario actual

        if (isEditMode) {
            mode = MODE_EDIT  // Establece el modo a edición
            checkEditingState(tripID, stopID!!, userName)   // Verifica el estado de edición
            loadStopData(tripID, stopID!!)                  // Carga los datos del punto para edición
        } else {
            mode = MODE_CREATE // Establece el modo a creación
            setupForNewStop()  // Configura la actividad para crear un nuevo punto
        }

        // Inicializa el helper para seleccionar imágenes
        imagePickerHelper = ImagePickerHelper(
            context = this,
            singleImageMode = false,    // Permitir múltiples imágenes
            onImagePicked = { uris ->   // Callback para manejar imágenes seleccionadas
                uris.forEach { uri ->
                    if (!imagesList.contains(uri.toString())) { // Evita duplicados
                        newImages.add(uri.toString())   // Añade a la lista de nuevas imágenes
                        imagesList.add(uri.toString())  // Añade a la lista general de imágenes
                    }
                }
                sv_images.isVisible = imagesList.isNotEmpty() // Muestra el ScrollView si hay imágenes
                adapter.notifyDataSetChanged() // Notifica al adaptador de cambios
            }
        )
    }

    /**
     * Verifica el estado de edición de un punto de interés (Stop) y reserva el punto para el usuario actual.
     *
     * Si otro usuario está editando el punto, se mostrará un mensaje y se finalizará la actividad.
     * Si nadie está editando, se actualizará el estado para indicar que el usuario actual lo está editando.
     *
     * @param tripID El ID del viaje al que pertenece el punto.
     * @param stopID El ID del punto de interés.
     * @param userName El nombre del usuario actual (email).
     */
    private fun checkEditingState(tripID: String, stopID: String, userName: String) {
        // Referencia al documento del punto de interés en Firestore
        val stopDoc = db.collection("trips").document(tripID).collection("stops").document(stopID)

        // Intentar obtener el estado de edición del punto
        stopDoc.get().addOnSuccessListener { document ->
            val editingUser = document.getString("editing") // Verifica si alguien está editando el punto
            if (editingUser != null && editingUser != userName) {
                // Caso: Otro usuario está editando el punto
                Toast.makeText(
                    this, "Esta parada está siendo editada por $editingUser.", Toast.LENGTH_LONG
                ).show()
                finish() // Cierra la actividad para evitar conflictos
            } else {
                // Caso: Nadie está editando, actualizar el campo para indicar que lo edita el usuario actual
                stopDoc.update("editing", userName).addOnSuccessListener {
                    loadStopData(tripID, stopID)
                    Toast.makeText(
                        this, "Punto de Interés reservado para edición.", Toast.LENGTH_LONG
                    ).show()
                }.addOnFailureListener {
                    Toast.makeText(
                        this, "No se pudo establecer el estado de edición.", Toast.LENGTH_LONG
                    ).show()
                    finish()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error al verificar el estado de edición.", Toast.LENGTH_LONG)
                .show()
            finish()
        }
    }

    /**
     *
     * - Configura las vistas de la interfaz.
     * - Establece el adaptador para manejar imágenes, incluyendo eliminación.
     * - Configura el fragmento de Autocomplete de Google Places.
     * - Configura los campos a obtener de Google Places.
     * - Llama al método `setupListeners` para establecer los listeners de eventos en las vistas.
     */
    private fun init() {
        // Vincular vistas con sus IDs en el layout
        etName = findViewById(R.id.et_name)
        etDescription = findViewById(R.id.et_description)
        btnUpload = findViewById(R.id.btn_upload)
        rvImages = findViewById(R.id.rv_images)
        sv_images = findViewById(R.id.sv_images)
        tvDate = findViewById(R.id.tv_date)
        tvTime = findViewById(R.id.tv_time)
        fabDone = findViewById(R.id.fab_done)
        pb_load = findViewById(R.id.pb_load)

        db = FirebaseFirestore.getInstance()

        // Configuración del RecyclerView
        layoutManager = GridLayoutManager(this, 3)  // Layout en rejilla con 3 columnas
        adapter = ImageAdapter(imagesList) { imageUrl -> // Manejar eliminación de imágenes
            if (!newImages.remove(imageUrl)) {          // Si la imagen no está en las nuevas
                imagesToDelete.add(imageUrl)            // Se marca para eliminar
            }
            imagesList.remove(imageUrl)                 // Eliminar de la lista general
            adapter.notifyDataSetChanged()              // Notificar cambios al adaptador
            Toast.makeText(this, "Imagen eliminada", Toast.LENGTH_SHORT).show()
        }
        rvImages.layoutManager = layoutManager          // Asignar LayoutManager
        rvImages.adapter = adapter                      // Asignar adaptador

        // Configuración del fragmento Autocomplete de Google Places
        placeFragment = supportFragmentManager.findFragmentById(R.id.fg_autocomplete)
                as AutocompleteSupportFragment?

        // Personalización de colores del fragmento Autocomplete
        placeFragment!!.view?.findViewById<EditText>(
            com.google.android.libraries.places.R.id.places_autocomplete_search_input
        )?.setTextColor(getResources().getColor(R.color.black))
        placeFragment!!.view?.findViewById<ImageButton>(
            com.google.android.libraries.places.R.id.places_autocomplete_search_button
        )?.setColorFilter(getResources().getColor(R.color.black))
        placeFragment!!.view?.findViewById<ImageButton>(
            com.google.android.libraries.places.R.id.places_autocomplete_clear_button
        )?.setColorFilter(getResources().getColor(R.color.black))

        // Recuperar la API KEY desde los metadatos de la aplicación
        val ai: ApplicationInfo? = applicationContext.packageManager?.getApplicationInfo(
            applicationContext.packageName, PackageManager.GET_META_DATA
        )
        val apiKey = ai?.metaData?.getString("com.google.android.geo.API_KEY").toString()

        // Inicializar Google Places API si no está inicializada
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        // Configurar los campos a obtener de Google Places
        placeFragment!!.setPlaceFields(
            listOf(
                Place.Field.NAME,
                Place.Field.ID,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS
            )
        )

        // Configurar los listeners de eventos para las vistas
        setupListeners()
    }

    /**
     * Configura la interfaz y los valores iniciales para crear un nuevo punto de interés.
     */
    private fun setupForNewStop() {
        // Inicializa el calendario con la fecha y hora actuales.
        calendar = Calendar.getInstance()

        // Formatea y muestra la fecha actual en el TextView correspondiente.
        tvDate.text = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault()).format(calendar.timeInMillis)

        // Formatea y muestra la hora actual en el TextView correspondiente.
        tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.timeInMillis)

        // Establece el timestamp global con la fecha y hora actuales.
        timestampFb = Timestamp(calendar.time)
    }

    /**
     * Configura los listeners para los elementos interactivos de la actividad.
     */
    private fun setupListeners() {

        // Configura el listener para el fragmento de Google Places.
        placeFragment!!.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                // Guarda los datos seleccionados del lugar.
                idPlace = place.id!!.toString()
                namePlace = place.name!!.toString()
                addressPlace = place.address!!.toString()
                geoPoint = GeoPoint(place.latLng!!.latitude, place.latLng!!.longitude)
            }

            override fun onError(status: Status) {
                Toast.makeText(applicationContext, "Ocurrió un error al seleccionar el lugar.", Toast.LENGTH_SHORT).show()
            }
        })

        // Configura el DatePicker para seleccionar la fecha.
        tvDate.setOnClickListener {
            // Obtiene los valores actuales de año, mes y día.
            val currentYear = calendar.get(Calendar.YEAR)
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                R.style.CustomDatePickerTheme,
                { _, year, month, day ->
                    // Actualiza el calendario con la fecha seleccionada.
                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, day)

                    // Muestra la fecha seleccionada en el TextView correspondiente.
                    tvDate.text = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault())
                        .format(calendar.time)

                    // Actualiza el timestamp global con la nueva fecha.
                    timestampFb = Timestamp(calendar.time)
                },
                currentYear, currentMonth, currentDay
            )
            datePickerDialog.show()
        }

        // Configura el TimePicker para seleccionar la hora.
        tvTime.setOnClickListener {
            // Obtiene los valores actuales de hora y minuto.
            val currentHour = calendar.get(Calendar.HOUR_OF_DAY)
            val currentMinute = calendar.get(Calendar.MINUTE)

            TimePickerDialog(
                this,
                R.style.CustomTimePickerTheme,
                { _, hour, minute ->
                    // Actualiza el calendario con la hora seleccionada.
                    calendar.set(Calendar.HOUR_OF_DAY, hour)
                    calendar.set(Calendar.MINUTE, minute)

                    // Muestra la hora seleccionada en el TextView correspondiente.
                    tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

                    // Actualiza el timestamp global con la nueva hora.
                    timestampFb = Timestamp(calendar.time)
                },
                currentHour, currentMinute,
                true // Define el formato de 24 horas.
            ).show()
        }

        // Subir imágenes.
        btnUpload.setOnClickListener {
            // Abre el diálogo para seleccionar imágenes desde la galería o cámara.
            imagePickerHelper.showImagePickerDialog(
                galleryLauncher = galleryLauncher,
                cameraLauncher = cameraLauncher,
                permissionLauncher = permissionLauncher
            )
        }

        // Guardar o actualizar el punto de interés.
        fabDone.setOnClickListener {
            pb_load.visibility = View.VISIBLE

            // Verifica el modo de la actividad y realiza la acción correspondiente.
            if (mode == MODE_CREATE) {
                createStop() // Crea un nuevo punto de interés.
            } else if (mode == MODE_EDIT) {
                updateStop() // Actualiza un punto de interés existente.
            }

            updateTrip() // Actualiza las fechas del viaje asociado.

            pb_load.visibility = View.GONE
            db.waitForPendingWrites().addOnCompleteListener {
                finish()
            }
        }
    }

    /**
     * Actualiza las fechas asociadas al viaje (initDate, endDate) y calcula la duración en días.
     * Para que las fechas del viaje reflejen el rango de tiempo
     * en el que ocurren los puntos de interés.
     */
    private fun updateTrip() {
        // Obtiene el documento del viaje desde Firestore.
        db.collection("trips").document(tripID).get().addOnSuccessListener { document ->
            val trip = document.toObject(Trip::class.java)

            // Crear una fecha máxima válida utilizando Calendar.
            val maxDate = Calendar.getInstance().apply {
                set(9999, Calendar.DECEMBER, 31, 23, 59, 59) // Año 9999, mes diciembre, último segundo del día.
            }.time

            // Obtener las fechas actuales del viaje o asignar valores por defecto.
            val currentInitDate = trip?.initDate ?: Timestamp(maxDate)
            val currentEndDate = trip?.endDate ?: Timestamp(Date(0, 1, 1)) // Fecha mínima.

            var newInitDate = currentInitDate // Nueva fecha de inicio.
            var newEndDate = currentEndDate   // Nueva fecha de fin.

            // Actualizar las fechas si el timestamp del punto actual las excede.
            if (timestampFb.toDate().before(currentInitDate.toDate())) {
                newInitDate = timestampFb
            }
            if (timestampFb.toDate().after(currentEndDate.toDate())) {
                newEndDate = timestampFb
            }

            // Calcular la duración del viaje en días.
            val durationInDays =
                ((newEndDate.seconds - newInitDate.seconds) / (60 * 60 * 24)).toInt()

            // Actualizar las fechas y la duración en Firestore.
            db.collection("trips").document(tripID).update(
                mapOf(
                    "initDate" to newInitDate,
                    "endDate" to newEndDate,
                    "duration" to durationInDays
                )
            ).addOnSuccessListener {
                // Notificar al usuario que las fechas y duración han sido actualizadas.
                Toast.makeText(
                    applicationContext, "Fechas y duración actualizadas", Toast.LENGTH_SHORT
                ).show()
            }.addOnFailureListener { ex ->
                // Manejar errores en caso de fallo al actualizar los datos.
                Toast.makeText(
                    applicationContext,
                    "Error al actualizar fechas: ${ex.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


    /**
     * Carga los datos de un punto de interés específico desde Firestore y actualiza la interfaz.
     *
     * @param tripID ID del viaje al que pertenece el punto.
     * @param stopID ID del punto de interés a cargar.
     */
    private fun loadStopData(tripID: String, stopID: String) {
        val stopViewModel: StopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)

        // Solicita los datos del punto de interés al ViewModel.
        stopViewModel.loadStop(tripID, stopID)

        // Observa los cambios en el LiveData del punto de interés.
        stopViewModel.stop.observe(this) { stop ->
            stop?.let {
                updateUI(it) // Actualiza la interfaz con los datos del punto.
            } ?: run {
                showError("Error al cargar los datos del punto de interés")
            }
        }

        // Observa posibles errores en el ViewModel.
        stopViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                showError(it) // Muestra el mensaje de error.
            }
        }
    }

    /**
     * Actualiza la interfaz con los datos del punto de interés cargado.
     *
     * @param stop Objeto del punto de interés con los datos cargados.
     */
    private fun updateUI(stop: Stop) {
        this.stop = stop

        // Actualiza los campos de texto.
        etName.setText(stop.name)
        etDescription.setText(stop.text)

        // Actualiza la fecha y hora desde el Timestamp.
        stop.timestamp?.let { timestamp ->
            calendar.time = timestamp.toDate() // Sincroniza el Calendar global.
            tvDate.text = SimpleDateFormat("dd 'de' MMMM 'de' yyyy", Locale.getDefault()).format(calendar.time)
            tvTime.text = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)
            timestampFb = stop.timestamp!! // Actualiza el timestamp global.
        }

        // Actualiza el fragmento de Google Places si está inicializado.
        if (Places.isInitialized()) {
            placeFragment?.setText(stop.namePlace)
        }

        // Actualiza los datos de Google Places.
        idPlace = stop.idPlace!!
        namePlace = stop.namePlace!!
        addressPlace = stop.addressPlace!!
        geoPoint = stop.geoPoint!!

        // Actualiza las imágenes en la interfaz.
        imagesList.clear()
        imagesList.addAll(stop.photos ?: emptyList()) // Agrega imágenes existentes.
        adapter.notifyDataSetChanged() // Notifica al adaptador del RecyclerView.
    }

    /**
     * Muestra un mensaje de error al usuario.
     *
     * @param message Mensaje de error a mostrar.
     */
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Crea un nuevo punto de interés en Firestore.
     */
    private fun createStop() {
        val stopData = hashMapOf(
            "name" to etName.text.toString(),
            "text" to etDescription.text.toString(),
            "timestamp" to timestampFb,
            "geoPoint" to geoPoint,
            "idPlace" to idPlace,
            "namePlace" to namePlace,
            "addressPlace" to addressPlace
        )

        db.collection("trips").document(tripID).collection("stops").add(stopData)
            .addOnSuccessListener { documentReference ->
                uploadImages(documentReference.id) // Sube imágenes asociadas al punto.
                Toast.makeText(this, "Punto de interés creada con éxito", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Actualiza los datos de un punto de interés existente en Firestore.
     */
    private fun updateStop() {
        val stopData = hashMapOf(
            "name" to etName.text.toString(),
            "text" to etDescription.text.toString(),
            "timestamp" to timestampFb,
            "geoPoint" to geoPoint,
            "idPlace" to idPlace,
            "namePlace" to namePlace,
            "addressPlace" to addressPlace
        )

        stopID?.let { id ->
            db.collection("trips").document(tripID).collection("stops").document(id)
                .update(stopData as Map<String, Any>).addOnSuccessListener {
                    uploadImages(id) // Sube o elimina imágenes según corresponda.
                    Toast.makeText(this, "Parada actualizada con éxito", Toast.LENGTH_SHORT).show()
                }
        }

        Toast.makeText(
            this, "Punto de Interés liberado.", Toast.LENGTH_LONG
        ).show()
    }

    /**
     * Maneja la subida y eliminación de imágenes asociadas a un punto de interés.
     *
     * @param stopId ID del punto de interés.
     */
    private fun uploadImages(stopId: String) {

        // Si está en modo creación, agrega todas las imágenes a la lista de nuevas.
        if (!isEditMode) {
            newImages.addAll(imagesList)
            imagesToDelete.clear()
        }

        // Elimina duplicados de las listas de imágenes.
        newImages = newImages.distinct().toMutableList()
        imagesToDelete = imagesToDelete.distinct().toMutableList()

        // Sube nuevas imágenes a Firebase Storage.
        newImages.forEach { uriString ->
            val storageRef = FirebaseStorage.getInstance()
                .getReference("Stop_Image/$tripID/$stopId/${System.currentTimeMillis()}")
            storageRef.putFile(uriString.toUri()).addOnSuccessListener { taskSnapshot ->
                taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                    val photoData = hashMapOf("url" to uri.toString())
                    db.collection("trips").document(tripID).collection("stops").document(stopId)
                        .collection("photos").add(photoData)
                }
            }
        }

        // Elimina imágenes marcadas desde Firebase Storage.
        imagesToDelete.forEach { url ->
            val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)
            storageRef.delete().addOnSuccessListener {
                db.collection("trips").document(tripID).collection("stops").document(stopId)
                    .collection("photos").whereEqualTo("url", url).get()
                    .addOnSuccessListener { querySnapshot ->
                        querySnapshot.documents.forEach { it.reference.delete() }
                    }
            }
        }

        // Limpia las listas después de procesarlas.
        newImages.clear()
        imagesToDelete.clear()
    }

    /**
     * Método llamado al destruir la actividad.
     * Si la actividad estaba en modo edición, libera el estado de edición.
     */
    override fun onDestroy() {
        super.onDestroy()
        if (mode == MODE_EDIT) {
            clearEditingState(tripID, stopID)
            Toast.makeText(
                this,
                "Punto de Interés liberado.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    /**
     * Limpia el estado de edición al salir de la actividad.
     *
     * @param tripID ID del viaje al que pertenece el punto.
     * @param stopID ID del punto de interés.
     */
    private fun clearEditingState(tripID: String, stopID: String?) {
        val stopDoc = db.collection("trips").document(tripID).collection("stops").document(stopID!!)
        stopDoc.update("editing", null).addOnFailureListener {
            Log.e("PostStopActivity", "No se pudo limpiar el estado de edición.")
        }
    }

    /**
     * Maneja la solicitud de permisos del sistema (como acceso a la cámara o galería).
     * Si el permiso no es concedido, muestra un mensaje al usuario.
     */
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (!isGranted) {
                Toast.makeText(
                    this,
                    "Permiso denegado. Vuelva a intentarlo.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    /**
     * Maneja la selección de imágenes desde la galería.
     * Utiliza el ImagePickerHelper para procesar los datos seleccionados por el usuario.
     *
     * @param result Contiene el resultado de la actividad lanzada para seleccionar imágenes.
     */
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imagePickerHelper.handleGalleryResult(result.data)
            }
        }

    /**
     * Maneja la captura de imágenes utilizando la cámara del dispositivo.
     * Utiliza el ImagePickerHelper para procesar las imágenes capturadas por el usuario.
     *
     * @param result Contiene el resultado de la actividad lanzada para capturar imágenes.
     */
    private val cameraLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                imagePickerHelper.handleCameraResult()
            }
        }

}
