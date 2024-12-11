package com.example.traveltrace.view.trip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.traveltrace.view.tripExtra.AlbumFragment
import com.example.traveltrace.view.tripExtra.ShareTripFragment
import com.example.traveltrace.R
import com.example.traveltrace.view.adapters.StopAdapter
import com.example.traveltrace.viewmodel.StopViewModel
import com.example.traveltrace.viewmodel.TripViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import com.example.traveltrace.view.stop.PostStopActivity
import java.util.Date

/**
 * Clase DetailedTripActivity
 *
 * Esta actividad muestra los detalles de un viaje específico, incluyendo:
 * - Lista de paradas del viaje.
 * - Un mapa con la ubicación de las paradas.
 * - Opciones para agregar, editar o eliminar paradas (dependiendo del modo).
 *
 * Implementa la interfaz OnMapReadyCallback para gestionar el mapa de Google.
 */
class DetailedTripActivity : AppCompatActivity(), OnMapReadyCallback {

    // Vistas y variables principales
    private lateinit var fab_newStop:   FloatingActionButton    // Botón para agregar una nueva parada
    private lateinit var toolbar:       Toolbar                 // Barra de herramientas con el título del viaje
    private lateinit var viewModel:     TripViewModel           // ViewModel para gestionar los datos del viaje
    private lateinit var stopViewModel: StopViewModel           // ViewModel para gestionar las paradas del viaje
    private lateinit var stopRecyclerView: RecyclerView         // RecyclerView para mostrar la lista de paradas
    private lateinit var stopAdapter:   StopAdapter             // Adaptador para gestionar los datos de las paradas
    private lateinit var mMap:          GoogleMap               // Instancia del mapa de Google
    private lateinit var tripID:        String                  // ID del viaje cargado
    private lateinit var initDate:      Date                    // Fecha de inicio del viaje
    private lateinit var tv_no_stop:    TextView                // Texto que muestra "sin paradas" si no hay datos
    private lateinit var coordinates:   ArrayList<GeoPoint>     // Lista de coordenadas de las paradas
    private var isReadOnly:             Boolean = false         // Bandera para indicar si el modo es de solo lectura
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout // Swipe para refrescar la lista de paradas


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_trip)

        // Configuración del SwipeRefreshLayout
        swipeRefreshLayout = findViewById(R.id.swipe)
        swipeRefreshLayout.setOnRefreshListener {
            stopViewModel.loadStopsForTrip(tripID) // Recargar paradas
            stopAdapter.notifyDataSetChanged()
        }
        swipeRefreshLayout.setProgressBackgroundColorSchemeColor(getResources().getColor(R.color.green))
        swipeRefreshLayout.setColorSchemeResources(R.color.yellow)

        // Inicialización de vistas
        fab_newStop = findViewById(R.id.fab_newStop)
        tv_no_stop = findViewById(R.id.tv_no_stop)
        toolbar = findViewById(R.id.toolbar)

        // Obtener el ID del viaje desde el intent
        tripID = intent.getStringExtra("id").toString()

        // Configuración del ViewModel del viaje
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(tripID)
        viewModel.trip.observe(this) {
            if (it != null) {
                toolbar.title = it.name.toString() // Establecer el título del toolbar
                initDate = it.initDate?.toDate() ?: Date(9999 - 12 - 31) // Fecha de inicio
            }
        }

        // Configuración de la barra de herramientas
        setSupportActionBar(toolbar)

        // Configuración del Action Bar
        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        // Obtener el modo de solo lectura desde el intent
        isReadOnly = intent.getBooleanExtra("isReadOnly", false)

        // Ajustar la interfaz según el modo
        if (isReadOnly) {
            fab_newStop.visibility = View.GONE // Ocultar botón de nueva parada
        }

        // Configuración del RecyclerView para mostrar paradas
        stopRecyclerView = findViewById(R.id.rv_stops)
        stopRecyclerView.layoutManager = LinearLayoutManager(this)
        //stopRecyclerView.setHasFixedSize(true)
        stopAdapter = StopAdapter(isReadOnly = isReadOnly)
        stopRecyclerView.adapter = stopAdapter

        // Configuración del ViewModel para las paradas
        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)
        stopAdapter.tripID = tripID
        stopViewModel.stopsForTrip.observe(this) { stops ->
            if (stops.isEmpty()) {
                tv_no_stop.visibility = View.VISIBLE // Mostrar mensaje de "sin paradas"
                stopAdapter.updateStopList(emptyList())
            } else {
                tv_no_stop.visibility = View.GONE // Ocultar mensaje
                stopAdapter.updateStopList(stops) // Actualizar la lista en el adaptador
                coordinates.clear()

                // Procesar las coordenadas de las paradas y marcarlas en el mapa
                stops.forEach { stop ->
                    stop.geoPoint?.let { geoPoint ->
                        if (geoPoint.latitude != 0.0 || geoPoint.longitude != 0.0) {
                            coordinates.add(geoPoint)
                            LatLng(geoPoint.latitude, geoPoint.longitude).let { latLng ->
                                mMap.addMarker(
                                    MarkerOptions()
                                        .position(latLng)
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                                )
                            }
                        }
                    }
                }
                setupMap() // Configurar el mapa con las coordenadas
            }
            swipeRefreshLayout.isRefreshing = false // Detener el giro del refresco
        }

        stopViewModel.loadStopsForTrip(tripID) // Cargar las paradas del viaje
        coordinates = stopViewModel.getCoordinates(tripID)!!

        // Configuración del botón para agregar una nueva parada
        fab_newStop.setOnClickListener {
            val intent = Intent(this, PostStopActivity::class.java)
            intent.putExtra("tripID", tripID) // Pasar el ID del viaje
            intent.putExtra("isEditMode", false) // Crear una nueva parada
            startActivity(intent)
        }

        // Configuración del fragmento del mapa
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    /**
     * Configura el mapa con las coordenadas de las paradas del viaje.
     * Si hay coordenadas disponibles, ajusta el mapa para mostrarlas todas.
     * Si no hay coordenadas, muestra una vista global del mapa.
     */
    private fun setupMap() {
        if (::mMap.isInitialized) { // Verifica que el mapa esté inicializado
            if (coordinates.isNotEmpty()) { // Verifica si hay coordenadas para mostrar
                val bld = LatLngBounds.Builder()
                coordinates.forEach { geoPoint ->
                    val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                    bld.include(latLng)
                }

                val bounds = bld.build()
                val mapView = (supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment).view
                mapView?.post {
                    try {
                        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 150)) // Ajusta la cámara
                        if (coordinates.size == 1) { // Si hay solo un punto, ajusta el zoom
                            mMap.animateCamera(CameraUpdateFactory.zoomTo(12.5f))
                        }
                    } catch (e: Exception) {
                        Log.e("DetailedTripActivity", "Error adjusting map bounds: ${e.message}")
                    }
                }
            } else {
                // Si no hay coordenadas, muestra una vista global
                val worldCenter = LatLng(0.0, 0.0) // Centro del mundo
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(worldCenter, 1.5f)) // Zoom bajo para ver continentes
            }
        }
    }

    /**
     * Configuración inicial del mapa de Google cuando está listo.
     * Habilita opciones de interacción
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMyLocationButtonEnabled = true // Botón "Mi ubicación"
        mMap.uiSettings.isZoomControlsEnabled = true    // Controles de zoom
        mMap.uiSettings.isCompassEnabled = true         // Brújula
    }

    /**
     * Configuración del menú de opciones en el Toolbar.
     * Oculta opciones específicas si la actividad está en modo solo lectura.
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.trip_toolbar, menu)

        if (isReadOnly) { // Oculta opciones según el modo
            menu?.findItem(R.id.edit)?.isVisible = false
            menu?.findItem(R.id.add_from_img)?.isVisible = false
            menu?.findItem(R.id.share)?.isVisible = false
            menu?.findItem(R.id.delete)?.isVisible = false
        }
        return true
    }

    /**
     * Maneja las acciones seleccionadas en el menú de opciones.
     * Incluye acciones como abrir el álbum, editar, eliminar, compartir o agregar paradas desde imágenes.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId

        if (id == android.R.id.home) { // Regresar a la actividad anterior
            finish()
            return true
        }

        if (id == R.id.album) { // Abrir el álbum de fotos
            val fragment = AlbumFragment()
            val bundle = Bundle()
            bundle.putString("trip", tripID)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }

        if (id == R.id.edit) { // Editar viaje
            val fragment = EditTripFragment()
            val bundle = Bundle()
            bundle.putString("trip", tripID)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }

        if (id == R.id.delete) { // Eliminar viaje
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            val db = FirebaseFirestore.getInstance()

            // Verificar si el usuario es administrador
            db.collection("trips").document(tripID).get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val adminId = document.getString("admin") // UID del administrador
                    if (currentUserId == adminId) { // Si es administrador
                        showDeleteConfirmationDialog { deleteTripCompletely() }
                    } else { // Si no es administrador
                        showDeleteConfirmationDialog { deleteFromMembers(currentUserId) }
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("DeleteTrip", "Error al verificar rol: ${e.message}")
            }
        }

        if (id == R.id.add_from_img) { // Agregar paradas desde imágenes
            val intent = Intent(applicationContext, CreateFromImgActivity::class.java)
            intent.putExtra("tripID", tripID)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            applicationContext.startActivity(intent)
        }

        if (id == R.id.share) { // Compartir viaje
            val fragment = ShareTripFragment()
            val bundle = Bundle()
            bundle.putString("trip", tripID)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }
        return true
    }

    /**
     * Muestra un diálogo de confirmación para eliminar el viaje.
     *
     * @param onConfirm Acción a realizar si el usuario confirma la eliminación.
     */
    private fun showDeleteConfirmationDialog(onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar este viaje? Esta acción no se puede deshacer.")
        builder.setPositiveButton("Eliminar") { dialog, _ ->
            onConfirm() // Ejecutar la acción confirmada
            dialog.dismiss()
            finish()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    /**
     * Elimina al usuario actual de la colección "members" del viaje.
     *
     * @param userId ID del usuario actual.
     */
    private fun deleteFromMembers(userId: String?) {
        val db = FirebaseFirestore.getInstance()
        if (userId != null) {
            db.collection("members").whereEqualTo("tripID", tripID).whereEqualTo("userID", userId)
                .get().addOnSuccessListener { snapshot ->
                    for (doc in snapshot) {
                        db.collection("members").document(doc.id).delete()
                    }
                }.addOnFailureListener { e ->
                    Log.e("DeleteTrip", "Error al eliminar de members: ${e.message}")
                }
        }
    }

    /**
     * Elimina completamente un viaje, incluyendo:
     * - Colección "members".
     * - Imágenes de la portada y las paradas.
     * - Documento del viaje.
     */
    private fun deleteTripCompletely() {
        val db = FirebaseFirestore.getInstance()

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
                    val stopImagesRef = storage.reference.child(stopImagesPath)
                    stopImagesRef.listAll().addOnSuccessListener { listResult ->
                        for (file in listResult.items) {
                            file.delete()
                        }
                    }
                }
            }.addOnFailureListener { e ->
                Log.e("DeleteTrip", "Error al cargar las paradas: ${e.message}")
            }

        // Eliminar documento del viaje
        db.collection("trips").document(tripID).delete().addOnSuccessListener {
        }.addOnFailureListener { e ->
            Log.e("DeleteTrip", "Error al eliminar el viaje: ${e.message}")
        }
    }

    /**
     * Método onResume.
     * Recarga las paradas del viaje y actualiza la lista.
     */
    override fun onResume() {
        super.onResume()
        stopViewModel.loadStopsForTrip(tripID)
        stopAdapter.notifyDataSetChanged()
    }

}

