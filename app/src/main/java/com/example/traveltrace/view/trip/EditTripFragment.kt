package com.example.traveltrace.view.trip

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.SearchView
import androidx.core.view.isVisible
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.example.traveltrace.R
import com.example.traveltrace.viewmodel.TripViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore

class EditTripFragment : Fragment() {

    private lateinit var iv_cover: ImageView
    private lateinit var btn_new_image: Button
    private lateinit var et_name: EditText
    private lateinit var cb_online: CheckBox
    private lateinit var ll_cb_share: LinearLayout
    private lateinit var sv_user: SearchView
    private lateinit var ll_users: LinearLayout
    private lateinit var cb_share: CheckBox
    private lateinit var btn_create: Button
    private lateinit var db: FirebaseFirestore
    private var uri: Uri? = null
    private var uriString: String? = null
    private lateinit var user: FirebaseUser
    private lateinit var viewModel: TripViewModel
    private lateinit var trip: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_trip, container, false)
        iv_cover = view.findViewById(R.id.iv_cover)
        btn_new_image = view.findViewById(R.id.btn_new_image)
        et_name = view.findViewById(R.id.et_name)
        cb_online = view.findViewById(R.id.cb_online)
        ll_cb_share = view.findViewById(R.id.ll_cb_share)
        sv_user = view.findViewById(R.id.sv_user)
        ll_users = view.findViewById(R.id.ll_users)
        cb_share = view.findViewById(R.id.cb_share)
        btn_create = view.findViewById(R.id.btn_create)
        db = FirebaseFirestore.getInstance()
        user = FirebaseAuth.getInstance().currentUser!!
        uriString = String()

        //Get Trip Intent
        trip = arguments?.getString("trip")!!

        //Get Trip
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(trip)

        return view
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel = ViewModelProvider(this).get(TripViewModel::class.java)
        viewModel.loadTrip(trip)
        viewModel.trip.observe(viewLifecycleOwner, Observer {
            et_name.setText(it.name)
            iv_cover.contentDescription = it.image
            Glide.with(this)
                .load(it.image)
                .placeholder(R.drawable.img_cover_travel_trace)
                .into(iv_cover)
        })
    }

}