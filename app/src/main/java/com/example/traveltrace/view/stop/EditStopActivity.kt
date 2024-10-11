package com.example.traveltrace.view.stop

import android.content.ContentValues
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Stop
import com.example.traveltrace.view.adapters.ImageAdapter
import com.example.traveltrace.viewmodel.StopViewModel
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditStopActivity : AppCompatActivity(), OnMapReadyCallback {

    //XML
    private lateinit var et_name: EditText
    private lateinit var et_description: EditText
    private lateinit var cb_expenses: CheckBox
    private lateinit var ll_expenses: LinearLayout
    private lateinit var spr_category: Spinner
    private lateinit var et_amount: EditText
    //    private lateinit var cb_flight: CheckBox
//    private lateinit var ll_flight: LinearLayout
//    private lateinit var sv_from: SearchView
//    private lateinit var sv_to: SearchView
    private lateinit var btn_gallery: Button
    private lateinit var btn_camera: Button
    private lateinit var sv_images: ScrollView
    private lateinit var rv_images: RecyclerView
    private lateinit var tv_date: TextView
    private var calendar = Calendar.getInstance()
    private lateinit var tv_time: TextView
    private lateinit var timestamp_fb: Timestamp
    private lateinit var ll_alert: LinearLayout
    private lateinit var tv_alert: TextView
    private lateinit var fab_done: FloatingActionButton
    private lateinit var db: FirebaseFirestore
    private var uri: Uri? = null

    //Multimedia
    private lateinit var adapter: ImageAdapter
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var imagesList: ArrayList<String>

    //Ubicación
    private var placeFragment: AutocompleteSupportFragment? = null
    private var idPlace: String = ""
    private var namePlace: String = ""
    private var addressPlace: String = ""
    private var geoPoint: GeoPoint = GeoPoint(0.0, 0.0)
    private lateinit var mapManager: SupportMapFragment
    private lateinit var mMap: GoogleMap
    private lateinit var latLng: LatLng

    //Trip & Stop
    private lateinit var tripID: String
    private lateinit var stopID: String
    private lateinit var stop: Stop
    private lateinit var oldStop: Stop
    private lateinit var stopViewModel: StopViewModel

    //Other
    private lateinit var initDate: Timestamp

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_stop)
        init()

        // Recuperar API KEY
        val ai: ApplicationInfo? = applicationContext.packageManager
            ?.getApplicationInfo(
                applicationContext.packageName.toString(),
                PackageManager.GET_META_DATA
            )
        val apiKey = ai?.metaData?.get("com.google.android.geo.API_KEY").toString()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, apiKey)
        }

        placeFragment!!.setPlaceFields(
            listOf(
                Place.Field.NAME,
                Place.Field.ID,
                Place.Field.LAT_LNG,
                Place.Field.ADDRESS,
                Place.Field.ADDRESS_COMPONENTS
            )
        )

        // Display the fetched information after clicking on one of the options
        placeFragment!!.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                idPlace = place.id.toString()
                namePlace = place.name.toString()
                addressPlace = place.address.toString()
                geoPoint = GeoPoint(place.latLng.latitude, place.latLng.longitude)

            }

            override fun onError(status: Status) {
                Toast.makeText(applicationContext, "Some error occurred", Toast.LENGTH_SHORT)
                    .show()
            }

        })


        //Guardar
        fab_done.setOnClickListener {
            val stop = hashMapOf(
                "name" to et_name.text.toString(),
                "text" to et_description.text.toString(),
                "timestamp" to timestamp_fb,
                "idPlace" to idPlace,
                "namePlace" to namePlace,
                "addressPlace" to addressPlace,
                "geoPoint" to geoPoint
            )

            // Agregar a la colección con nuevo ID
            db.collection("trips")
                .document(tripID)
                .collection("stops")
                .document(stopID)
                .update(stop as Map<String, Any>)
                .addOnSuccessListener { documentReference ->
                    Toast.makeText(
                        applicationContext,
                        "Se ha registrado con éxito",
                        Toast.LENGTH_SHORT
                    ).show()
                    //Subir a Storage
                    if (imagesList != null) {
                        for (uri in imagesList) {
                            val rutaImagen =
                                "Stop_Image/" + tripID + "/" + stopID + "/" + System.currentTimeMillis()
                            val referenceStorage =
                                FirebaseStorage.getInstance().getReference(rutaImagen)
                            referenceStorage.putFile(uri.toUri()!!).addOnSuccessListener { tarea ->
                                val uriTarea: Task<Uri> = tarea.storage.downloadUrl
                                while (!uriTarea.isSuccessful);
                                val url = "${uriTarea.result}"
                                UpdateFirestore(url, stopID)

                            }.addOnFailureListener { e ->
                                Toast.makeText(
                                    applicationContext,
                                    "No se ha podido subir la imagen debido a: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                    //Fecha de Inicio del Viaje Portada
                    initDate = db.collection("trips").document(tripID)
                        .get().result.get("initDate") as Timestamp

                    if ((initDate!! > timestamp_fb) or
                        (initDate == null)
                    ) {
                        db.collection("trips")
                            .document(tripID).update("initDate", timestamp_fb)
                        // TODO: Exception Handling
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(applicationContext, "${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }

            finish()
        }
    }


    private fun init() {

        //Get Intent
        tripID = intent.getStringExtra("tripID").toString()
        stopID = intent.getStringExtra("stopID").toString()

        //Trip Initial Date

        //Get Stop
        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)
        stopViewModel.loadStop(tripID, stopID)
        stopViewModel.stop.observe(this, Observer {
            if (it != null) {
                stop = it

                if (Places.isInitialized()) {
                    placeFragment!!.setText(stop.namePlace)
                }
            }
        })

        et_name = findViewById(R.id.et_name)
        et_description = findViewById(R.id.et_description)
        cb_expenses = findViewById(R.id.cb_expenses)
        ll_expenses = findViewById(R.id.ll_expenses)
        spr_category = findViewById(R.id.spr_category)
        et_amount = findViewById(R.id.et_amount)
        btn_gallery = findViewById(R.id.btn_gallery)
        btn_camera = findViewById(R.id.btn_camera)
        rv_images = findViewById(R.id.rv_images)
        sv_images = findViewById(R.id.sv_images)
        tv_date = findViewById(R.id.tv_date)
        tv_time = findViewById(R.id.tv_time)
        ll_alert = findViewById(R.id.ll_alert)
        tv_alert = findViewById(R.id.tv_alert)
        fab_done = findViewById(R.id.fab_done)
        db = FirebaseFirestore.getInstance()

        imagesList = ArrayList()
        layoutManager = GridLayoutManager(this, 3)
        adapter = ImageAdapter(imagesList)
        rv_images.layoutManager = layoutManager
        rv_images.adapter = adapter

        placeFragment =
            supportFragmentManager.findFragmentById(R.id.fg_autocomplete) as AutocompleteSupportFragment?

        //Cargar datos de la Stop
         db.collection("trips").document(tripID).collection("stops").document(stopID)
            .get()
            .addOnSuccessListener { document ->
                oldStop = document.toObject(Stop::class.java)!!


                et_name.setText(oldStop.name)
                et_description.setText(oldStop.text)

                //Fecha y Hora
                tv_date.text = SimpleDateFormat(
                    "dd 'de' MMMM 'de' yyyy",
                    Locale.getDefault()
                ).format(oldStop.timestamp!!.toDate())
                tv_time.text = SimpleDateFormat("HH:mm").format(oldStop.timestamp!!.toDate())
                timestamp_fb = oldStop.timestamp!!

                //
                setupMap()
                loadMultimedia()
            }

    }

    //Mapa

    private fun setupMap() {

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMyLocationButtonEnabled = false
        mMap.uiSettings.isZoomControlsEnabled = false
        mMap.uiSettings.isCompassEnabled = false
        if (oldStop != null) {
            latLng = LatLng(oldStop!!.geoPoint!!.latitude, oldStop!!.geoPoint!!.longitude)
            mMap.addMarker(MarkerOptions().position(latLng))

            val bld = LatLngBounds.Builder()
            bld.include(latLng)
            val bounds = bld.build()
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 1))

            //Mapa
            mapManager = supportFragmentManager.findFragmentById(R.id.mapStop) as SupportMapFragment
            mapManager.getMapAsync(this)

        }
    }

    //Multimedia

    private fun loadMultimedia() {
        if (!oldStop?.photos.isNullOrEmpty()) {
            val layoutManager =
                LinearLayoutManager(rv_images.context, LinearLayoutManager.HORIZONTAL, false)
            var adapter = ImageAdapter(oldStop?.photos!!)
            rv_images.layoutManager = layoutManager
            rv_images.adapter = adapter
        }
    }

    private fun UpdateFirestore(url: String, stopID: String) {
        val photo = hashMapOf(
            "url" to url
        )
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(intent.getStringExtra("trip").toString())
            .collection("stops")
            .document(stopID)
            .collection("photos")
            .add(photo)
            .addOnFailureListener { e ->
                Toast.makeText(
                    applicationContext,
                    "No se ha actualizado su imagen debido a: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        galleryActivityResultLauncher.launch(intent)
    }

    private fun openCamera() {
        val values = ContentValues()
        values.put(MediaStore.Images.Media.TITLE, "Titulo")
        values.put(MediaStore.Images.Media.DESCRIPTION, "Descripcion")
        uri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        )

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        intent.putExtra(MediaStore.EXTRA_OUTPUT, uri)
        cameraActivityResultLauncher.launch(intent)
    }


    private val galleryPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openGallery()
            } else {
                Toast.makeText(
                    applicationContext,
                    "El permiso para acceder a la galería no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }


    private val cameraPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permission ->
            if (permission) {
                openCamera()
            } else {
                Toast.makeText(
                    applicationContext,
                    "El permiso para acceder a la cámara no ha sido concedido",
                    Toast.LENGTH_SHORT
                ).show()
            }

        }

    private val galleryActivityResultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
        ActivityResultCallback<ActivityResult> { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                uri = data!!.data
                //iv_cover.setImageURI(uri)
                if (data.clipData != null) {
                    for (i in 0 until data.clipData!!.itemCount) {
                        val imageUri = data.clipData!!.getItemAt(i).uri.toString()
                        imagesList.add(imageUri)
                    }
                } else {
                    val imageUri = data.data.toString()
                    imagesList.add(imageUri)
                }
                sv_images.isVisible = true
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(applicationContext, "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()

            }

        }
    )

    private val cameraActivityResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == AppCompatActivity.RESULT_OK) {
                uri?.let { imagesList.add(it.toString()) }
                sv_images.isVisible = true
                adapter.notifyDataSetChanged()
            } else {
                Toast.makeText(applicationContext, "Cancelado por el usuario", Toast.LENGTH_SHORT)
                    .show()
            }
        }
	
}