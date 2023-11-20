package com.example.traveltrace.view.trip

import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.model.data.Photo
import com.example.traveltrace.model.data.Trip
import com.example.traveltrace.view.adapters.ImageAdapter
import com.google.firebase.firestore.FirebaseFirestore

class AlbumFragment : Fragment() {

    private lateinit var rv_images : RecyclerView

    private lateinit var db : FirebaseFirestore
    private var uri: Uri? = null

    private lateinit var adapter: ImageAdapter
    private lateinit var layoutManager: GridLayoutManager
    private lateinit var imagesList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
        }
    }
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_album, container, false)
        rv_images = view.findViewById(R.id.rv_images)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //Get TripID
        val id = arguments?.getString("trip")!!

        imagesList = ArrayList()

        //Get all trip stops
        FirebaseFirestore.getInstance().collection("trips")
            .document(id)
            .collection("stops")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w("Error", "album failed.", e)
                    return@addSnapshotListener
                }
                //Puntos de Interes
                if (snapshot != null) {
                    for(stop in snapshot){
                        FirebaseFirestore.getInstance().collection("trips")
                            .document(id)
                            .collection("stops")
                            .document(stop.id.toString())
                            .collection("photos")
                            .addSnapshotListener { query, e ->
                                if (e != null) {
                                    Log.w("Error", "Photos failed.", e)
                                    return@addSnapshotListener
                                }
                                //Fotos
                                if (query != null) {
                                    for(photo in query){
                                        val photo = photo.toObject(Photo::class.java)
                                        if (photo != null) {
                                            imagesList.add(photo.url.toString())
                                            adapter.notifyDataSetChanged()
                                        }
                                    }
                                }
                            }
                    }
                }
            }

        layoutManager = GridLayoutManager (context,3)
        adapter = ImageAdapter(imagesList)
        rv_images.layoutManager = layoutManager
        rv_images.adapter = adapter

    }

}