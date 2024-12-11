package com.example.traveltrace.view.stop

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Stop
import com.example.traveltrace.view.adapters.ImageAdapter
import com.example.traveltrace.viewmodel.StopViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.traveltrace.model.data.Trip
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Activity detallada que muestra la información completa de un punto de interés (Stop).
 * Incluye datos como ubicación, imágenes, notas, y una vista de mapa interactiva.
 *
 * Implementa `OnMapReadyCallback` para gestionar el mapa de Google.
 */
class DetailedStopActivity : AppCompatActivity(), OnMapReadyCallback {

    // Punto de interés
    private lateinit var stopID:      String            // ID del punto de interés
    private lateinit var tripID:      String            // ID del viaje al que pertenece el punto
    private var stop:                 Stop? = null      // Objeto del punto de interés
    private lateinit var stopViewModel: StopViewModel   // ViewModel para gestionar datos del punto

    // Vistas del layout
    private lateinit var tv_date:     TextView          // Muestra la fecha del punto
    private lateinit var tv_time:     TextView          // Muestra la hora del punto
    private lateinit var tv_place:    TextView          // Muestra el nombre del lugar
    private lateinit var tv_address:  TextView          // Muestra la dirección del lugar
    private lateinit var tv_notes:    TextView          // Muestra las notas asociadas al punto
    private lateinit var rv_images:   RecyclerView      // RecyclerView para mostrar imágenes del punto
    private lateinit var tv_name:     TextView          // Muestra el nombre del punto de interés
    private lateinit var toolbar:     Toolbar           // Toolbar del layout

    // Mapa
    private lateinit var mapManager:  SupportMapFragment // Fragmento del mapa
    private lateinit var mMap:        GoogleMap         // Instancia del mapa de Google
    private lateinit var latLng:      LatLng            // Latitud y longitud del punto
    private lateinit var llPlace:     LinearLayout      // Layout para información de ubicación

