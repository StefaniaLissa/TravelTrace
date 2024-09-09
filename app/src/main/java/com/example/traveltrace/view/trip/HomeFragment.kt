package com.example.traveltrace.view.trip

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.traveltrace.R
import com.example.traveltrace.view.adapters.TripAdapter
import com.example.traveltrace.viewmodel.TripViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton


class HomeFragment : Fragment() {

    private lateinit var tripViewModel : TripViewModel
    private lateinit var tripRecyclerView: RecyclerView
    lateinit var tripAdapter: TripAdapter
    private lateinit var  fab_create : FloatingActionButton


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        fab_create = view.findViewById(R.id.fab_create)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        tripRecyclerView = view.findViewById(R.id.recyclerView)
        tripRecyclerView.layoutManager = LinearLayoutManager(context)
        tripRecyclerView.setHasFixedSize(true)
        tripAdapter = TripAdapter()
        tripRecyclerView.adapter = tripAdapter

        tripViewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        tripViewModel.loadTrips()

        tripViewModel.allTrips.observe(viewLifecycleOwner, Observer {
            tripAdapter.updateTripList(it.toMutableList())
            Log.w("BD", "loadTripsHome")
        })

        fab_create.setOnClickListener {
            val intent = Intent(getActivity(), CreateTripActivity::class.java)
            startActivity(intent)
        }


    }

    override fun onResume() {
        super.onResume()
        tripViewModel.allTrips.observeForever {
            tripAdapter.updateTripList(it)
        }

    }

}