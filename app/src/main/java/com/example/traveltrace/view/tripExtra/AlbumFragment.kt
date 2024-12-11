package com.example.traveltrace.view.tripExtra

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.view.adapters.ImageAdapter
import com.example.traveltrace.viewmodel.TripViewModel

/**
 * Fragmento que muestra el álbum de fotos asociadas a un viaje.
 * Permite visualizar imágenes almacenadas en el álbum de un viaje específico.
 */
class AlbumFragment : Fragment() {

    private lateinit var layoutManager: GridLayoutManager
    private lateinit var tripViewModel: TripViewModel
    private lateinit var adapter:       ImageAdapter
    private lateinit var imagesList:    ArrayList<String>
    private lateinit var rv_images:     RecyclerView
    private lateinit var tv_no_fotos:   TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_album, container, false)

        // Configuración inicial de vistas
        rv_images = view.findViewById(R.id.rv_images) // RecyclerView para las fotos
        tv_no_fotos = view.findViewById(R.id.tv_no_fotos) // Texto para "sin fotos"

        // Configurar el Toolbar para navegación y título
        val toolbar: Toolbar = view.findViewById(R.id.toolbar)
        (activity as AppCompatActivity).setSupportActionBar(toolbar)
        val actionBar = (activity as AppCompatActivity).supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true) // Habilitar botón de "atrás"
        actionBar?.title = "Álbum"
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed() // Navegar hacia atrás
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializar la lista de imágenes y el adaptador
        imagesList = ArrayList()
        layoutManager = GridLayoutManager(context, 3) // Diseño en 3 columnas
        adapter = ImageAdapter(imagesList)
        rv_images.layoutManager = layoutManager
        rv_images.adapter = adapter

        // Inicializar el ViewModel para cargar datos del álbum
        tripViewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        val tripId = arguments?.getString("trip")!!
        tripViewModel.loadAlbumPhotos(tripId)

        // Observar cambios en la lista de fotos
        tripViewModel.albumPhotos.observe(viewLifecycleOwner) { photos ->
            if (photos.isEmpty()) {
                // Mostrar mensaje cuando no hay fotos disponibles
                tv_no_fotos.visibility = View.VISIBLE
                rv_images.visibility = View.GONE
            } else {
                // Mostrar las fotos en el RecyclerView
                tv_no_fotos.visibility = View.GONE
                rv_images.visibility = View.VISIBLE
                imagesList.clear()
                imagesList.addAll(photos)
                adapter.notifyDataSetChanged()
            }
        }
    }
}
