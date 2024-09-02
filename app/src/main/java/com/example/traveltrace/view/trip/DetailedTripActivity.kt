package com.example.traveltrace.view.trip

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Stop
import com.example.traveltrace.view.stop.CreateStopActivity
import com.example.traveltrace.view.adapters.StopAdapter
import com.example.traveltrace.viewmodel.StopViewModel
import com.example.traveltrace.viewmodel.TripViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import java.text.SimpleDateFormat
import java.util.Locale

class DetailedTripActivity : AppCompatActivity(), OnMapReadyCallback {

    //private lateinit var tv_title: TextView
    private lateinit var fab_newStop: FloatingActionButton
    private lateinit var toolbar: Toolbar
    private lateinit var viewModel: TripViewModel
    private lateinit var stopViewModel: StopViewModel
    private lateinit var stopRecyclerView: RecyclerView
    lateinit var stopAdapter: StopAdapter
    private lateinit var mMap: GoogleMap
    private lateinit var trip: String
    private lateinit var initDate: String
    private lateinit var stops: ArrayList<Stop>

    private lateinit var coordinates: ArrayList<GeoPoint>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detailed_trip)

        //tv_title = findViewById(R.id.tv_title)
        fab_newStop = findViewById(R.id.fab_newStop)
        toolbar = findViewById(R.id.toolbar)

        //Get Trip Intent
        trip = intent.getStringExtra("id").toString()
        initDate = intent.getStringExtra("initDate").toString()

        //Get Trip
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(trip)
        viewModel.trip.observe(this, Observer {
            if (it != null) {
                toolbar.title = it.name.toString()
            }
        })

        setSupportActionBar(toolbar)



        //Get Trip Stops
        stopRecyclerView = findViewById(R.id.rv_stops)
        stopRecyclerView.layoutManager = LinearLayoutManager(this)
        stopRecyclerView.setHasFixedSize(true)
        stopAdapter = StopAdapter()
        stopRecyclerView.adapter = stopAdapter

        stopViewModel = ViewModelProvider(this).get(StopViewModel::class.java)
        stopViewModel.stopsForTrip.observe(this, Observer {
            stopAdapter.updateStopList(it)
            coordinates.clear()
            it.forEach {
//                val latLng = it.geoPoint?.let {
//                    LatLng(it.latitude, it.longitude)}
                it.geoPoint?.let { it2 -> coordinates.add(it2)
                    LatLng(it2.latitude, it2.longitude).let {
                        mMap.addMarker(
                            MarkerOptions()
                                .position(it)
                        )
                    }
            }
            }
        })
        stopViewModel.loadStopsForTrip(trip)
        coordinates = stopViewModel.getCoordinates(trip)!!

//        stops = stopViewModel.getStops()!!

        //New Stop
        fab_newStop.setOnClickListener {
            val intent = Intent(this, CreateStopActivity::class.java)
            intent.putExtra("trip", trip)
            intent.putExtra("initDate", initDate)
            startActivity(intent)
        }

        //Map
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        coordinates.forEach { stop ->
            val latLng = stop.let {
//                val coordinates = it.split(",")
//                LatLng(coordinates[0].toDouble(), coordinates[1].toDouble())}
                LatLng(it.latitude, it.longitude)}

            latLng?.let {
                mMap.addMarker(
                    MarkerOptions()
                        .position(it)
                )
            }
        }
        // Add a marker in Madrid, Spain, and move the camera
//        val madrid = LatLng(40.4168, -3.7038)
//        mMap.addMarker(
//            MarkerOptions()
//                .position(madrid)
//                .title("Madrid")
//        )


        // Set the zoom level (e.g., 12.0f is the zoom level)
//        val zoomLevel = 12.0f

        // Move the camera with zoom to the specified location
//        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(madrid, zoomLevel))

        //mMap.moveCamera(CameraUpdateFactory.newLatLng(madrid))
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.trip_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id = item.itemId
        if (id == R.id.expenses){

        }
        if (id == R.id.album){
            val fragment = AlbumFragment()
            val bundle = Bundle()
            bundle.putString("trip", trip)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }
        if (id == R.id.share){
            val fragment = ShareTripFragment()
            val bundle = Bundle()
            bundle.putString("trip", trip)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }
        if (id == R.id.pdf){

        }
        if (id == R.id.edit){
            val fragment = EditTripFragment()
            val bundle = Bundle()
            bundle.putString("trip", trip)
            fragment.arguments = bundle
            supportFragmentManager.beginTransaction()
                .add(R.id.frame_layout, fragment)
                .addToBackStack(null)
                .commit()
        }
        return true
    }

}