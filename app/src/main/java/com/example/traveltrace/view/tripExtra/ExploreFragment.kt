package com.example.traveltrace.view.tripExtra

import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.view.adapters.TripAdapter
import com.example.traveltrace.viewmodel.TripViewModel

/**
 * Fragmento para explorar viajes según filtros de duración y ubicación.
 * Proporciona opciones de búsqueda y filtrado para facilitar la navegación por los viajes disponibles.
 */
class ExploreFragment : Fragment() {


    private lateinit var recyclerView: RecyclerView
    private lateinit var tripAdapter: TripAdapter
    private lateinit var tripViewModel: TripViewModel

    // Botones para filtrar los viajes según su duración
    private lateinit var btnDayTrips: Button
    private lateinit var btnShortTrips: Button
    private lateinit var btnLongTrips: Button

    private lateinit var autocompleteFragment: AutocompleteSupportFragment
    private var locationFilteredTrips: List<Trip>? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(com.example.traveltrace.R.layout.fragment_explore, container, false)

        // Inicializar vistas
        recyclerView = view.findViewById(com.example.traveltrace.R.id.recyclerView)
        btnDayTrips = view.findViewById(com.example.traveltrace.R.id.btn_flt_day)
        btnShortTrips = view.findViewById(com.example.traveltrace.R.id.btn_flt_short)
        btnLongTrips = view.findViewById(com.example.traveltrace.R.id.btn_flt_long)

        // Configurar el adaptador y el RecyclerView
        tripAdapter = TripAdapter(isReadOnly = true) // Modo de solo lectura
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = tripAdapter

        // Inicializar ViewModel para cargar y observar datos
        tripViewModel = ViewModelProvider(this).get(TripViewModel::class.java)

        setupFilters() // Configurar botones de filtro
        setupAutocompleteSearch() // Configurar búsqueda por ubicación

        return view
    }

    /**
     * Configura los botones para filtrar viajes según su duración.
     */
    private fun setupFilters() {
        btnDayTrips.setOnClickListener {
            filterTripsByDuration(0, 1) // Filtrar viajes de 1 día o menos
        }

        btnShortTrips.setOnClickListener {
            filterTripsByDuration(2, 7) // Filtrar viajes de 2 a 7 días
        }

        btnLongTrips.setOnClickListener {
            filterTripsByDuration(7, Int.MAX_VALUE) // Filtrar viajes de más de 7 días
        }
    }

    /**
     * Filtra los viajes por la duración especificada en días.
     *
     * @param minDays Duración mínima del viaje en días.
     * @param maxDays Duración máxima del viaje en días.
     */
    private fun filterTripsByDuration(minDays: Int, maxDays: Int) {
        if (locationFilteredTrips != null) {
            // Filtrar resultados ya filtrados por ubicación
            val filteredTrips = locationFilteredTrips!!.filter { trip ->
                val duration = trip.duration ?: 0
                duration in minDays..maxDays
            }
            if (filteredTrips.isNotEmpty()) {
                tripAdapter.updateTripList(filteredTrips)
            } else {
                Toast.makeText(requireContext(), "No se encontraron viajes", Toast.LENGTH_SHORT)
                    .show()
            }
        } else {
            // Filtrar directamente desde el ViewModel
            tripViewModel.getTripsByDuration(minDays, maxDays)
                .observe(viewLifecycleOwner) { trips ->
                    if (trips.isNotEmpty()) {
                        tripAdapter.updateTripList(trips)
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "No se encontraron viajes",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }
    }

    /**
     * Configura la búsqueda autocompletada basada en ubicación.
     */
    private fun setupAutocompleteSearch() {

        // Recuperar la API Key para Google Places
        val ai: ApplicationInfo? = requireContext().packageManager
            ?.getApplicationInfo(
                requireContext().packageName,
                PackageManager.GET_META_DATA
            )
        val apiKey = ai?.metaData?.getString("com.google.android.geo.API_KEY").toString()

        // Inicializar Places SDK si no está inicializado
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), apiKey)
        }

        // Obtener el fragmento de autocompletar
        autocompleteFragment =
            childFragmentManager.findFragmentById(com.example.traveltrace.R.id.fg_autocomplete)
                    as AutocompleteSupportFragment

        // Configurar los campos de lugar que se desean obtener
        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.LAT_LNG
            )
        )

        // Botón para limpiar búsqueda
        autocompleteFragment.view?.findViewById<ImageButton>(com.google.android.libraries.places.R.id.places_autocomplete_clear_button)
            ?.setOnClickListener { view ->
                autocompleteFragment.setText("")
                view.visibility = View.GONE
                locationFilteredTrips = null
                tripAdapter.updateTripList(emptyList())
            }

        // Listener para manejar selección de lugar
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let { latLng ->
                    filterTripsByLocation(latLng.latitude, latLng.longitude)
                }
            }

            override fun onError(status: com.google.android.gms.common.api.Status) {
                if (status != com.google.android.gms.common.api.Status.RESULT_CANCELED) {
                    Toast.makeText(requireContext(), "Error al buscar: $status", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        })
    }

    /**
     * Filtra los viajes en un radio de 50 km desde una ubicación específica.
     *
     * @param lat Latitud del punto de búsqueda.
     * @param lng Longitud del punto de búsqueda.
     */
    private fun filterTripsByLocation(lat: Double, lng: Double) {
        tripViewModel.getTripsByLocation(lat, lng, 50.0).observe(viewLifecycleOwner) { trips ->
            if (trips.isNotEmpty()) {
                locationFilteredTrips = trips // Guardar resultados filtrados por ubicación
                tripAdapter.updateTripList(trips)
            } else {
                Toast.makeText(
                    requireContext(),
                    "No se encontraron viajes cerca",
                    Toast.LENGTH_SHORT
                ).show()
                locationFilteredTrips = null // Limpiar resultados
                tripAdapter.updateTripList(emptyList()) // Limpiar RecyclerView
            }
        }
    }
}
