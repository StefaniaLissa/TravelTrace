package com.example.traveltrace.view.stop

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Stop
import com.example.traveltrace.view.adapters.ImageAdapter
import com.example.traveltrace.viewmodel.StopViewModel
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class EditStopActivity : AppCompatActivity() {


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

    private lateinit var adapter: ImageAdapter
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var imagesList: ArrayList<String>

    private var placeFragment: AutocompleteSupportFragment? = null
    private var idPlace: String = ""
    private var namePlace: String = ""
    private var addressPlace: String = ""
    private var geoPoint: GeoPoint = GeoPoint(0.0, 0.0)

    private lateinit var tripID: String
    private lateinit var stopID: String

    private lateinit var stop: Stop
    private lateinit var stopViewModel: StopViewModel

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



    }


    private fun init() {

        //Get Intent
        tripID = intent.getStringExtra("tripID").toString()
        stopID = intent.getStringExtra("stopID").toString()

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

        //Cargar Fecha y Hora actual
        calendar = Calendar.getInstance()
        tv_date.text = SimpleDateFormat(
            "dd 'de' MMMM 'de' yyyy",
            Locale.getDefault()
        ).format(calendar.timeInMillis)
        tv_time.text = SimpleDateFormat("HH:mm").format(calendar.timeInMillis)
        timestamp_fb = Timestamp(calendar.time)
    }
	
}