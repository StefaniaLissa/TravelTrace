package com.example.traveltrace.view.trip

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.view.adapters.TripAdapter
import com.example.traveltrace.viewmodel.TripViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton

/**
 * Fragmento principal de la aplicación que muestra la lista de viajes creados por el usuario.
 */
class HomeFragment : Fragment() {

    private lateinit var tripViewModel:     TripViewModel
    private lateinit var tripRecyclerView:  RecyclerView
    private lateinit var tripAdapter:       TripAdapter
    private lateinit var fab_create:        FloatingActionButton
    private lateinit var tv_no_trip:        TextView

    /**
     * Infla el diseño del fragmento y establece las referencias iniciales a las vistas.
     */
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        fab_create = view.findViewById(R.id.fab_custom) // Referencia al botón flotante
        tv_no_trip = view.findViewById(R.id.tv_no_trip) // Referencia al texto para viajes vacíos
        return view
    }

    /**
     * Configura la lógica y las vistas del fragmento después de que la vista haya sido creada.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Configuración del RecyclerView para mostrar la lista de viajes
        tripRecyclerView = view.findViewById(R.id.recyclerView)
        tripRecyclerView.layoutManager = LinearLayoutManager(context)
        //tripRecyclerView.setHasFixedSize(true) // Mejora el rendimiento si las dimensiones no cambian
        tripAdapter = TripAdapter()
        tripRecyclerView.adapter = tripAdapter

        // Inicialización del ViewModel para cargar los datos
        tripViewModel = ViewModelProvider(this)[TripViewModel::class.java]
        tripViewModel.loadTrips() // Cargar la lista de viajes

        // Observador para la lista de viajes
        tripViewModel.allTrips.observe(viewLifecycleOwner) { trips ->
            tripAdapter.updateTripList(trips.toMutableList())
            if (trips.isEmpty()) {
                // Muestra un mensaje si no hay viajes
                tv_no_trip.visibility = View.VISIBLE
            } else {
                tv_no_trip.visibility = View.GONE
            }
        }

        // Configuración del botón flotante para crear un nuevo viaje
        fab_create.setOnClickListener {
            val intent = Intent(activity, CreateTripActivity::class.java)
            startActivity(intent)
        }
    }
}