    private var isReadOnly: Boolean = false             // Flag para determinar si es modo solo lectura
    private lateinit var timestampFb: Timestamp         // Timestamp asociado al punto de interés

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_stop) // Asigna el layout correspondiente
        init() // Inicialización de vistas y variables

        // Determina si la actividad está en modo solo lectura
        isReadOnly = intent.getBooleanExtra("isReadOnly", false)

        // Configuración del ViewModel y observación de datos
        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)
        stopViewModel.loadStop(tripID, stopID) // Carga el punto de interés con su ID
        stopViewModel.stop.observe(this) { it ->
            if (it != null) {
                // Asigna el objeto del punto a la variable `stop`
                stop = it
                tv_name.text = it.name

                // Fecha y hora
                val calendar = Calendar.getInstance()
                calendar.time = Date(it.timestamp!!.seconds * 1000) // Convierte el timestamp a Date
                timestampFb = it.timestamp!! // Guarda el timestamp para referencia futura

                tv_date.text = String.format(
                    Locale.getDefault(),
                    "%02d/%02d/%d",
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1, // Ajusta el índice del mes (+1)
                    calendar.get(Calendar.YEAR)
                )

                tv_time.text = String.format(
                    Locale.getDefault(),
                    "%02d:%02d",
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE)
                )

                // Place
                if (!it.namePlace.isNullOrBlank()) {
                    tv_place.text = it.namePlace.toString()
                    llPlace.visibility = View.VISIBLE
                }

                if (!it.addressPlace.isNullOrBlank()) {
                    tv_address.text = it.addressPlace.toString()
                    llPlace.visibility = View.VISIBLE
                }

                // Muestra notas si están disponibles, o las oculta si no
                if (!it.text.isNullOrBlank()) {
                    tv_notes.text = it.text
                    tv_notes.visibility = View.VISIBLE
                } else {
                    tv_notes.visibility = View.GONE
                }

                loadMultimedia() // Llama a la función para cargar imágenes relacionadas
                setupMap()       // Configura y muestra el mapa con el punto de interés
            }
        }


        // Configuración del mapa
        mapManager = supportFragmentManager.findFragmentById(R.id.mapStop) as SupportMapFragment
        mapManager.getMapAsync(this)

        // Configuración de la Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    // Método para cargar imágenes en el RecyclerView
    private fun loadMultimedia() {
        if (!stop?.photos.isNullOrEmpty()) {
            val layoutManager =
                LinearLayoutManager(rv_images.context, LinearLayoutManager.HORIZONTAL, false)
            rv_images.layoutManager = layoutManager
            val adapter = ImageAdapter(stop!!.photos!!)
            adapter.notifyDataSetChanged()
            rv_images.adapter = adapter
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isCompassEnabled = false
    }

    // Configuración y posicionamiento del mapa
    private fun setupMap() {
        stop?.geoPoint?.let {
            latLng = LatLng(it.latitude, it.longitude)
            if (latLng != LatLng(0.0, 0.0)) {
                mMap.addMarker(
                    MarkerOptions().position(latLng)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.marker))
                )

                val bounds = LatLngBounds.Builder().include(latLng).build()
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 1))
                mMap.animateCamera(CameraUpdateFactory.zoomTo(12.5f))
                llPlace.visibility = View.VISIBLE
            } else {
                llPlace.visibility = View.GONE
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }

            R.id.delete -> {
                showDeleteConfirmationDialog { deleteStop() }
            }

            R.id.edit -> {
                editStop()
            }
        }

        return true
    }

    // Navegar a la actividad de edición del punto
    private fun editStop() {
        val intent = Intent(this, PostStopActivity::class.java)
        intent.putExtra("tripID", tripID)
        intent.putExtra("stopID", stopID)
        intent.putExtra("isEditMode", true)
        startActivity(intent)
    }

    // Eliminar punto de interés de Firestore
    private fun deleteStop() {
        FirebaseFirestore.getInstance().collection("trips").document(tripID)
            .collection("stops").document(stopID).delete().addOnSuccessListener {
                val stopImagesPath = "Stop_Image/$tripID/$stopID/"
                val stopImagesRef = FirebaseStorage.getInstance().reference.child(stopImagesPath)
                stopImagesRef.listAll().addOnSuccessListener { listResult ->
                    for (file in listResult.items) {
                        file.delete() // Elimina cada imagen de las paradas
                    }
                }

                // Actualizar las fechas del viaje (initDate, endDate, duración)
                FirebaseFirestore.getInstance().collection("trips").document(tripID).get()
                    .addOnSuccessListener { document ->
                        val trip = document.toObject(Trip::class.java)
                        val currentInitDate = trip?.initDate ?: Timestamp(Date(9999, 12, 31))
                        val currentEndDate = trip?.endDate ?: Timestamp(Date(0, 1, 1))

                        var newInitDate = currentInitDate
                        var newEndDate = currentEndDate

                        // Comparar y actualizar initDate y endDate
                        if (timestampFb.toDate().before(currentInitDate.toDate())) {
                            newInitDate = timestampFb
                        }
                        if (timestampFb.toDate().after(currentEndDate.toDate())) {
                            newEndDate = timestampFb
                        }

                        // Calcular duración en días
                        val durationInDays =
                            ((newEndDate.seconds - newInitDate.seconds) / (60 * 60 * 24)).toInt()

                        // Actualizar en la base de datos
                        FirebaseFirestore.getInstance().collection("trips").document(tripID).update(
                            mapOf(
                                "initDate" to newInitDate,
                                "endDate" to newEndDate,
                                "duration" to durationInDays
                            )
                        ).addOnSuccessListener {
                            Toast.makeText(
                                applicationContext,
                                "Fechas y duración actualizadas",
                                Toast.LENGTH_SHORT
                            ).show()
                        }.addOnFailureListener { ex ->
                            Toast.makeText(
                                applicationContext,
                                "Error al actualizar fechas: ${ex.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                finish()
            }
    }

    // Mostrar cuadro de diálogo de confirmación para eliminar
    private fun showDeleteConfirmationDialog(onConfirm: () -> Unit) {
        val builder = AlertDialog.Builder(this, R.style.CustomDialogTheme)
        builder.setTitle("Confirmar eliminación")
        builder.setMessage("¿Estás seguro de que deseas eliminar este punto de interés?")
        builder.setPositiveButton("Eliminar") { dialog, _ ->
            onConfirm()
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancelar") { dialog, _ -> dialog.dismiss() }
        builder.create().show()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.stop_toolbar, menu)
        if (isReadOnly) {
            menu?.findItem(R.id.edit)?.isVisible = false
            menu?.findItem(R.id.delete)?.isVisible = false
        }
        return true
    }

    // Recargar datos al reanudar actividad
    override fun onResume() {
        super.onResume()
        stopViewModel.loadStop(tripID, stopID)
    }

    // Inicializar vistas y variables
    private fun init() {
        stopID = intent.getStringExtra("stopID").toString()
        tripID = intent.getStringExtra("tripID").toString()
        stop = Stop()

        tv_date = findViewById(R.id.tv_date)
        tv_time = findViewById(R.id.tv_time)
        tv_place = findViewById(R.id.tvPlace)
        tv_address = findViewById(R.id.tvAddress)
        tv_notes = findViewById(R.id.tv_notes)
        rv_images = findViewById(R.id.rv_images)
        tv_name = findViewById(R.id.tvName)
        llPlace = findViewById(R.id.llPlace)

        toolbar = findViewById(R.id.tb_stop)

    }

}