package com.example.traveltrace.view.stop

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Stop
import com.example.traveltrace.view.adapters.ImageAdapter
import com.example.traveltrace.viewmodel.StopViewModel
import com.example.traveltrace.viewmodel.TripViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class DetailedStopActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var stopID: String
    private lateinit var tripID: String
    private var stop: Stop? = null
    private lateinit var stopViewModel: StopViewModel

    private lateinit var tv_date: TextView
    private lateinit var tv_time: TextView
    private lateinit var tv_place: TextView
    private lateinit var tv_address: TextView
    private lateinit var tv_notes: TextView
    private lateinit var rv_images: RecyclerView
    private lateinit var iv_image: ImageView
    private lateinit var tv_name: TextView

    private lateinit var toolbar: Toolbar
    private lateinit var mapManager: SupportMapFragment
    private lateinit var mMap: GoogleMap
    private lateinit var latLng: LatLng

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_stop)
        initLateinit()

        //Get Stop
        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)
        stopViewModel.loadStop(tripID, stopID)
        stopViewModel.stop.observe(this, Observer {
            if (it != null) {
                tv_name.text = it.name

                var calendar = Calendar.getInstance()
                calendar.time = Date(it.timestamp!!.seconds * 1000)

                tv_date.text =  String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH)) + "/" +
                                String.format("%02d", calendar.get(Calendar.MONTH)) + "/" +
                                calendar.get(Calendar.YEAR).toString()

                tv_time.text =  String.format("%02d", calendar.get(Calendar.HOUR_OF_DAY)) + ":" +
                                String.format("%02d", calendar.get(Calendar.MINUTE))

                tv_place.text = it.namePlace.toString()
                tv_address.text = it.namePlace.toString()
                tv_notes.text = it.text
                stop = it
                setupMap()
                loadMultimedia()
            }
        })

        //Mapa
        mapManager = supportFragmentManager.findFragmentById(R.id.mapStop) as SupportMapFragment
        mapManager.getMapAsync(this)

        //Toolbar
        setSupportActionBar(toolbar)
        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true);
        getSupportActionBar()?.setDisplayShowHomeEnabled(true);

    }

    private fun loadMultimedia() {
        if (!stop?.photos.isNullOrEmpty()) {
            val layoutManager =
                LinearLayoutManager(rv_images.context, LinearLayoutManager.HORIZONTAL, false)
            var adapter = ImageAdapter(stop?.photos!!)
            rv_images.layoutManager = layoutManager
            rv_images.adapter = adapter
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isMyLocationButtonEnabled = true
        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true

    }

    private fun setupMap() {
        if (stop != null) {
            latLng = LatLng(stop!!.geoPoint!!.latitude, stop!!.geoPoint!!.longitude)
            mMap.addMarker(MarkerOptions().position(latLng))

            val bld = LatLngBounds.Builder()
            bld.include(latLng)
            val bounds = bld.build()
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.delete -> {
                deleteStop()
            }
        }
        return true
    }

    private fun deleteStop() {
        FirebaseFirestore.getInstance()
            .collection("trips")
            .document(tripID)
            .collection("stops")
            .document(stopID)
            .delete()
            .addOnSuccessListener {
                finish()
            }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.stop_toolbar, menu)
        return true
    }

    private fun initLateinit() {

        //Get Trip Intent
        stopID = intent.getStringExtra("stopID").toString()
        tripID = intent.getStringExtra("tripID").toString()
        stop = Stop()

        tv_date = findViewById(R.id.tv_date)
        tv_time = findViewById(R.id.tv_time)
        tv_place = findViewById(R.id.tvPlace)
        tv_address = findViewById(R.id.tvAddress)
        tv_notes = findViewById(R.id.tv_notes)
        rv_images = findViewById(R.id.rv_images)
        iv_image = findViewById(R.id.ivImage)
        tv_name = findViewById(R.id.tvName)

        toolbar = findViewById(R.id.tb_stop)

    }

}