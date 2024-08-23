package com.example.traveltrace.view.stop

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.traveltrace.R
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.common.api.Status
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

// TODO: Fragmento No utilizado, Juntar todo en CreatStopAtivity o Separarlo (Borrar si es solo para usar en CreateStop)

private var apiKey: String? = null

class SearchPlaceFragment : Fragment() {
    lateinit var autoCompleteSupportFragment : AutocompleteSupportFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Recuperar API KEY
        val ai: ApplicationInfo? = getActivity()?.getApplicationContext()?.packageManager
            ?.getApplicationInfo(getActivity()?.getApplicationContext()?.packageName.toString(), PackageManager.GET_META_DATA)
        apiKey = ai?.metaData?.get("com.google.android.geo.API_KEY").toString()

        autoCompleteSupportFragment = (childFragmentManager.findFragmentById(R.id.fg_autocomplete) as AutocompleteSupportFragment?)!!

        // Inicializar Places API
        if (!Places.isInitialized()) {
            Places.initialize(getActivity()?.getApplicationContext(), apiKey)
        }

        // Configurar AutocompleteSupportFragment
        autoCompleteSupportFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.WEBSITE_URI,
                Place.Field.LAT_LNG
            )
        )

        autoCompleteSupportFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                val name = place.name
                val web = place.websiteUri
                val latlng = place.latLng
                val latitude = latlng?.latitude
                val longitude = latlng?.longitude

                val text = "Name: $name \nWeb: $web \n" +
                        "Latitude, Longitude: $latitude , $longitude \n"
            }

            override fun onError(status: Status) {
                Toast.makeText(getActivity()?.getApplicationContext(),"Some error occurred", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_search_place, container, false)

        //var fragmentManager = (activity as FragmentActivity).supportFragmentManager
        //autoCompleteSupportFragment = (fragmentManager.findFragmentById(R.id.fg_autocomplete) as AutocompleteSupportFragment?)!!

    return view
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




    }

    }